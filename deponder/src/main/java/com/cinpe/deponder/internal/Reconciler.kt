package com.cinpe.deponder.internal

import android.view.View
import androidx.annotation.MainThread
import com.cinpe.deponder.BindAdapter
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.model.SimplePlanet
import com.cinpe.deponder.model.SimpleRubber
import com.cinpe.deponder.option.SimpleRootOption
import java.util.Random

internal fun hashCapacity(expectedSize: Int): Int = when {
    expectedSize <= 0 -> 0
    expectedSize < 3 -> expectedSize + 1
    expectedSize < 1_073_741_824 -> ((expectedSize.toFloat() / 0.75f) + 1.0f).toInt()
    else -> Int.MAX_VALUE
}

/**
 * Replaces Guava `Maps.difference` with an allocation-light unordered diff.
 */
internal class MapDiff<V>(
    val onlyLeftIds: List<String>,
    val onlyRight: Map<String, V>,
    val differingRight: Map<String, V>,
) {
    val isEmpty: Boolean
        get() = onlyLeftIds.isEmpty() && onlyRight.isEmpty() && differingRight.isEmpty()

    companion object {
        fun <V> compute(
            oldMap: Map<String, V>,
            newMap: Map<String, V>,
            areEqual: (V, V) -> Boolean,
        ): MapDiff<V> {
            val onlyLeftIds = ArrayList<String>()
            val onlyRight = HashMap<String, V>()
            val differing = HashMap<String, V>()

            for (k in oldMap.keys) {
                if (!newMap.containsKey(k)) {
                    onlyLeftIds.add(k)
                }
            }
            for ((k, newVal) in newMap) {
                if (!oldMap.containsKey(k)) {
                    onlyRight[k] = newVal
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val oldVal = oldMap[k] as V
                    if (!areEqual(oldVal, newVal)) {
                        differing[k] = newVal
                    }
                }
            }
            return MapDiff(
                onlyLeftIds = onlyLeftIds,
                onlyRight = onlyRight,
                differingRight = differing,
            )
        }
    }
}

/**
 * Stateful, idempotent main-thread reconciler for planets (replaces `poClt` `buffer(2,1)` + `Maps.difference` + `scan`).
 *
 * Idempotency on equal consecutive snapshots guarantees safe replay across `Lifecycle` stop/start cycles.
 */
