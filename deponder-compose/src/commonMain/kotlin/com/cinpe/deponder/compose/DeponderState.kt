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

/**
 * Imperative physics state and controller for a [deponderRoot] container in Compose Multiplatform.
 *
 * Manages node/link topology registration, density synchronization, zoom [scale], dynamic rubber
 * elasticity updates, node pinning, programmatic dragging, and the zero-recomposition VSYNC simulation loop.
 */
@Stable
public class DeponderState(
    physics: RootPhysics = RootPhysics(),
) {
    public var physics: RootPhysics = physics
        private set

    internal val world: SimulationWorld = SimulationWorld(physics.toEnvironmentSpec(density = 1f))

    private var currentDensity: Float = 1f
    private var attachedRootNode: DeponderRootNode? = null

    public var scale: Float by mutableFloatStateOf(
        physics.initScale.coerceIn(physics.minScale, physics.maxScale),
    )
        private set

    public var isAwake: Boolean by mutableStateOf(false)
        internal set

    /**
     * Monotonically increasing tick read exclusively inside `placeWithLayer` / draw scopes so
     * physics steps never trigger `@Composable` recompositions or parent re-measurements.
     */
    internal var frameTick: Long by mutableLongStateOf(0L)
        private set

    private var topZCounter: Float = 1f

    internal val activePlanetsById = LinkedHashMap<String, DeponderPlanetNode>()
    internal val activeRubbersByKey = LinkedHashMap<EdgeKey, DeponderRubberNode>()

    /**
     * Updates the root container [RootPhysics] while preserving all active planet coordinates and velocities.
     */
    public fun updatePhysics(newPhysics: RootPhysics) {
        if (physics == newPhysics) return
        physics = newPhysics
        val clampedScale = scale.coerceIn(newPhysics.minScale, newPhysics.maxScale)
        if (scale != clampedScale) {
            scale = clampedScale
        }
        world.updateEnvironment(newPhysics.toEnvironmentSpec(currentDensity), reheatSimulation = true)
        world.refreshRubberPoses(scale)
        notifyFrameChanged()
        wakeIfPopulated()
    }

    /**
     * Updates the simulation zoom scale within `[physics.minScale, physics.maxScale]` and wakes the loop.
     */
    public fun submitScale(newScale: Float) {
        val clamped = newScale.coerceIn(physics.minScale, physics.maxScale)
        if (scale != clamped) {
            scale = clamped
            world.refreshRubberPoses(clamped)
            world.reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
            notifyFrameChanged()
            wakeIfPopulated()
        }
    }

    /**
     * Dynamically updates the spring [elasticity] across all registered rubbers and wakes the loop.
     */
    public fun submitRubberElasticity(elasticity: Float) {
        world.updateAllRubbersElasticity(elasticity)
        notifyFrameChanged()
        wakeIfPopulated()
    }

    /**
     * Reheats simulated annealing and wakes the physics frame loop if sleeping.
     */
    public fun reheat() {
        world.reheat(DeponderDefaults.Simulation.INITIAL_ALPHA)
        wakeIfPopulated()
    }

    /**
     * Dynamically pins or unpins planet [id].
     */
    public fun setPinned(id: String, pinned: Boolean) {
        world.setBodyPinned(id, pinned)
        if (!pinned) {
            wakeIfPopulated()
        }
    }

    /**
     * Marks planet [id] as actively pressed/held (`pressed = true` locks velocity and elevates interaction alpha).
     */
    public fun setPressed(id: String, pressed: Boolean) {
        world.setBodyPressed(id, pressed)
        wakeIfPopulated()
    }

    /**
     * Offsets planet [id] by `(dxPx, dyPx)` in static `deponderRoot` pixel coordinates and wakes neighbors.
     */
    public fun dragBy(id: String, dxPx: Float, dyPx: Float) {
        if (dxPx == 0f && dyPx == 0f) return
        world.offsetBodyPosition(id, dxPx, dyPx, scale)
        notifyFrameChanged()
        wakeIfPopulated()
    }

    /**
     * Moves planet [id] directly to `(xPx, yPx)` in static `deponderRoot` pixel coordinates and wakes neighbors.
     */
    public fun moveTo(id: String, xPx: Float, yPx: Float) {
        world.setBodyPosition(id, xPx, yPx, scale)
        notifyFrameChanged()
        wakeIfPopulated()
    }

    /**
     * Elevates planet [id]'s visual draw Z-order above sibling planets without mutating tree order.
     */
    public fun bringToFront(id: String) {
        val planetNode = activePlanetsById[id] ?: return
        topZCounter += 1f
        planetNode.zOrder = topZCounter
    }

    internal fun attachRoot(node: DeponderRootNode, density: Float) {
        val existing = attachedRootNode
        check(existing == null || existing === node) {
            "DeponderState is already attached to another active Modifier.deponderRoot instance."
        }
        attachedRootNode = node
        updateDensity(density)
        wakeIfPopulated()
    }

    internal fun detachRoot(node: DeponderRootNode) {
        if (attachedRootNode === node) {
            attachedRootNode = null
            isAwake = false
        }
    }

    internal fun updateDensity(density: Float) {
        val safeDensity = if (density > 0f) density else 1f
        if (currentDensity == safeDensity) return
        currentDensity = safeDensity
        world.updateEnvironment(physics.toEnvironmentSpec(safeDensity), reheatSimulation = false)
        for ((id, planetNode) in activePlanetsById) {
            val body = world.bodyOf(id)
            if (body != null) {
                body.spec = planetNode.physics.toPlanetSpec(id, safeDensity)
            }
        }
        for ((key, rubberNode) in activeRubbersByKey) {
            val pose = world.rubberOf(key)
            if (pose != null) {
                pose.spec = rubberNode.physics.toRubberSpec(rubberNode.startId, rubberNode.endId, safeDensity)
            }
        }
        world.refreshRubberPoses(scale)
        notifyFrameChanged()
        wakeIfPopulated()
    }

    internal fun updateViewportSize(widthPx: Int, heightPx: Int) {
        world.updateViewport(widthPx.toFloat(), heightPx.toFloat())
        notifyFrameChanged()
        wakeIfPopulated()
    }

    internal fun registerPlanetNode(node: DeponderPlanetNode) {
        val existing = activePlanetsById[node.planetId]
        check(existing == null || existing === node) {
            "Duplicate deponderPlanet id '${node.planetId}' under the same deponderRoot."
        }
        activePlanetsById[node.planetId] = node
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
        notifyFrameChanged()
        wakeIfPopulated()
        return body
    }

    internal fun updatePlanetMeasuredSize(id: String, widthPx: Int, heightPx: Int) {
        if (world.bodyOf(id) == null) return
        world.setBodySize(
            id = id,
            widthPx = widthPx.toFloat(),
            heightPx = heightPx.toFloat(),
            scale = scale,
        )
        notifyFrameChanged()
        wakeIfPopulated()
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
                notifyFrameChanged()
                wakeIfPopulated()
                return world.bodyOf(newId)
            }
            notifyFrameChanged()
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
            notifyFrameChanged()
            wakeIfPopulated()
            return world.bodyOf(newId)
        }
        return null
    }

    internal fun unregisterPlanetNode(node: DeponderPlanetNode) {
        if (activePlanetsById[node.planetId] === node) {
            activePlanetsById.remove(node.planetId)
            world.removePlanet(node.planetId, scale)
            notifyFrameChanged()
            wakeIfPopulated()
        }
    }

    internal fun registerRubberNode(node: DeponderRubberNode): RubberPose {
        val key = EdgeKey(node.startId, node.endId)
        val existing = activeRubbersByKey[key]
        check(existing == null || existing === node) {
            "Duplicate deponderRubber link between '${node.startId}' and '${node.endId}' (${key.encoded}) under the same deponderRoot."
        }
        activeRubbersByKey[key] = node
        val spec = node.physics.toRubberSpec(node.startId, node.endId, currentDensity)
        val pose = world.upsertRubber(spec, scale)
        notifyFrameChanged()
        wakeIfPopulated()
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
            newPhysics.toRubberSpec(newStartId, newEndId, currentDensity),
            scale,
        )
        notifyFrameChanged()
        wakeIfPopulated()
        return pose
    }

    internal fun unregisterRubberNode(node: DeponderRubberNode) {
        val key = EdgeKey(node.startId, node.endId)
        if (activeRubbersByKey[key] === node) {
            activeRubbersByKey.remove(key)
            world.removeRubber(key, scale)
            notifyFrameChanged()
            wakeIfPopulated()
        }
    }

    internal suspend fun stepFrame(dtSeconds: Float): Boolean {
        val keepRunning = world.step(dtSeconds, scale)
        notifyFrameChanged()
        if (!keepRunning) {
            isAwake = false
        }
        return keepRunning
    }

    internal fun notifyFrameChanged() {
        frameTick += 1L
    }

    private fun wakeIfPopulated() {
        if (attachedRootNode != null && world.bodies.isNotEmpty()) {
            isAwake = true
        }
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
