package com.cinpe.deponder.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetBody
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import com.cinpe.deponder.core.RubberPose
import com.cinpe.deponder.core.SimulationWorld
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

private sealed interface StateIntent
private data class StateUpdatePhysics(val physics: RootPhysics) : StateIntent
private data class StateSubmitScale(val scale: Float) : StateIntent
private data class StateSubmitRubberElasticity(val elasticity: Float) : StateIntent
private data object StateReheat : StateIntent
private data class StateSetPinned(val id: String, val pinned: Boolean) : StateIntent
private data class StateSetPressed(val id: String, val pressed: Boolean) : StateIntent
private data class StateBringToFront(val id: String) : StateIntent
private data class StateUpdateDensity(val density: Float) : StateIntent
private data class StateUpdateViewportSize(val widthPx: Int, val heightPx: Int) : StateIntent
private data class StateUpdatePlanetSize(val id: String, val widthPx: Int, val heightPx: Int) : StateIntent

/**
 * Imperative physics state and controller for a [deponderRoot] container in Compose Multiplatform.
 *
 * Implements a Dual-Plane Unidirectional Data Flow (UDF):
 * - **Control Plane**: Orthogonal host-attachment, non-empty topology, and kinetic-energy sources
 *   merged with [combine] + [distinctUntilChanged] into [loopGateFlow], so detaching or swapping
 *   hosts never corrupts unsettled mechanical energy and settled graphs stay at 0% CPU.
 * - **Data Plane**: State mutations flow through a single reducer pipeline (`StateIntent` for discrete
 *   configuration/registration updates, zero-allocation fast paths for 120Hz pointer dragging),
 *   projecting via non-recomposable [frameTick] reads in graphics layer scopes only.
 */