internal class PlanetReconciler<P>(
    private val rootOption: SimpleRootOption,
    private val adapter: BindAdapter<P, *>,
    private val random: Random = Random(),
) {
    private var previous: Map<String, P> = emptyMap()
    private val active: HashMap<String, SimplePlanet> = HashMap()
    private val activeTypes: HashMap<String, Int> = HashMap()
    private val cacheByType: HashMap<Int, ArrayList<SimplePlanet>> = HashMap()

    private fun ensurePlanetId(sp: SimplePlanet, expectedId: String): SimplePlanet =
        if (sp.id() == expectedId) sp else sp.toBuilder().id(expectedId).build()

    private fun copyViewTransform(from: View, to: View) {
        to.translationX = from.translationX
        to.translationY = from.translationY
        to.scaleX = from.scaleX
        to.scaleY = from.scaleY
    }

    /**
     * Applies [items] if anything changed. Returns the updated active list, or `null` if nothing changed.
     */
    @MainThread
    fun reconcile(items: List<P>): List<SimplePlanet>? {
        val next = HashMap<String, P>(hashCapacity(items.size))
        for (p in items) {
            next[adapter.planetId(p)] = p
        }
        val diff = MapDiff.compute(previous, next) { a, b ->
            adapter.typePlanet(a) == adapter.typePlanet(b) &&
                if (a != null && b != null) adapter.areContentsTheSameByP(a, b) else a == b
        }
        if (diff.isEmpty) {
            return null
        }
        previous = next

        // 1. Move removed planets to the type-bucketed reuse pool.
        for (id in diff.onlyLeftIds) {
            val removed = active.remove(id)
            val oldType = activeTypes.remove(id) ?: 0
            if (removed != null) {
                cacheByType.getOrPut(oldType) { ArrayList() }.add(removed)
            }
        }

        // 2. Reconcile newly added planets, reusing from matching viewType bucket when available.
        for ((id, p) in diff.onlyRight) {
            val viewType = adapter.typePlanet(p)
            val bucket = cacheByType[viewType]
            val sp = if (!bucket.isNullOrEmpty()) {
                val holder = bucket.removeAt(bucket.size - 1)
                ensurePlanetId(adapter.bindPlanet(holder, p), id)
            } else {
                ensurePlanetId(adapter.createPlanet(rootOption.itemView(), p), id)
            }
            active[id] = sp
            activeTypes[id] = viewType
            val v = sp.itemView()
            if (v.parent == null) {
                rootOption.itemView().addView(v)
            }
            DeponderHelper.bindDefTouchPlanet(sp)
            v.translationX = random.nextFloat()
            v.translationY = random.nextFloat()
            v.scaleX = rootOption.initScale()
            v.scaleY = rootOption.initScale()
        }

        // 3. Rebind modified planets in place while preserving position and scale.
        for ((id, p) in diff.differingRight) {
            val existing = active[id] ?: continue
            val oldType = activeTypes[id] ?: 0
            val newType = adapter.typePlanet(p)
            if (oldType != newType) {
                cacheByType.getOrPut(oldType) { ArrayList() }.add(existing)
                val bucket = cacheByType[newType]
                val raw = if (!bucket.isNullOrEmpty()) {
                    adapter.bindPlanet(bucket.removeAt(bucket.size - 1), p)
                } else {
                    adapter.createPlanet(rootOption.itemView(), p)
                }
                val rebound = ensurePlanetId(raw, id)
                copyViewTransform(existing.itemView(), rebound.itemView())
                active[id] = rebound
                activeTypes[id] = newType
                if (rebound.itemView().parent == null) {
                    rootOption.itemView().addView(rebound.itemView())
                }
                DeponderHelper.bindDefTouchPlanet(rebound)
            } else {
                val raw = adapter.bindPlanet(existing, p)
                if (raw.itemView() === existing.itemView()) {
                    val rebound = ensurePlanetId(raw, id)
                    active[id] = rebound
                } else {
                    existing.itemView().setOnTouchListener(null)
                    rootOption.itemView().removeView(existing.itemView())
                    val rebound = ensurePlanetId(raw, id)
                    copyViewTransform(existing.itemView(), rebound.itemView())
                    active[id] = rebound
                    rootOption.itemView().addView(rebound.itemView())
                    DeponderHelper.bindDefTouchPlanet(rebound)
                }
            }
        }

        // 4. Detach any leftover cached views from the parent ViewGroup.
        for (bucket in cacheByType.values) {
            for (po in bucket) {
                if (po.itemView().parent != null) {
                    po.itemView().setOnTouchListener(null)
                    rootOption.itemView().removeView(po.itemView())
                }
            }
        }

        return ArrayList(active.values)
    }
}

/**
 * Stateful, idempotent main-thread reconciler for rubbers (replaces `roClt` `combineLatest` + `Maps.difference` + `scan`).
 */
