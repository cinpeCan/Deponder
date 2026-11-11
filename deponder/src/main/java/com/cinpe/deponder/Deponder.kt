/*
 * Copyright (c) 2022-present, Cinpecan and Deponder Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Deponder baseUrl: https://github.com/cinpeCan/Deponder
 */
package com.cinpe.deponder

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cinpe.deponder.control.DeponderControl
import com.cinpe.deponder.internal.PhysicsEngine
import com.cinpe.deponder.internal.PlanetNode
import com.cinpe.deponder.internal.PlanetReconciler
import com.cinpe.deponder.internal.RubberReconciler
import com.cinpe.deponder.internal.choreographerFrameFlow
import com.cinpe.deponder.internal.hashCapacity
import com.cinpe.deponder.model.SimplePlanet
import com.cinpe.deponder.model.SimpleRoot
import com.cinpe.deponder.model.SimpleRubber
import com.cinpe.deponder.option.SimpleRootOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @Description:
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class Deponder<P, R>(
    private val owner: LifecycleOwner,
    private val rootOption: SimpleRootOption,
) : DeponderControl<P, R>, BindAdapter<P, R> {

    public constructor(lifecycleOwner: LifecycleOwner, rootView: ViewGroup) : this(
        lifecycleOwner,
        SimpleRoot.builder().itemView(rootView).build(),
    )

    /**
     * Identity-equality wrapper so submitting a mutated or structurally equal collection still triggers emission
     * (preserving `MutableLiveData.setValue` semantics on top of `MutableStateFlow`).
     */
    private class Submission<T>(val items: List<T>)

    private val planetSubmissions = MutableStateFlow<Submission<P>?>(null)
    private val rubberSubmissions = MutableStateFlow<Submission<R>?>(null)
    private val scaleState = MutableStateFlow(rootOption.initScale())

    private val activeNodesState = MutableStateFlow<List<PlanetNode<SimplePlanet>>>(emptyList())
    private val activePlanetIdsState = MutableStateFlow<Set<String>>(emptySet())
    private val activeRubbersState = MutableStateFlow<Map<String, SimpleRubber>>(emptyMap())

    private val planetReconciler = PlanetReconciler(rootOption, this)
    private val rubberReconciler = RubberReconciler(rootOption, this)
    private val physicsEngine = PhysicsEngine(rootOption)
    private val instanceJob: Job = SupervisorJob(owner.lifecycleScope.coroutineContext[Job])

    init {
        if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            initPipelines()
        }
    }

    @OptIn(FlowPreview::class)
    private fun <T> Flow<T>.maybeDebounce(): Flow<T> {
        val interval = rootOption.minInterval()
        return if (interval <= 0L) this else debounce(interval)
    }

    private fun initPipelines() {
        owner.lifecycleScope.launch(Dispatchers.Main.immediate + instanceJob, start = CoroutineStart.DEFAULT) {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Planet reconciliation pipeline.
                launch {
                    planetSubmissions
                        .maybeDebounce()
                        .collect { submission ->
                            if (submission == null) return@collect
                            val updated = planetReconciler.reconcile(submission.items)
                            if (updated != null) {
                                val prevNodes = activeNodesState.value
                                val prevById = HashMap<String, PlanetNode<SimplePlanet>>(hashCapacity(prevNodes.size))
                                for (node in prevNodes) {
                                    prevById[node.p.id()] = node
                                }
                                val nextNodes = ArrayList<PlanetNode<SimplePlanet>>(updated.size)
                                val nextIds = HashSet<String>(hashCapacity(updated.size))
                                for (sp in updated) {
                                    val node = prevById[sp.id()]?.also {
                                        it.p = sp
                                        it.syncCenter()
                                    } ?: PlanetNode(sp)
                                    nextNodes.add(node)
                                    nextIds.add(sp.id())
                                }
                                activeNodesState.value = nextNodes
                                activePlanetIdsState.value = nextIds
                            }
                        }
                }

                // 2. Rubber reconciliation pipeline (debounces external rubber submissions; reacts immediately to active planet IDs).
                launch {
                    combine(
                        rubberSubmissions.maybeDebounce(),
                        activePlanetIdsState,
                    ) { submission, planetIds ->
                        submission to planetIds
                    }.collect { (submission, planetIds) ->
                        if (submission == null) return@collect
                        val updated = rubberReconciler.reconcile(submission.items, planetIds)
                        if (updated != null) {
                            activeRubbersState.value = updated
                        }
                    }
                }

                // 3. Choreographer VSYNC physics integration loop (attached only while at least one planet is active).
                launch {
                    try {
                        activeNodesState
                            .map { it.isNotEmpty() }
                            .distinctUntilChanged()
                            .collectLatest { hasPlanets ->
                                if (!hasPlanets) return@collectLatest
                                var lastFrameTimeNs = 0L
                                choreographerFrameFlow().collect { frameTimeNanos ->
                                    val dt = if (lastFrameTimeNs == 0L) {
                                        DEFAULT_FRAME_DT_MS
                                    } else {
                                        ((frameTimeNanos - lastFrameTimeNs) / 1_000_000L)
                                            .coerceIn(MIN_FRAME_DT_MS, MAX_FRAME_DT_MS)
                                    }
                                    lastFrameTimeNs = frameTimeNanos
                                    val nodes = activeNodesState.value
                                    if (nodes.isNotEmpty()) {
                                        physicsEngine.step(
                                            nodes = nodes,
                                            rubbers = activeRubbersState.value,
                                            dt = dt,
                                            scale = scaleState.value,
                                        )
                                    }
                                }
                            }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        Log.e(TAG, "Physics loop terminated due to unexpected error", e)
                    }
                }
            }
        }
    }

    /**
     * submit P
     */
    final override fun submitPlanet(pList: @JvmSuppressWildcards Collection<P>) {
        planetSubmissions.value = Submission(pList.toList())
    }

    /**
     * submit R
     */
    final override fun submitRubber(rList: @JvmSuppressWildcards Collection<R>) {
        rubberSubmissions.value = Submission(rList.toList())
    }

    /**
     * submit scale
     */
    final override fun submitScale(@FloatRange(from = 0.0, fromInclusive = false) scale: Float) {
        if (scale <= rootOption.maxScale() && scale >= rootOption.minScale()) {
            scaleState.value = scale
        }
    }

    override fun cancel() {
        instanceJob.cancel()
    }

    private companion object {
        private const val TAG: String = "Deponder"
        private const val DEFAULT_FRAME_DT_MS: Long = 16L
        private const val MIN_FRAME_DT_MS: Long = 8L
        private const val MAX_FRAME_DT_MS: Long = 32L
    }
}