@Stable
public class DeponderState(
    physics: RootPhysics = RootPhysics(),
) {

    public var physics: RootPhysics = physics
        private set

    internal val world: SimulationWorld = SimulationWorld(physics.toEnvironmentSpec(density = 1f))

    internal var currentDensity: Float = 1f
        private set
    private var attachedRootNode: DeponderRootNode? = null

    public var scale: Float by mutableFloatStateOf(
        physics.initScale.coerceIn(physics.minScale, physics.maxScale),
    )
        private set

    public var isAwake: Boolean by mutableStateOf(false)
        private set

    // Orthogonal Control-Plane sources: each owner writes only its own fact.
    private val hostAttachedSource = MutableStateFlow(false)
    private val topologyActiveSource = MutableStateFlow(false)
    private val energyAwakeSource = MutableStateFlow(false)

    internal val loopGateFlow: Flow<Boolean> = combine(
        hostAttachedSource,
        topologyActiveSource,
        energyAwakeSource,
    ) { attached, hasBodies, energyAwake ->
        attached && hasBodies && energyAwake
    }.distinctUntilChanged()

    private var rawFrameTick: Long = 0L
    private val frameTickState = mutableLongStateOf(0L)

    /**
     * Monotonically increasing tick read exclusively inside `placeWithLayer` / draw scopes so
     * physics steps never trigger `@Composable` recompositions or parent re-measurements.
     */
    internal val frameTick: Long
        get() = frameTickState.longValue

    private var topZCounter: Float = 1f

    internal val activePlanetsById = LinkedHashMap<String, DeponderPlanetNode>()
    internal val activeRubbersByKey = LinkedHashMap<EdgeKey, DeponderRubberNode>()

    private fun syncDerivedAwakeFlag() {
        val nextAwake = hostAttachedSource.value &&
            topologyActiveSource.value &&
            energyAwakeSource.value
        if (isAwake != nextAwake) {
            isAwake = nextAwake
        }
    }

    private fun completeReduction(
        energized: Boolean?,
        notifyFrame: Boolean,
    ) {
        val hasBodies = world.bodies.isNotEmpty()
        topologyActiveSource.value = hasBodies
        if (energized != null) {
            energyAwakeSource.value = energized
        }
        if (notifyFrame) {
            notifyFrameChanged()
        }
        syncDerivedAwakeFlag()
    }

    private fun dispatch(intent: StateIntent) {
        when (intent) {
            is StateUpdatePhysics -> {
                if (physics == intent.physics) return
                physics = intent.physics
                val clampedScale = scale.coerceIn(intent.physics.minScale, intent.physics.maxScale)
                if (scale != clampedScale) {
                    scale = clampedScale
                }
                world.updateEnvironment(intent.physics.toEnvironmentSpec(currentDensity), reheatSimulation = true)
                world.refreshRubberPoses(scale)
                completeReduction(energized = true, notifyFrame = true)
            }

            is StateSubmitScale -> {
                val clamped = intent.scale.coerceIn(physics.minScale, physics.maxScale)
                if (scale != clamped) {
                    scale = clamped
                    world.refreshRubberPoses(clamped)
                    world.wake(DeponderDefaults.Simulation.INTERACTION_ALPHA)
                    completeReduction(energized = true, notifyFrame = true)
                }
            }

            is StateSubmitRubberElasticity -> {
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
                        notifyFrame = true,
                    )
                }
            }

            StateReheat -> {
                world.reheat(DeponderDefaults.Simulation.INITIAL_ALPHA)
                completeReduction(energized = true, notifyFrame = false)
            }

            is StateSetPinned -> {
                world.setBodyPinned(intent.id, intent.pinned)
                completeReduction(
                    energized = if (!intent.pinned) true else null,
                    notifyFrame = false,
                )
            }

            is StateSetPressed -> {
                world.setBodyPressed(intent.id, intent.pressed)
                completeReduction(energized = true, notifyFrame = false)
            }

            is StateBringToFront -> {
                val planetNode = activePlanetsById[intent.id] ?: return
                topZCounter += 1f
                planetNode.zOrder = topZCounter
            }

            is StateUpdateDensity -> {
                val safeDensity = if (intent.density.isFinite() && intent.density > 0f) intent.density else 1f
                if (currentDensity == safeDensity) return
                currentDensity = safeDensity
                world.updateEnvironment(physics.toEnvironmentSpec(safeDensity), reheatSimulation = false)
                for ((id, planetNode) in activePlanetsById) {
                    val body = world.bodyOf(id) ?: continue
                    world.upsertPlanet(
                        spec = planetNode.physics.toPlanetSpec(id, safeDensity).copy(pinned = body.spec.pinned),
                        initialX = body.x,
                        initialY = body.y,
                        scale = scale,
                    )
                }
                for ((key, rubberNode) in activeRubbersByKey) {
                    val pose = world.rubberOf(key) ?: continue
                    val targetNatPx = rubberNode.physics.resolveNaturalLengthPx(
                        density = safeDensity,
                        contentWidthPx = rubberNode.resolveAdaptiveContentWidthPx().toFloat(),
                    )
                    world.upsertRubber(pose.spec.copy(naturalLengthPx = targetNatPx), scale)
                }
                world.refreshRubberPoses(scale)
                completeReduction(
                    energized = if (world.bodies.isNotEmpty()) true else null,
                    notifyFrame = true,
                )
            }

            is StateUpdateViewportSize -> {
                val prevW = world.viewportWidthPx
                val prevH = world.viewportHeightPx
                world.updateViewport(intent.widthPx.toFloat(), intent.heightPx.toFloat())
                val changed = world.viewportWidthPx != prevW || world.viewportHeightPx != prevH
                completeReduction(
                    energized = if (changed && world.bodies.isNotEmpty()) true else null,
                    notifyFrame = changed,
                )
            }

            is StateUpdatePlanetSize -> {
                val body = world.bodyOf(intent.id) ?: return
                val prevHalfW = body.halfWidthPx
                val prevHalfH = body.halfHeightPx
                world.setBodySize(
                    id = intent.id,
                    widthPx = intent.widthPx.toFloat(),
                    heightPx = intent.heightPx.toFloat(),
                    scale = scale,
                )
                val changed = body.halfWidthPx != prevHalfW || body.halfHeightPx != prevHalfH
                completeReduction(
                    energized = null,
                    notifyFrame = changed,
                )
            }
        }
    }

    /**
     * Updates the root container [RootPhysics] while preserving all active planet coordinates and velocities.
     */
    public fun updatePhysics(newPhysics: RootPhysics) {
        dispatch(StateUpdatePhysics(newPhysics))
    }

    /**
     * Updates the simulation zoom scale within `[physics.minScale, physics.maxScale]` and wakes the loop.
     */
    public fun submitScale(newScale: Float) {
        require(newScale.isFinite()) { "scale must be finite, but was $newScale" }
        dispatch(StateSubmitScale(newScale))
    }

    /**
     * Dynamically updates the spring [elasticity] across all registered rubbers and wakes the loop.
     */
    public fun submitRubberElasticity(elasticity: Float) {
        require(elasticity.isFinite()) { "elasticity must be finite, but was $elasticity" }
        dispatch(StateSubmitRubberElasticity(elasticity))
    }

    /**
     * Reheats simulated annealing and wakes the physics frame loop if sleeping.
     */
    public fun reheat() {
        dispatch(StateReheat)
    }

    /**
     * Dynamically pins or unpins planet [id].
     */
    public fun setPinned(id: String, pinned: Boolean) {
        dispatch(StateSetPinned(id, pinned))
    }

    /**
     * Marks planet [id] as actively pressed/held (`pressed = true` locks velocity and elevates interaction alpha).
     */
    public fun setPressed(id: String, pressed: Boolean) {
        dispatch(StateSetPressed(id, pressed))
    }

    /**
     * Offsets planet [id] by `(dxPx, dyPx)` in static `deponderRoot` pixel coordinates and wakes neighbors.
     *
     * Fast-path zero-allocation Data-Plane reducer preserving 0-frame drag latency.
     */
    public fun dragBy(id: String, dxPx: Float, dyPx: Float) {
        require(dxPx.isFinite() && dyPx.isFinite()) {
            "dragBy delta must be finite, but were ($dxPx, $dyPx)"
        }
        if (dxPx == 0f && dyPx == 0f) return
        world.offsetBodyPosition(id, dxPx, dyPx, scale)
        completeReduction(energized = true, notifyFrame = true)
    }

    /**
     * Moves planet [id] directly to `(xPx, yPx)` in static `deponderRoot` pixel coordinates and wakes neighbors.
     *
     * Fast-path zero-allocation Data-Plane reducer preserving 0-frame drag latency.
     */
    public fun moveTo(id: String, xPx: Float, yPx: Float) {
        require(xPx.isFinite() && yPx.isFinite()) {
            "moveTo coordinates must be finite, but were ($xPx, $yPx)"
        }
        world.setBodyPosition(id, xPx, yPx, scale)
        completeReduction(energized = true, notifyFrame = true)
    }

    /**
     * Elevates planet [id]'s visual draw Z-order above sibling planets without mutating tree order.
     */
    public fun bringToFront(id: String) {
        dispatch(StateBringToFront(id))
    }

    internal fun attachRoot(node: DeponderRootNode, density: Float) {
        val existing = attachedRootNode
        check(existing == null || existing === node) {
            "DeponderState is already attached to another active Modifier.deponderRoot instance."
        }
        attachedRootNode = node
        hostAttachedSource.value = true
        updateDensity(density)
        completeReduction(energized = null, notifyFrame = true)
    }

    internal fun detachRoot(node: DeponderRootNode) {
        if (attachedRootNode === node) {
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
            attachedRootNode = null
            hostAttachedSource.value = false
            syncDerivedAwakeFlag()
        }
    }

    internal fun updateDensity(density: Float) {
        dispatch(StateUpdateDensity(density))
    }

    internal fun updateViewportSize(widthPx: Int, heightPx: Int) {
        dispatch(StateUpdateViewportSize(widthPx, heightPx))
    }

    internal fun registerPlanetNode(node: DeponderPlanetNode) {
        val existing = activePlanetsById[node.planetId]
        check(existing == null || existing === node) {
            "Duplicate deponderPlanet id '${node.planetId}' under the same deponderRoot."
        }
        activePlanetsById[node.planetId] = node
        node.registeredState = this
    }

    internal fun syncPlanetPlacement(
        node: DeponderPlanetNode,
        initialCenterX: Float,
        initialCenterY: Float,
    ): PlanetBody {
        val id = node.planetId
        val spec = node.physics.toPlanetSpec(id, currentDensity)
        world.upsertPlanet(spec, initialCenterX, initialCenterY, scale)
        world.setBodySize(
            id = id,
            widthPx = node.measuredWidthPx.toFloat(),
            heightPx = node.measuredHeightPx.toFloat(),
            scale = scale,
        )
        val body = world.bodyOf(id)!!
        node.boundBody = body
        completeReduction(energized = true, notifyFrame = true)
        return body
    }

    internal fun updatePlanetMeasuredSize(id: String, widthPx: Int, heightPx: Int) {
        dispatch(StateUpdatePlanetSize(id, widthPx, heightPx))
    }

    internal fun updatePlanetRegistration(
        node: DeponderPlanetNode,
        oldId: String,
        newId: String,
        newPhysics: PlanetPhysics,
        fallbackCenterX: Float,
        fallbackCenterY: Float,
    ): PlanetBody? {
        if (oldId != newId) {
            val conflict = activePlanetsById[newId]
            check(conflict == null || conflict === node) {
                "Duplicate deponderPlanet id '$newId' under the same deponderRoot."
            }
            val prevBody = world.bodyOf(oldId)
            val seedX = prevBody?.x ?: fallbackCenterX
            val seedY = prevBody?.y ?: fallbackCenterY
            activePlanetsById.remove(oldId)
            world.removePlanet(oldId, scale)
            activePlanetsById[newId] = node
            if (node.hasBaseOrigin) {
                world.upsertPlanet(newPhysics.toPlanetSpec(newId, currentDensity), seedX, seedY, scale)
                world.setBodySize(
                    id = newId,
                    widthPx = node.measuredWidthPx.toFloat(),
                    heightPx = node.measuredHeightPx.toFloat(),
                    scale = scale,
                )
                val body = world.bodyOf(newId)
                completeReduction(energized = true, notifyFrame = true)
                return body
            }
            completeReduction(
                energized = if (world.bodies.isNotEmpty()) true else null,
                notifyFrame = true,
            )
            return null
        }

        if (node.hasBaseOrigin) {
            val existingBody = world.bodyOf(newId)
            val seedX = existingBody?.x ?: fallbackCenterX
            val seedY = existingBody?.y ?: fallbackCenterY
            world.upsertPlanet(newPhysics.toPlanetSpec(newId, currentDensity), seedX, seedY, scale)
            world.setBodySize(
                id = newId,
                widthPx = node.measuredWidthPx.toFloat(),
                heightPx = node.measuredHeightPx.toFloat(),
                scale = scale,
            )
            val body = world.bodyOf(newId)
            completeReduction(energized = true, notifyFrame = true)
            return body
        }
        return null
    }

    internal fun unregisterPlanetNode(node: DeponderPlanetNode) {
        if (activePlanetsById[node.planetId] === node) {
            activePlanetsById.remove(node.planetId)
            if (node.registeredState === this) {
                node.registeredState = null
            }
            world.removePlanet(node.planetId, scale)
            completeReduction(
                energized = world.bodies.isNotEmpty(),
                notifyFrame = true,
            )
        }
    }

    internal fun registerRubberNode(node: DeponderRubberNode): RubberPose {
        val key = EdgeKey(node.startId, node.endId)
        val existing = activeRubbersByKey[key]
        check(existing == null || existing === node) {
            "Duplicate deponderRubber link between '${node.startId}' and '${node.endId}' (${key.encoded}) under the same deponderRoot."
        }
        activeRubbersByKey[key] = node
        node.registeredState = this
        val spec = node.physics.toRubberSpec(
            startId = node.startId,
            endId = node.endId,
            density = currentDensity,
            contentWidthPx = node.resolveAdaptiveContentWidthPx().toFloat(),
        )
        val pose = world.upsertRubber(spec, scale)
        completeReduction(energized = true, notifyFrame = true)
        return pose
    }

    internal fun updateRubberRegistration(
        node: DeponderRubberNode,
        oldKey: EdgeKey,
        newStartId: String,
        newEndId: String,
        newPhysics: RubberPhysics,
    ): RubberPose {
        val newKey = EdgeKey(newStartId, newEndId)
        if (oldKey != newKey) {
            val conflict = activeRubbersByKey[newKey]
            check(conflict == null || conflict === node) {
                "Duplicate deponderRubber link between '$newStartId' and '$newEndId' (${newKey.encoded}) under the same deponderRoot."
            }
            if (activeRubbersByKey[oldKey] === node) {
                activeRubbersByKey.remove(oldKey)
                world.removeRubber(oldKey, scale)
            }
            activeRubbersByKey[newKey] = node
        }
        val pose = world.upsertRubber(
            newPhysics.toRubberSpec(
                startId = newStartId,
                endId = newEndId,
                density = currentDensity,
                contentWidthPx = node.resolveAdaptiveContentWidthPx().toFloat(),
            ),
            scale,
        )
        completeReduction(energized = true, notifyFrame = true)
        return pose
    }

    internal fun syncRubberAdaptiveLength(node: DeponderRubberNode) {
        if (!node.physics.isAutoLength) return
        val key = EdgeKey(node.startId, node.endId)
        if (activeRubbersByKey[key] !== node) return
        val pose = world.rubberOf(key) ?: return
        val targetNatPx = node.physics.resolveNaturalLengthPx(
            density = currentDensity,
            contentWidthPx = node.resolveAdaptiveContentWidthPx().toFloat(),
        )
        if (abs(pose.spec.naturalLengthPx - targetNatPx) > 0.05f) {
            world.upsertRubber(pose.spec.copy(naturalLengthPx = targetNatPx), scale)
            completeReduction(energized = true, notifyFrame = true)
        }
    }

    internal fun unregisterRubberNode(node: DeponderRubberNode) {
        val key = EdgeKey(node.startId, node.endId)
        if (activeRubbersByKey[key] === node) {
            activeRubbersByKey.remove(key)
            if (node.registeredState === this) {
                node.registeredState = null
            }
            world.removeRubber(key, scale)
            completeReduction(
                energized = if (world.bodies.isNotEmpty()) true else null,
                notifyFrame = true,
            )
        }
    }

    internal suspend fun stepFrame(dtSeconds: Float): Boolean {
        val keepRunning = world.step(dtSeconds, scale)
        completeReduction(
            energized = if (keepRunning) null else false,
            notifyFrame = true,
        )
        return keepRunning
    }

    internal fun notifyFrameChanged() {
        rawFrameTick += 1L
        frameTickState.longValue = rawFrameTick
    }
}

/**
 * Creates and remembers a [DeponderState] across compositions, keeping [physics] synchronized.
 */
@Composable
public fun rememberDeponderState(
    physics: RootPhysics = RootPhysics(),
): DeponderState {
    val state = remember { DeponderState(physics) }
    SideEffect {
        state.updatePhysics(physics)
    }
    return state
}