internal class RubberReconciler<R>(
    private val rootOption: SimpleRootOption,
    private val adapter: BindAdapter<*, R>,
) {
    private var previous: Map<String, R> = emptyMap()
    private val active: HashMap<String, SimpleRubber> = HashMap()
    private val activeTypes: HashMap<String, Int> = HashMap()
    private val cacheByType: HashMap<Int, ArrayList<SimpleRubber>> = HashMap()

    private fun detachRubberView(ro: SimpleRubber) {
        rootOption.itemView().removeView(ro.itemView())
    }

    private fun ensureRubberEndpoints(ro: SimpleRubber, sId: String, eId: String): SimpleRubber =
        if (ro.sId() == sId && ro.eId() == eId) {
            ro
        } else {
            ro.toBuilder().sId(sId).eId(eId).build().withRuntimeStateFrom(ro)
        }

    /**
     * Filters [items] to non-self-loop rubbers whose endpoints both exist in [planetIds] and reconciles views if changed.
     * Returns the updated active map keyed by `SimpleRubber.id()`, or `null` if nothing changed.
     */
    @MainThread
    fun reconcile(items: List<R>, planetIds: Set<String>): Map<String, SimpleRubber>? {
        val next = HashMap<String, R>(hashCapacity(items.size))
        for (r in items) {
            val pair = adapter.rubberPairId(r)
            if (pair.first != pair.second && pair.first in planetIds && pair.second in planetIds) {
                next[adapter.rubberId(r)] = r
            }
        }

        val diff = MapDiff.compute(previous, next) { a, b ->
            adapter.typeRubber(a) == adapter.typeRubber(b) &&
                if (a != null && b != null) adapter.areContentsTheSameByR(a, b) else a == b
        }
        if (diff.isEmpty) {
            return null
        }
        previous = next

        // 1. Detach removed rubbers and push them into the type-bucketed reuse pool.
        for (id in diff.onlyLeftIds) {
            val ro = active.remove(id) ?: continue
            val oldType = activeTypes.remove(id) ?: 0
            detachRubberView(ro)
            cacheByType.getOrPut(oldType) { ArrayList() }.add(ro)
        }

        // 2. Reconcile added, modified, and unchanged rubbers keyed consistently by `adapter.rubberId(r)`.
        for ((id, r) in diff.onlyRight) {
            val pair = adapter.rubberPairId(r)
            val viewType = adapter.typeRubber(r)
            val bucket = cacheByType[viewType]
            val ro = if (bucket.isNullOrEmpty()) {
                ensureRubberEndpoints(adapter.createRubber(rootOption.itemView(), r), pair.first, pair.second)
            } else {
                val holder = bucket.removeAt(bucket.size - 1)
                val bound = adapter.bindRubber(holder, r)
                val withCache = if (bound.itemView() === holder.itemView()) bound.withRuntimeStateFrom(holder) else bound
                ensureRubberEndpoints(withCache, pair.first, pair.second)
            }
            active[id] = ro
            activeTypes[id] = viewType
        }
        for ((id, r) in diff.differingRight) {
            val existing = active[id] ?: continue
            val pair = adapter.rubberPairId(r)
            val oldType = activeTypes[id] ?: 0
            val newType = adapter.typeRubber(r)
            val ro = if (oldType != newType) {
                detachRubberView(existing)
                cacheByType.getOrPut(oldType) { ArrayList() }.add(existing)
                val bucket = cacheByType[newType]
                if (bucket.isNullOrEmpty()) {
                    ensureRubberEndpoints(adapter.createRubber(rootOption.itemView(), r), pair.first, pair.second)
                } else {
                    val holder = bucket.removeAt(bucket.size - 1)
                    val bound = adapter.bindRubber(holder, r)
                    val withCache = if (bound.itemView() === holder.itemView()) bound.withRuntimeStateFrom(holder) else bound
                    ensureRubberEndpoints(withCache, pair.first, pair.second)
                }
            } else {
                val raw = adapter.bindRubber(existing, r)
                if (raw.itemView() === existing.itemView()) {
                    ensureRubberEndpoints(raw.withRuntimeStateFrom(existing), pair.first, pair.second)
                } else {
                    detachRubberView(existing)
                    ensureRubberEndpoints(raw, pair.first, pair.second)
                }
            }
            active[id] = ro
            activeTypes[id] = newType
        }

        // 3. Attach active rubber views and index active rubbers by physics pair ID.
        val byPhysicsId = HashMap<String, SimpleRubber>(hashCapacity(active.size))
        for (ro in active.values) {
            byPhysicsId[ro.id()] = ro
            if (ro.itemView().parent == null) {
                rootOption.itemView().addView(ro.itemView())
            }
        }

        return byPhysicsId
    }
}
