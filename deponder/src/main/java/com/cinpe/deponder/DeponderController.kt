package com.cinpe.deponder

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetBody
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPose
import com.cinpe.deponder.core.SimulationWorld
import com.cinpe.deponder.internal.awaitChoreographerFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private sealed interface ControllerIntent
private data class ControllerUpdatePhysics(val physics: RootPhysics) : ControllerIntent
private data class ControllerSubmitScale(val scale: Float) : ControllerIntent
private data class ControllerSubmitRubberElasticity(val elasticity: Float) : ControllerIntent
private data object ControllerReheat : ControllerIntent
private data class ControllerSetPinned(val id: String, val pinned: Boolean) : ControllerIntent
private data class ControllerSetPressed(val id: String, val pressed: Boolean) : ControllerIntent
private data class ControllerBringToFront(val id: String) : ControllerIntent
private data class ControllerUpdateViewport(val widthPx: Int, val heightPx: Int) : ControllerIntent
private data class ControllerUpdatePlanetSize(val id: String, val widthPx: Int, val heightPx: Int) : ControllerIntent
private data object ControllerCancel : ControllerIntent

/**
 * Imperative physics controller bound to a host [ViewGroup] via [deponderRoot].
 *
 * - Architecture follows a Dual-Plane Unidirectional Data Flow (UDF):
 *   1. **Control Plane**: Orthogonal host-attachment, Android [Lifecycle], topology-non-empty,
 *      cancellation, and kinetic-energy sources are merged with [combine] + [distinctUntilChanged]
 *      into [loopGateFlow], driving a single `collectLatest` Choreographer VSYNC loop.
 *   2. **Data Plane**: State mutations flow through a single reducer pipeline (`ControllerIntent`
 *      for discrete configuration/lifecycle events, zero-allocation fast-path reducers for
 *      120Hz dragging), followed by read-only pose projection to View `RenderNode` properties.
 * - Never calls `addView`/`removeView`, never modifies `clipChildren`/`clipToPadding`, and never
 *   mutates child indices via `ViewGroup.bringToFront()`.
 */
