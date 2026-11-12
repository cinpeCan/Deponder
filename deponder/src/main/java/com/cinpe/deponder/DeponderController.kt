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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Imperative physics controller bound to a host [ViewGroup] via [deponderRoot].
 *
 * - Never calls `addView`/`removeView`, never modifies `clipChildren`/`clipToPadding`, and never
 *   mutates child indices via `ViewGroup.bringToFront()`.
 * - Automatically pauses the Choreographer VSYNC loop when [Lifecycle.State] drops below
 *   [Lifecycle.State.STARTED] or when [rootView] detaches from window, preserving all bound
 *   planet/rubber topologies and coordinates for seamless resume.
 */
public class DeponderController internal constructor(
    public val rootView: ViewGroup,
    explicitLifecycleOwner: LifecycleOwner?,
    initialPhysics: RootPhysics,
) {
    public var physics: RootPhysics = initialPhysics
        private set

    private val density: Float
        get() = rootView.resources.displayMetrics.density.coerceAtLeast(1e-3f)

    internal val xmlStretchToken: String = rootView.resources.getString(R.string.deponder_stretch)

    internal val world: SimulationWorld = SimulationWorld(initialPhysics.toEnvironmentSpec(density))

    public var scale: Float = initialPhysics.initScale.coerceIn(initialPhysics.minScale, initialPhysics.maxScale)
        private set

    public var isAwake: Boolean = false
        private set

    public var isCancelled: Boolean = false
        private set

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var frameLoopJob: Job? = null

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
            world.updateViewport(w.toFloat(), h.toFloat())
            applyCurrentPosesToViews()
            ensureFrameLoopIfEligible()
        }
    }

    private val rootAttachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            orphanValidationDirty = true
            bindLifecycleIfAvailable()
            if (rootView.width > 0 || rootView.height > 0) {
                world.updateViewport(rootView.width.toFloat(), rootView.height.toFloat())
            }
            attachPendingDirectChildren()
            applyCurrentPosesToViews()
            ensureFrameLoopIfEligible()
        }

        override fun onViewDetachedFromWindow(v: View) {
            rootView.removeCallbacks(orphanCheckRunnable)
            orphanCheckPosted = false
            pauseFrameLoop()
        }
    }

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME,
            -> ensureFrameLoopIfEligible()

            Lifecycle.Event.ON_STOP -> pauseFrameLoop()

            Lifecycle.Event.ON_DESTROY -> cancel()

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
    }

    private fun bindLifecycleIfAvailable() {
        val owner = explicitOwner ?: rootView.findViewTreeLifecycleOwner()
        val nextLifecycle = owner?.lifecycle
        if (observedLifecycle === nextLifecycle) return
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycle = nextLifecycle
        if (nextLifecycle != null) {
            if (nextLifecycle.currentState == Lifecycle.State.DESTROYED) {
                cancel()
                return
            }
            nextLifecycle.addObserver(lifecycleObserver)
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
            if (child.getTag(R.id.deponder_rubber_binding_tag) == null) {
                assertNoOrphanXmlStretchInSubtree(child, xmlStretchToken)
            }
            if (child.getTag(R.id.deponder_planet_binding_tag) == null) {
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
        if (isCancelled || physics == newPhysics) return
        physics = newPhysics
        scale = scale.coerceIn(newPhysics.minScale, newPhysics.maxScale)
        world.updateEnvironment(newPhysics.toEnvironmentSpec(density), reheatSimulation = true)
        world.refreshRubberPoses(scale)
        applyCurrentPosesToViews()
        wakeUpInternal()
    }

    /**
     * Updates the simulation zoom scale within `[physics.minScale, physics.maxScale]` and wakes the loop.
     */
    public fun submitScale(newScale: Float) {
        if (isCancelled) return
        val clamped = newScale.coerceIn(physics.minScale, physics.maxScale)
        if (scale != clamped) {
            scale = clamped
            world.refreshRubberPoses(clamped)
            world.reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
            applyCurrentPosesToViews()
            wakeUpInternal()
        }
    }

    /**
     * Dynamically updates the spring [elasticity] across all bound rubbers and wakes the physics loop.
     */
    public fun submitRubberElasticity(elasticity: Float) {
        if (isCancelled) return
        world.updateAllRubbersElasticity(elasticity)
        wakeUpInternal()
    }

    /**
     * Reheats simulated annealing and wakes the physics VSYNC loop if sleeping.
     */
    public fun reheat() {
        if (isCancelled) return
        world.reheat(DeponderDefaults.Simulation.INITIAL_ALPHA)
        wakeUpInternal()
    }

    /**
     * Dynamically pins or unpins planet [id].
     */
    public fun setPinned(id: String, pinned: Boolean) {
        if (isCancelled) return
        world.setBodyPinned(id, pinned)
        if (!pinned) {
            wakeUpInternal()
        }
    }

    /**
     * Marks planet [id] as actively pressed/held (`pressed = true` locks velocity and elevates interaction alpha).
     */
    public fun setPressed(id: String, pressed: Boolean) {
        if (isCancelled) return
        world.setBodyPressed(id, pressed)
        wakeUpInternal()
    }

    /**
     * Offsets planet [id] by `(dxPx, dyPx)` in static [rootView] pixel coordinates and wakes neighbors.
     */
    public fun dragBy(id: String, dxPx: Float, dyPx: Float) {
        if (isCancelled || (dxPx == 0f && dyPx == 0f)) return
        world.offsetBodyPosition(id, dxPx, dyPx, scale)
        applyCurrentPosesToViews()
        wakeUpInternal()
    }

    /**
     * Moves planet [id] directly to `(xPx, yPx)` in static [rootView] pixel coordinates and wakes neighbors.
     */
    public fun moveTo(id: String, xPx: Float, yPx: Float) {
        if (isCancelled) return
        world.setBodyPosition(id, xPx, yPx, scale)
        applyCurrentPosesToViews()
        wakeUpInternal()
    }

    /**
     * Elevates planet [id]'s visual draw Z-order (`elevation + translationZ`) strictly above all
     * sibling planets (including high-elevation `CardView`s) without mutating `ViewGroup` child order.
     */
    public fun bringToFront(id: String) {
        if (isCancelled) return
        val binding = activePlanetsById[id] ?: return
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

    /**
     * Permanently detaches this controller from [rootView], cancels VSYNC coroutines, and resets bound view transforms.
     */
    public fun cancel() {
        if (isCancelled) return
        isCancelled = true
        isAwake = false
        rootView.removeCallbacks(orphanCheckRunnable)
        orphanCheckPosted = false
        pauseFrameLoop()
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

    internal fun registerPlanetBinding(binding: PlanetViewBinding) {
        val existing = activePlanetsById[binding.planetId]
        check(existing == null || existing === binding) {
            "Duplicate deponderPlanet id '${binding.planetId}' under the same deponderRoot."
        }
        assertNoOrphanXmlStretchInSubtree(
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
        world.upsertPlanet(binding.physics.toPlanetSpec(binding.planetId, density), cx, cy, scale)
        world.setBodySize(
            id = binding.planetId,
            widthPx = binding.view.width.toFloat(),
            heightPx = binding.view.height.toFloat(),
            scale = scale,
        )
        val body = world.bodyOf(binding.planetId)!!
        applyCurrentPosesToViews()
        wakeUpInternal()
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
                world.upsertPlanet(binding.physics.toPlanetSpec(newId, density), seedX, seedY, scale)
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
            world.upsertPlanet(binding.physics.toPlanetSpec(newId, density), seedX, seedY, scale)
            world.setBodySize(
                id = newId,
                widthPx = binding.view.width.toFloat(),
                heightPx = binding.view.height.toFloat(),
                scale = scale,
            )
        }
        applyCurrentPosesToViews()
        wakeUpInternal()
    }

    internal fun unregisterPlanetBinding(binding: PlanetViewBinding) {
        if (activePlanetsById[binding.planetId] === binding) {
            activePlanetsById.remove(binding.planetId)
            world.removePlanet(binding.planetId, scale)
            applyCurrentPosesToViews()
            wakeUpInternal()
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
            binding.physics.toRubberSpec(binding.startId, binding.endId, density),
            scale,
        )
        applyCurrentPosesToViews()
        wakeUpInternal()
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
            binding.physics.toRubberSpec(binding.startId, binding.endId, density),
            scale,
        )
        applyCurrentPosesToViews()
        wakeUpInternal()
    }

    internal fun unregisterRubberBinding(binding: RubberViewBinding) {
        val key = EdgeKey(binding.startId, binding.endId)
        if (activeRubbersByKey[key] === binding) {
            activeRubbersByKey.remove(key)
            world.removeRubber(key, scale)
            applyCurrentPosesToViews()
            wakeUpInternal()
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
        applyCurrentPosesToViews()
        if (!keepRunning) {
            isAwake = false
        }
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

    private fun wakeUpInternal() {
        if (isCancelled || world.bodies.isEmpty()) return
        isAwake = true
        ensureFrameLoopIfEligible()
    }

    private fun isLifecycleStartedOrUnbound(): Boolean {
        val lc = observedLifecycle ?: return true
        return lc.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun ensureFrameLoopIfEligible() {
        if (isCancelled || !isAwake || world.bodies.isEmpty()) return
        if (!rootView.isAttachedToWindow || !isLifecycleStartedOrUnbound()) return
        if (frameLoopJob?.isActive == true) return

        frameLoopJob = scope.launch {
            var lastNanos = awaitChoreographerFrameNanos()
            while (isActive && isAwake && rootView.isAttachedToWindow && isLifecycleStartedOrUnbound()) {
                val nowNanos = awaitChoreographerFrameNanos()
                val dtSeconds = (nowNanos - lastNanos) * 1e-9f
                lastNanos = nowNanos
                if (!stepSingleFrame(dtSeconds)) {
                    break
                }
            }
        }
    }

    private fun pauseFrameLoop() {
        frameLoopJob?.cancel()
        frameLoopJob = null
    }
}