public class DeponderController internal constructor(
    public val rootView: ViewGroup,
    explicitLifecycleOwner: LifecycleOwner?,
    initialPhysics: RootPhysics,
) {

    public var physics: RootPhysics = initialPhysics
        private set

    internal val density: Float
        get() = rootView.resources.displayMetrics.density.coerceAtLeast(1e-3f)

    private var currentDensity: Float = density

    internal val xmlStretchToken: String = rootView.resources.getString(R.string.deponder_stretch)

    internal val world: SimulationWorld = SimulationWorld(initialPhysics.toEnvironmentSpec(currentDensity))

    public var scale: Float = initialPhysics.initScale.coerceIn(initialPhysics.minScale, initialPhysics.maxScale)
        private set

    public var isAwake: Boolean = false
        private set

    public var isCancelled: Boolean = false
        private set

    // Orthogonal Control-Plane sources: each listener writes only its own fact.
    private val cancelledSource = MutableStateFlow(false)
    private val hostAttachedSource = MutableStateFlow(rootView.isAttachedToWindow)
    private val lifecycleStartedSource = MutableStateFlow(true)
    private val topologyActiveSource = MutableStateFlow(false)
    private val energyAwakeSource = MutableStateFlow(false)

    internal val loopGateFlow: Flow<Boolean> = combine(
        cancelledSource,
        hostAttachedSource,
        lifecycleStartedSource,
        topologyActiveSource,
        energyAwakeSource,
    ) { cancelled, attached, started, hasBodies, energyAwake ->
        !cancelled && attached && started && hasBodies && energyAwake
    }.distinctUntilChanged()

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var observedLifecycle: Lifecycle? = null
    private var explicitOwner: LifecycleOwner? = explicitLifecycleOwner

    private var topVisualZCounter: Float = 1f
    private var orphanValidationDirty: Boolean = true
    private var lastValidatedChildCount: Int = -1
    private var orphanCheckPosted: Boolean = false
    private val orphanCheckRunnable = Runnable {
        orphanCheckPosted = false
        validateOrphanOperatorsInDirectChildren()
    }

    internal val activePlanetsById = LinkedHashMap<String, PlanetViewBinding>()
    internal val activeRubbersByKey = LinkedHashMap<EdgeKey, RubberViewBinding>()

    internal fun markOrphanValidationDirty() {
        orphanValidationDirty = true
        if (!isCancelled && !orphanCheckPosted && rootView.isAttachedToWindow) {
            orphanCheckPosted = true
            rootView.post(orphanCheckRunnable)
        }
    }

    private val rootLayoutListener = View.OnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
        val w = (right - left).coerceAtLeast(0)
        val h = (bottom - top).coerceAtLeast(0)
        if (v === rootView) {
            validateOrphanOperatorsInDirectChildren()
            dispatch(ControllerUpdateViewport(w, h))
        }
    }

    private val rootAttachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            orphanValidationDirty = true
            hostAttachedSource.value = true
            bindLifecycleIfAvailable()
            val densityChanged = syncDensityIfChanged()
            if (rootView.width > 0 || rootView.height > 0) {
                dispatch(ControllerUpdateViewport(rootView.width, rootView.height))
            } else if (densityChanged && world.bodies.isNotEmpty()) {
                energyAwakeSource.value = true
            }
            attachPendingDirectChildren()
            applyCurrentPosesToViews()
            syncDerivedFlags()
        }

        override fun onViewDetachedFromWindow(v: View) {
            rootView.removeCallbacks(orphanCheckRunnable)
            orphanCheckPosted = false
            releaseAllActivePressesAndDrags()
            hostAttachedSource.value = false
            syncDerivedFlags()
        }
    }

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME,
            -> {
                lifecycleStartedSource.value = true
                syncDerivedFlags()
            }

            Lifecycle.Event.ON_STOP -> {
                releaseAllActivePressesAndDrags()
                lifecycleStartedSource.value = false
                syncDerivedFlags()
            }

            Lifecycle.Event.ON_DESTROY -> dispatch(ControllerCancel)

            else -> Unit
        }
    }

    init {
        rootView.addOnLayoutChangeListener(rootLayoutListener)
        rootView.addOnAttachStateChangeListener(rootAttachListener)
        if (rootView.width > 0 || rootView.height > 0) {
            world.updateViewport(rootView.width.toFloat(), rootView.height.toFloat())
        }
        bindLifecycleIfAvailable()
        attachPendingDirectChildren()
        syncDerivedFlags()

        scope.launch {
            loopGateFlow.collectLatest { canRun ->
                syncDerivedFlags()
                if (!canRun) return@collectLatest
                var lastNanos = awaitChoreographerFrameNanos()
                while (isActive && !isCancelled && isAwake) {
                    val nowNanos = awaitChoreographerFrameNanos()
                    if (!isActive || isCancelled || !isAwake) break
                    val dtSeconds = (nowNanos - lastNanos) * 1e-9f
                    lastNanos = nowNanos
                    if (!stepSingleFrame(dtSeconds)) {
                        break
                    }
                }
            }
        }
    }

    private fun releaseAllActivePressesAndDrags() {
        for (binding in activePlanetsById.values) {
            cancelActiveDragProbesInSubtree(binding.view)
        }
        var releasedAny = false
        val bodies = world.bodies
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.isPressed) {
                world.setBodyPressed(body.spec.id, false)
                releasedAny = true
            }
        }
        if (releasedAny && world.bodies.isNotEmpty()) {
            energyAwakeSource.value = true
        }
    }

    private fun syncDensityIfChanged(): Boolean {
        val nextDensity = density
        if (abs(currentDensity - nextDensity) <= 1e-4f) return false
        currentDensity = nextDensity
        world.updateEnvironment(physics.toEnvironmentSpec(nextDensity), reheatSimulation = false)
        for ((id, binding) in activePlanetsById) {
            val body = world.bodyOf(id) ?: continue
            world.upsertPlanet(
                spec = binding.physics.toPlanetSpec(id, nextDensity).copy(pinned = body.spec.pinned),
                initialX = body.x,
                initialY = body.y,
                scale = scale,
            )
        }
        for ((key, binding) in activeRubbersByKey) {
            val pose = world.rubberOf(key) ?: continue
            val targetNatPx = binding.physics.resolveNaturalLengthPx(
                density = nextDensity,
                contentWidthPx = binding.currentAdaptiveContentWidthPx(),
            )
            world.upsertRubber(pose.spec.copy(naturalLengthPx = targetNatPx), scale)
        }
        world.refreshRubberPoses(scale)
        return true
    }

    private fun syncDerivedFlags() {
        val cancelled = cancelledSource.value
        isCancelled = cancelled
        isAwake = !cancelled &&
            hostAttachedSource.value &&
            lifecycleStartedSource.value &&
            topologyActiveSource.value &&
            energyAwakeSource.value
    }

    private fun completeReduction(
        energized: Boolean?,
        projectPoses: Boolean,
    ) {
        val hasBodies = world.bodies.isNotEmpty()
        topologyActiveSource.value = hasBodies
        if (energized != null) {
            energyAwakeSource.value = energized
        }
        if (projectPoses) {
            applyCurrentPosesToViews()
        }
        syncDerivedFlags()
    }

    private fun dispatch(intent: ControllerIntent) {
        if (intent is ControllerCancel) {
            reduceCancel()
            return
        }
        if (isCancelled) return
        when (intent) {
            is ControllerUpdatePhysics -> {
                if (physics == intent.physics) return
                physics = intent.physics
                scale = scale.coerceIn(intent.physics.minScale, intent.physics.maxScale)
                world.updateEnvironment(intent.physics.toEnvironmentSpec(currentDensity), reheatSimulation = true)
                world.refreshRubberPoses(scale)
                completeReduction(energized = true, projectPoses = true)
            }

            is ControllerSubmitScale -> {
                val clamped = intent.scale.coerceIn(physics.minScale, physics.maxScale)
                if (scale != clamped) {
                    scale = clamped
                    world.refreshRubberPoses(clamped)
                    world.wake(DeponderDefaults.Simulation.INTERACTION_ALPHA)
                    completeReduction(energized = true, projectPoses = true)
                }
            }

            is ControllerSubmitRubberElasticity -> {
                val rubbers = world.rubbers
                var anyChanged = false
                for (i in rubbers.indices) {
                    if (rubbers[i].spec.elasticity != intent.elasticity) {
                        anyChanged = true
                        break
                    }
                }
                if (anyChanged) {
                    world.updateAllRubbersElasticity(intent.elasticity)
                    completeReduction(
                        energized = if (world.bodies.isNotEmpty()) true else null,
                        projectPoses = true,
                    )
                }
            }

            ControllerReheat -> {
                world.reheat(DeponderDefaults.Simulation.INITIAL_ALPHA)
                completeReduction(energized = true, projectPoses = false)
            }

            is ControllerSetPinned -> {
                world.setBodyPinned(intent.id, intent.pinned)
                completeReduction(
                    energized = if (!intent.pinned) true else null,
                    projectPoses = false,
                )
            }

            is ControllerSetPressed -> {
                world.setBodyPressed(intent.id, intent.pressed)
                completeReduction(energized = true, projectPoses = false)
            }

            is ControllerBringToFront -> {
                val binding = activePlanetsById[intent.id] ?: return
                var maxVisualZ = topVisualZCounter
                for (other in activePlanetsById.values) {
                    val z = other.view.elevation + other.view.translationZ
                    if (z > maxVisualZ) maxVisualZ = z
                }
                val targetVisualZ = max(topVisualZCounter + 1f, maxVisualZ + 1f)
                topVisualZCounter = targetVisualZ
                val neededTranslationZ = targetVisualZ - binding.view.elevation
                binding.hasElevatedZ = true
                binding.elevatedTranslationZ = neededTranslationZ
                binding.view.translationZ = neededTranslationZ
            }

            is ControllerUpdateViewport -> {
                val densityChanged = syncDensityIfChanged()
                val prevW = world.viewportWidthPx
                val prevH = world.viewportHeightPx
                world.updateViewport(intent.widthPx.toFloat(), intent.heightPx.toFloat())
                val changed = world.viewportWidthPx != prevW || world.viewportHeightPx != prevH
                completeReduction(
                    energized = if ((changed || densityChanged) && world.bodies.isNotEmpty()) true else null,
                    projectPoses = true,
                )
            }

            is ControllerUpdatePlanetSize -> {
                val body = world.bodyOf(intent.id) ?: return
                val prevHalfW = body.halfWidthPx
                val prevHalfH = body.halfHeightPx
                world.setBodySize(
                    id = intent.id,
                    widthPx = intent.widthPx.toFloat(),
                    heightPx = intent.heightPx.toFloat(),
                    scale = scale,
                )
                val sizeChanged = body.halfWidthPx != prevHalfW || body.halfHeightPx != prevHalfH
                completeReduction(energized = null, projectPoses = sizeChanged)
            }

            ControllerCancel -> Unit
        }
    }

    private fun reduceCancel() {
        if (cancelledSource.value) return
        cancelledSource.value = true
        energyAwakeSource.value = false
        topologyActiveSource.value = false
        syncDerivedFlags()

        rootView.removeCallbacks(orphanCheckRunnable)
        orphanCheckPosted = false
        scope.cancel()
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycle = null
        explicitOwner = null
        rootView.removeOnLayoutChangeListener(rootLayoutListener)
        rootView.removeOnAttachStateChangeListener(rootAttachListener)

        val planetsSnapshot = ArrayList(activePlanetsById.values)
        activePlanetsById.clear()
        for (p in planetsSnapshot) {
            p.onRootCancelled()
        }

        val rubbersSnapshot = ArrayList(activeRubbersByKey.values)
        activeRubbersByKey.clear()
        for (r in rubbersSnapshot) {
            r.onRootCancelled()
        }

        if (rootView.getTag(R.id.deponder_root_controller_tag) === this) {
            rootView.setTag(R.id.deponder_root_controller_tag, null)
        }
    }

    private fun bindLifecycleIfAvailable() {
        val owner = explicitOwner ?: rootView.findViewTreeLifecycleOwner()
        val nextLifecycle = owner?.lifecycle
        if (observedLifecycle === nextLifecycle) return
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycle = nextLifecycle
        if (nextLifecycle != null) {
            if (nextLifecycle.currentState == Lifecycle.State.DESTROYED) {
                dispatch(ControllerCancel)
                return
            }
            lifecycleStartedSource.value = nextLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            nextLifecycle.addObserver(lifecycleObserver)
            syncDerivedFlags()
        } else {
            lifecycleStartedSource.value = true
            syncDerivedFlags()
        }
    }

    internal fun attachPendingDirectChildren() {
        if (isCancelled) return
        for (i in 0 until rootView.childCount) {
            val child = rootView.getChildAt(i)
            val planetBinding = child.getTag(R.id.deponder_planet_binding_tag) as? PlanetViewBinding
            if (planetBinding != null && planetBinding.attachedController == null && child.isAttachedToWindow) {
                planetBinding.attachToRootController(this)
            }
            val rubberBinding = child.getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
            if (rubberBinding != null && rubberBinding.attachedController == null && child.isAttachedToWindow) {
                rubberBinding.attachToRootController(this)
            }
        }
    }

    internal fun validateOrphanOperatorsInDirectChildren() {
        if (isCancelled || !rootView.isAttachedToWindow) return
        val count = rootView.childCount
        orphanValidationDirty = false
        lastValidatedChildCount = count
        for (i in 0 until count) {
            val child = rootView.getChildAt(i)
            val isRubber = child.getTag(R.id.deponder_rubber_binding_tag) != null
            val isPlanet = child.getTag(R.id.deponder_planet_binding_tag) != null
            if (!isRubber) {
                if (isPlanet) {
                    assertNoStretchInPlanetSubtree(child, xmlStretchToken)
                } else {
                    assertNoOrphanProgrammaticStretchInSubtree(child)
                }
            }
            if (!isPlanet) {
                assertNoOrphanDragInSubtree(child)
            }
        }
    }

    internal fun minPlanetVisualZOrZero(): Float {
        var minZ = 0f
        for (p in activePlanetsById.values) {
            val z = p.view.elevation + p.view.translationZ
            if (z < minZ) minZ = z
        }
        return minZ
    }

    /**
     * Updates the root container [RootPhysics] while preserving all active planet coordinates and velocities.
     */
    public fun updatePhysics(newPhysics: RootPhysics) {
        dispatch(ControllerUpdatePhysics(newPhysics))
    }

    /**
     * Updates the simulation zoom scale within `[physics.minScale, physics.maxScale]` and wakes the loop.
     */
    public fun submitScale(newScale: Float) {
        require(newScale.isFinite()) { "scale must be finite, but was $newScale" }
        dispatch(ControllerSubmitScale(newScale))
    }

    /**
     * Dynamically updates the spring [elasticity] across all bound rubbers and wakes the physics loop.
     */
    public fun submitRubberElasticity(elasticity: Float) {
        require(elasticity.isFinite()) { "elasticity must be finite, but was $elasticity" }
        dispatch(ControllerSubmitRubberElasticity(elasticity))
    }

    /**
     * Reheats simulated annealing and wakes the physics VSYNC loop if sleeping.
     */
    public fun reheat() {
        dispatch(ControllerReheat)
    }

    /**
     * Dynamically pins or unpins planet [id].
     */
    public fun setPinned(id: String, pinned: Boolean) {
        dispatch(ControllerSetPinned(id, pinned))
    }

    /**
     * Marks planet [id] as actively pressed/held (`pressed = true` locks velocity and elevates interaction alpha).
     */
    public fun setPressed(id: String, pressed: Boolean) {
        dispatch(ControllerSetPressed(id, pressed))
    }

    /**
     * Offsets planet [id] by `(dxPx, dyPx)` in static [rootView] pixel coordinates and wakes neighbors.
     *
     * Fast-path zero-allocation Data-Plane reducer so 120Hz touch dragging maintains 0-frame input latency.
     */
    public fun dragBy(id: String, dxPx: Float, dyPx: Float) {
        require(dxPx.isFinite() && dyPx.isFinite()) {
            "dragBy delta must be finite, but were ($dxPx, $dyPx)"
        }
        if (isCancelled || (dxPx == 0f && dyPx == 0f)) return
        world.offsetBodyPosition(id, dxPx, dyPx, scale)
        completeReduction(energized = true, projectPoses = true)
    }

    /**
     * Moves planet [id] directly to `(xPx, yPx)` in static [rootView] pixel coordinates and wakes neighbors.
     *
     * Fast-path zero-allocation Data-Plane reducer so 120Hz touch dragging maintains 0-frame input latency.
     */
    public fun moveTo(id: String, xPx: Float, yPx: Float) {
        require(xPx.isFinite() && yPx.isFinite()) {
            "moveTo coordinates must be finite, but were ($xPx, $yPx)"
        }
        if (isCancelled) return
        world.setBodyPosition(id, xPx, yPx, scale)
        completeReduction(energized = true, projectPoses = true)
    }

    /**
     * Elevates planet [id]'s visual draw Z-order (`elevation + translationZ`) strictly above all
     * sibling planets (including high-elevation `CardView`s) without mutating `ViewGroup` child order.
     */
    public fun bringToFront(id: String) {
        dispatch(ControllerBringToFront(id))
    }

    /**
     * Permanently detaches this controller from [rootView], cancels VSYNC coroutines, and resets bound view transforms.
     */
    public fun cancel() {
        dispatch(ControllerCancel)
    }

    internal fun updatePlanetMeasuredSize(id: String, widthPx: Int, heightPx: Int) {
        dispatch(ControllerUpdatePlanetSize(id, widthPx, heightPx))
    }

    internal fun registerPlanetBinding(binding: PlanetViewBinding) {
        val existing = activePlanetsById[binding.planetId]
        check(existing == null || existing === binding) {
            "Duplicate deponderPlanet id '${binding.planetId}' under the same deponderRoot."
        }
        assertNoStretchInPlanetSubtree(
            binding.view,
            xmlStretchToken,
        )
        activePlanetsById[binding.planetId] = binding
        if (binding.hasLaidOutBounds()) {
            seedOrUpdatePlanetBody(binding)
        }
    }

    internal fun seedOrUpdatePlanetBody(binding: PlanetViewBinding): PlanetBody {
        val cx = binding.staticCenterX()
        val cy = binding.staticCenterY()
        world.upsertPlanet(binding.physics.toPlanetSpec(binding.planetId, currentDensity), cx, cy, scale)
        world.setBodySize(
            id = binding.planetId,
            widthPx = binding.view.width.toFloat(),
            heightPx = binding.view.height.toFloat(),
            scale = scale,
        )
        val body = world.bodyOf(binding.planetId)!!
        completeReduction(energized = true, projectPoses = true)
        return body
    }

    internal fun rebindPlanetIdOrPhysics(
        binding: PlanetViewBinding,
        oldId: String,
        newId: String,
    ) {
        if (oldId != newId) {
            val conflict = activePlanetsById[newId]
            check(conflict == null || conflict === binding) {
                "Duplicate deponderPlanet id '$newId' under the same deponderRoot."
            }
            val prevBody = world.bodyOf(oldId)
            val seedX = prevBody?.x ?: binding.staticCenterX()
            val seedY = prevBody?.y ?: binding.staticCenterY()
            activePlanetsById.remove(oldId)
            world.removePlanet(oldId, scale)
            activePlanetsById[newId] = binding
            if (binding.hasLaidOutBounds() || prevBody != null) {
                world.upsertPlanet(binding.physics.toPlanetSpec(newId, currentDensity), seedX, seedY, scale)
                world.setBodySize(
                    id = newId,
                    widthPx = binding.view.width.toFloat(),
                    heightPx = binding.view.height.toFloat(),
                    scale = scale,
                )
            }
        } else if (binding.hasLaidOutBounds() || world.bodyOf(newId) != null) {
            val prevBody = world.bodyOf(newId)
            val seedX = prevBody?.x ?: binding.staticCenterX()
            val seedY = prevBody?.y ?: binding.staticCenterY()
            world.upsertPlanet(binding.physics.toPlanetSpec(newId, currentDensity), seedX, seedY, scale)
            world.setBodySize(
                id = newId,
                widthPx = binding.view.width.toFloat(),
                heightPx = binding.view.height.toFloat(),
                scale = scale,
            )
        }
        completeReduction(energized = if (world.bodies.isNotEmpty()) true else null, projectPoses = true)
    }

    internal fun unregisterPlanetBinding(binding: PlanetViewBinding) {
        if (activePlanetsById[binding.planetId] === binding) {
            activePlanetsById.remove(binding.planetId)
            world.removePlanet(binding.planetId, scale)
            completeReduction(
                energized = world.bodies.isNotEmpty(),
                projectPoses = true,
            )
        }
    }

    internal fun registerRubberBinding(binding: RubberViewBinding): RubberPose {
        val key = EdgeKey(binding.startId, binding.endId)
        val existing = activeRubbersByKey[key]
        check(existing == null || existing === binding) {
            "Duplicate deponderRubber link between '${binding.startId}' and '${binding.endId}' (${key.encoded}) under the same deponderRoot."
        }
        assertNoOrphanDragInSubtree(binding.view)
        activeRubbersByKey[key] = binding
        val pose = world.upsertRubber(
            binding.physics.toRubberSpec(
                startId = binding.startId,
                endId = binding.endId,
                density = currentDensity,
                contentWidthPx = binding.currentAdaptiveContentWidthPx(),
            ),
            scale,
        )
        completeReduction(energized = true, projectPoses = true)
        return pose
    }

    internal fun rebindRubberEndpointsOrPhysics(
        binding: RubberViewBinding,
        oldKey: EdgeKey,
    ) {
        val newKey = EdgeKey(binding.startId, binding.endId)
        if (oldKey != newKey) {
            val conflict = activeRubbersByKey[newKey]
            check(conflict == null || conflict === binding) {
                "Duplicate deponderRubber link between '${binding.startId}' and '${binding.endId}' (${newKey.encoded}) under the same deponderRoot."
            }
            if (activeRubbersByKey[oldKey] === binding) {
                activeRubbersByKey.remove(oldKey)
                world.removeRubber(oldKey, scale)
            }
            activeRubbersByKey[newKey] = binding
        }
        world.upsertRubber(
            binding.physics.toRubberSpec(
                startId = binding.startId,
                endId = binding.endId,
                density = currentDensity,
                contentWidthPx = binding.currentAdaptiveContentWidthPx(),
            ),
            scale,
        )
        completeReduction(energized = true, projectPoses = true)
    }

    internal fun syncRubberAdaptiveLength(binding: RubberViewBinding) {
        if (isCancelled || !binding.physics.isAutoLength) return
        val key = EdgeKey(binding.startId, binding.endId)
        if (activeRubbersByKey[key] !== binding) return
        val pose = world.rubberOf(key) ?: return
        val targetNatPx = binding.physics.resolveNaturalLengthPx(
            density = currentDensity,
            contentWidthPx = binding.currentAdaptiveContentWidthPx(),
        )
        if (abs(pose.spec.naturalLengthPx - targetNatPx) > 0.05f) {
            world.upsertRubber(pose.spec.copy(naturalLengthPx = targetNatPx), scale)
            completeReduction(energized = true, projectPoses = false)
        }
    }

    internal fun unregisterRubberBinding(binding: RubberViewBinding) {
        val key = EdgeKey(binding.startId, binding.endId)
        if (activeRubbersByKey[key] === binding) {
            activeRubbersByKey.remove(key)
            world.removeRubber(key, scale)
            completeReduction(
                energized = if (world.bodies.isNotEmpty()) true else null,
                projectPoses = true,
            )
        }
    }

    /**
     * Advances a single simulation step synchronously (also used directly in deterministic unit tests).
     */
    internal suspend fun stepSingleFrame(dtSeconds: Float): Boolean {
        if (isCancelled) return false
        if (orphanValidationDirty || rootView.childCount != lastValidatedChildCount) {
            validateOrphanOperatorsInDirectChildren()
        }
        val keepRunning = world.step(dtSeconds, scale)
        completeReduction(
            energized = if (keepRunning) null else false,
            projectPoses = true,
        )
        return keepRunning
    }

    internal fun applyCurrentPosesToViews() {
        for (binding in activePlanetsById.values) {
            val body = world.bodyOf(binding.planetId) ?: continue
            binding.applyBodyPose(body, scale)
        }
        for ((key, binding) in activeRubbersByKey) {
            val pose = world.rubberOf(key)
            binding.applyRubberPose(pose)
        }
    }
}
