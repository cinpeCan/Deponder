package com.cinpe.deponder.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin Multiplatform force-directed physics world.
 *
 * Implements:
 * - Strict pure point-mass force dynamics (center coordinates + mass + independent radiation field,
 *   zero rigid-body collision coupling) paired with visual ray-to-rounded-contour clearance on
 *   [RubberPose] so both built-in and custom developer rubbers terminate cleanly outside planet UI bounds.
 * - Deterministic golden-angle spiral symmetry breaking when stacked nodes start at identical `(x, y)` coordinates.
 * - Adaptive 3-tier execution:
 *   1. Small graphs (`N < 64`): direct symmetric array loop on current thread.
 *   2. Medium graphs (`64 <= N < 256`): single-thread zero-allocation [HybridQuadtree] without coroutine dispatch overhead.
 *   3. Large graphs (`N >= 256`): single outer [Dispatchers.Default] switch per VSYNC wrapping all substeps
 *      operating exclusively on isolated primitive snapshots (zero cross-thread collection/body access).
 * - Adaptive `1/120s` exact-division sub-stepping (`MAX_SUBSTEPS = 4`), exponential viscous air damping,
 *   full-strength `alpha = 1.0f` interaction response, and frame-rate-independent Global Kinetic-Force Sleep.
 */
public class SimulationWorld(
    environment: EnvironmentSpec = EnvironmentSpec(),
) {
    public var environment: EnvironmentSpec = environment
        private set

    public var viewportWidthPx: Float = 0f
        private set
    public var viewportHeightPx: Float = 0f
        private set

    public var alpha: Float = DeponderDefaults.Simulation.INITIAL_ALPHA
        private set

    private var settleTimeSeconds: Float = 0f
    private var perturbationCounter: Int = 0
    private var topologyVersion: Long = 0L

    private val bodiesById = LinkedHashMap<String, PlanetBody>()
    private val bodyList = ArrayList<PlanetBody>()
    private val indexById = HashMap<String, Int>()
    private val bodyMutationStampById = HashMap<String, Long>()
    private var mutationClock: Long = 0L

    private val declaredRubbersByKey = LinkedHashMap<EdgeKey, RubberPose>()
    private val rubberList = ArrayList<RubberPose>()

    // Reusable single-thread primitive buffers (Tier 1 & Tier 2: zero per-frame heap allocation).
    private var bufCap = 64
    private var snapPx = FloatArray(bufCap)
    private var snapPy = FloatArray(bufCap)
    private var snapEffR = FloatArray(bufCap)
    private var snapEc = FloatArray(bufCap)
    private var outPairFx = FloatArray(bufCap)
    private var outPairFy = FloatArray(bufCap)
    private var accumEdgeFx = FloatArray(bufCap)
    private var accumEdgeFy = FloatArray(bufCap)
    private var frameStartX = FloatArray(bufCap)
    private var frameStartY = FloatArray(bufCap)

    private val quadtree = HybridQuadtree()
    private val workerStacks = Array(DeponderDefaults.Simulation.PARALLEL_WORKER_CHUNKS) {
        IntArray(HybridQuadtree.REQUIRED_STACK_SIZE)
    }

    public val bodies: List<PlanetBody>
        get() = bodyList

    public val rubbers: List<RubberPose>
        get() = rubberList

    public fun bodyOf(id: String): PlanetBody? = bodiesById[id]

    public fun rubberOf(key: EdgeKey): RubberPose? = declaredRubbersByKey[key]

    private fun markBodyMutated(id: String) {
        mutationClock += 1L
        bodyMutationStampById[id] = mutationClock
    }

    public fun updateEnvironment(spec: EnvironmentSpec, reheatSimulation: Boolean = true) {
        if (environment != spec) {
            environment = spec
            if (reheatSimulation) reheat()
        }
    }

    public fun updateViewport(widthPx: Float, heightPx: Float) {
        val w = max(0f, widthPx)
        val h = max(0f, heightPx)
        if (viewportWidthPx != w || viewportHeightPx != h) {
            viewportWidthPx = w
            viewportHeightPx = h
            reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        }
    }

    /**
     * Reheats simulated annealing temperature and resets the sleep timer so the simulation wakes immediately.
     */
    public fun reheat(minAlpha: Float = DeponderDefaults.Simulation.INITIAL_ALPHA) {
        val clamped = minAlpha.coerceIn(0f, 1f)
        if (alpha < clamped) {
            alpha = clamped
        }
        settleTimeSeconds = 0f
    }

    private fun resolveInitialPosition(rawX: Float, rawY: Float, pinned: Boolean): Vec2 {
        val nonCoincident = resolveNonCoincidentPosition(rawX, rawY)
        if (pinned || bodyList.size < 2 || (nonCoincident.x != rawX || nonCoincident.y != rawY)) {
            return nonCoincident
        }
        return resolveCollinearDegeneracy(nonCoincident.x, nonCoincident.y)
    }

    private fun resolveNonCoincidentPosition(rawX: Float, rawY: Float): Vec2 {
        var x = rawX
        var y = rawY
        val epsSq = DeponderDefaults.Simulation.COINCIDENT_EPSILON_SQ
        var guard = 0
        while (guard < 64 && hasCoincidentExistingBody(x, y, epsSq)) {
            perturbationCounter += 1
            val k = perturbationCounter
            val angle = k.toDouble() * GOLDEN_ANGLE_RAD
            val radius = DeponderDefaults.Simulation.COINCIDENT_PERTURBATION_RADIUS_PX * sqrt(k.toFloat())
            x = rawX + (cos(angle) * radius).toFloat()
            y = rawY + (sin(angle) * radius).toFloat()
            guard += 1
        }
        return Vec2(x, y)
    }

    /**
     * Breaks 1D collinear saddle traps when 3 or more unpinned planets are initially laid out along
     * a single straight line (such as a Compose `Row`/`Column` or horizontal `ConstraintLayout` chain).
     */
    private fun resolveCollinearDegeneracy(x: Float, y: Float): Vec2 {
        val b0 = bodyList[0]
        val x0 = b0.x
        val y0 = b0.y
        var refDx = x - x0
        var refDy = y - y0
        var maxDistSq = refDx * refDx + refDy * refDy
        for (i in 1 until bodyList.size) {
            val b = bodyList[i]
            val dx = b.x - x0
            val dy = b.y - y0
            val dSq = dx * dx + dy * dy
            if (dSq > maxDistSq) {
                maxDistSq = dSq
                refDx = dx
                refDy = dy
            }
        }
        if (maxDistSq <= DeponderDefaults.Simulation.COINCIDENT_EPSILON_SQ) {
            return Vec2(x, y)
        }
        val invLen = 1f / sqrt(maxDistSq)
        val nx = -refDy * invLen
        val ny = refDx * invLen
        if (abs((x - x0) * nx + (y - y0) * ny) > COLLINEAR_BAND_PX) {
            return Vec2(x, y)
        }
        for (i in 1 until bodyList.size) {
            val b = bodyList[i]
            if (abs((b.x - x0) * nx + (b.y - y0) * ny) > COLLINEAR_BAND_PX) {
                return Vec2(x, y)
            }
        }
        perturbationCounter += 1
        val sign = if ((perturbationCounter and 1) == 0) 1f else -1f
        val offset = COLLINEAR_NUDGE_PX * sign
        return Vec2(x + nx * offset, y + ny * offset)
    }

    private fun hasCoincidentExistingBody(x: Float, y: Float, epsSq: Float): Boolean {
        for (i in bodyList.indices) {
            val b = bodyList[i]
            val dx = b.x - x
            val dy = b.y - y
            if (dx * dx + dy * dy < epsSq) return true
        }
        return false
    }

    /**
     * Registers or updates a single point-mass planet incrementally, preserving velocity and position
     * if `spec.id` is already active, or applying deterministic golden-angle symmetry breaking if stacked.
     */
    public fun upsertPlanet(
        spec: PlanetSpec,
        initialX: Float,
        initialY: Float,
        scale: Float = environment.initScale,
    ) {
        val existing = bodiesById[spec.id]
        if (existing != null) {
            existing.spec = spec
            if (spec.pinned) {
                existing.vx = 0f
                existing.vy = 0f
                markBodyMutated(spec.id)
            }
        } else {
            val resolved = resolveInitialPosition(initialX, initialY, spec.pinned)
            val created = PlanetBody(spec, resolved.x, resolved.y)
            bodiesById[spec.id] = created
            bodyList.add(created)
            markBodyMutated(spec.id)
        }
        rebuildTopologyIndex(scale)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
    }

    /**
     * Unregisters a planet by [id] and automatically gates off any incident rubbers.
     */
    public fun removePlanet(id: String, scale: Float = environment.initScale): Boolean {
        if (bodiesById.remove(id) == null) return false
        bodyMutationStampById.remove(id)
        rebuildTopologyIndex(scale)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        return true
    }

    /**
     * Registers or updates an undirected spring rubber link. If either endpoint is not yet attached
     * or `startId == endId`, the pose remains safely gated (`isActive = false`) until both planets mount.
     */
    public fun upsertRubber(
        spec: RubberSpec,
        scale: Float = environment.initScale,
    ): RubberPose {
        val key = spec.key
        val existing = declaredRubbersByKey[key]
        if (existing != null &&
            existing.spec.startId == spec.startId &&
            existing.spec.endId == spec.endId
        ) {
            existing.spec = spec
            existing.dirty = true
            refreshRubberPoses(scale)
            reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
            return existing
        }
        val pose = declaredRubbersByKey.getOrPut(key) { RubberPose(spec) }
        pose.spec = spec
        pose.dirty = true
        rebuildTopologyIndex(scale)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        return pose
    }

    /**
     * Unregisters an undirected spring rubber link by [key].
     */
    public fun removeRubber(key: EdgeKey, scale: Float = environment.initScale): Boolean {
        if (declaredRubbersByKey.remove(key) == null) return false
        rebuildTopologyIndex(scale)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        return true
    }

    /**
     * Atomically reconciles full planet and rubber lists while preserving existing coordinates and velocities.
     */
    public fun syncTopology(
        planets: List<PlanetSpec>,
        edges: List<RubberSpec>,
        initialPositionProvider: (PlanetSpec) -> Vec2,
    ) {
        val nextIds = HashSet<String>(planets.size * 2)
        for (spec in planets) {
            nextIds.add(spec.id)
        }

        val iter = bodiesById.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.key !in nextIds) {
                bodyMutationStampById.remove(entry.key)
                iter.remove()
            }
        }

        bodyList.clear()
        bodyList.addAll(bodiesById.values)

        for (spec in planets) {
            val existing = bodiesById[spec.id]
            if (existing != null) {
                existing.spec = spec
                if (spec.pinned) {
                    existing.vx = 0f
                    existing.vy = 0f
                    markBodyMutated(spec.id)
                }
            } else {
                val rawPos = initialPositionProvider(spec)
                val resolved = resolveInitialPosition(rawPos.x, rawPos.y, spec.pinned)
                val created = PlanetBody(spec, resolved.x, resolved.y)
                bodiesById[spec.id] = created
                bodyList.add(created)
                markBodyMutated(spec.id)
            }
        }

        val nextKeys = HashSet<EdgeKey>(edges.size * 2)
        for (edge in edges) {
            val key = edge.key
            if (!nextKeys.add(key)) continue
            val existingPose = declaredRubbersByKey[key]
            if (existingPose != null) {
                existingPose.spec = edge
                existingPose.dirty = true
            } else {
                declaredRubbersByKey[key] = RubberPose(edge)
            }
        }

        val rIter = declaredRubbersByKey.entries.iterator()
        while (rIter.hasNext()) {
            if (rIter.next().key !in nextKeys) {
                rIter.remove()
            }
        }

        rebuildTopologyIndex(environment.initScale)
        reheat(DeponderDefaults.Simulation.INITIAL_ALPHA)
    }

    private fun rebuildTopologyIndex(scale: Float) {
        topologyVersion += 1L
        val clampedScale = scale.coerceIn(environment.minScale, environment.maxScale)
        bodyList.clear()
        bodyList.addAll(bodiesById.values)
        indexById.clear()
        for (i in bodyList.indices) {
            val b = bodyList[i]
            b.degree = 0
            indexById[b.spec.id] = i
        }

        rubberList.clear()
        rubberList.addAll(declaredRubbersByKey.values)
        for (i in rubberList.indices) {
            val pose = rubberList[i]
            val spec = pose.spec
            if (spec.startId == spec.endId) {
                if (pose.isActive || pose.length != 0f) {
                    pose.isActive = false
                    pose.length = 0f
                    pose.centerDistance = 0f
                    pose.dirty = true
                }
                continue
            }
            val sIdx = indexById[spec.startId]
            val eIdx = indexById[spec.endId]
            if (sIdx == null || eIdx == null) {
                if (pose.isActive || pose.length != 0f) {
                    pose.isActive = false
                    pose.length = 0f
                    pose.centerDistance = 0f
                    pose.dirty = true
                }
                continue
            }
            bodyList[sIdx].degree += 1
            bodyList[eIdx].degree += 1
            val sBody = bodyList[sIdx]
            val eBody = bodyList[eIdx]
            updateSingleRubberPose(pose, sBody, eBody, clampedScale)
        }
        ensureBuffers(bodyList.size)
    }

    /**
     * Exact ray-to-rounded-rectangle boundary distance along unit vector `(ux, uy)` from center `(0, 0)`.
     * Returns `0f` when the body has no UI dimensions (`halfWidthPx <= 0f && halfHeightPx <= 0f`).
     */
    internal fun computeBodyContourClearance(
        halfWidthPx: Float,
        halfHeightPx: Float,
        gapPx: Float,
        clampedScale: Float,
        ux: Float,
        uy: Float,
    ): Float {
        if (halfWidthPx <= 0f && halfHeightPx <= 0f) return 0f
        val safeGap = max(0f, gapPx)
        val w = max(1e-3f, (max(0f, halfWidthPx) + safeGap) * clampedScale)
        val h = max(1e-3f, (max(0f, halfHeightPx) + safeGap) * clampedScale)
        val cornerR = min(w, h) * ROUNDED_CONTOUR_RATIO
        val coreX = w - cornerR
        val coreY = h - cornerR

        val ax = abs(ux)
        val ay = abs(uy)
        val tx = if (ax > 1e-5f) w / ax else Float.MAX_VALUE
        val ty = if (ay > 1e-5f) h / ay else Float.MAX_VALUE

        if (tx <= ty) {
            if (tx * ay <= coreY) return tx
        } else {
            if (ty * ax <= coreX) return ty
        }

        val dot = coreX * ax + coreY * ay
        val cross = coreX * ay - coreY * ax
        val disc = max(0f, cornerR * cornerR - cross * cross)
        return dot + sqrt(disc)
    }

    private fun updateSingleRubberPose(
        pose: RubberPose,
        sBody: PlanetBody,
        eBody: PlanetBody,
        clampedScale: Float,
    ) {
        val sx = sBody.x
        val sy = sBody.y
        val ex = eBody.x
        val ey = eBody.y
        val diffX = ex - sx
        val diffY = ey - sy
        val centerDist = sqrt(diffX * diffX + diffY * diffY)

        val rotDeg: Float
        val visLen: Float
        val visCx: Float
        val visCy: Float

        if (centerDist > 1e-4f) {
            val angle = atan2(diffX.toDouble(), diffY.toDouble())
            rotDeg = (90.0 - angle * RAD_TO_DEG).toFloat()
            val invDist = 1f / centerDist
            val ux = diffX * invDist
            val uy = diffY * invDist
            val gapPx = pose.spec.endpointGapPx
            val sClearance = computeBodyContourClearance(
                halfWidthPx = sBody.halfWidthPx,
                halfHeightPx = sBody.halfHeightPx,
                gapPx = gapPx,
                clampedScale = clampedScale,
                ux = ux,
                uy = uy,
            )
            val eClearance = computeBodyContourClearance(
                halfWidthPx = eBody.halfWidthPx,
                halfHeightPx = eBody.halfHeightPx,
                gapPx = gapPx,
                clampedScale = clampedScale,
                ux = ux,
                uy = uy,
            )
            if (sClearance + eClearance < centerDist) {
                visLen = centerDist - sClearance - eClearance
                val sxVis = sx + ux * sClearance
                val syVis = sy + uy * sClearance
                val exVis = ex - ux * eClearance
                val eyVis = ey - uy * eClearance
                visCx = (sxVis + exVis) * 0.5f
                visCy = (syVis + eyVis) * 0.5f
            } else {
                visLen = 0f
                visCx = (sx + ex) * 0.5f
                visCy = (sy + ey) * 0.5f
            }
        } else {
            rotDeg = pose.rotationDeg
            visLen = 0f
            visCx = sx
            visCy = sy
        }

        if (!pose.isActive ||
            pose.centerX != visCx ||
            pose.centerY != visCy ||
            pose.length != visLen ||
            pose.centerDistance != centerDist ||
            pose.rotationDeg != rotDeg ||
            pose.scale != clampedScale
        ) {
            pose.isActive = true
            pose.centerX = visCx
            pose.centerY = visCy
            pose.length = visLen
            pose.centerDistance = centerDist
            pose.rotationDeg = rotDeg
            pose.scale = clampedScale
            pose.dirty = true
        } else {
            pose.dirty = false
        }
    }

    /**
     * Updates the unscaled visual layout dimensions `(widthPx, heightPx)` of planet [id] so incident
     * visual [RubberPose] spans automatically clear the planet's UI bounds without altering point-mass physics.
     */
    public fun setBodySize(
        id: String,
        widthPx: Float,
        heightPx: Float,
        scale: Float = environment.initScale,
    ) {
        val body = bodiesById[id] ?: return
        val halfW = max(0f, widthPx) * 0.5f
        val halfH = max(0f, heightPx) * 0.5f
        if (body.halfWidthPx != halfW || body.halfHeightPx != halfH) {
            body.halfWidthPx = halfW
            body.halfHeightPx = halfH
            refreshRubberPoses(scale)
        }
    }

    /**
     * Updates the spring [elasticity] across all declared rubbers and wakes the simulation at full strength.
     */
    public fun updateAllRubbersElasticity(elasticity: Float) {
        var changed = false
        for (i in rubberList.indices) {
            val pose = rubberList[i]
            if (pose.spec.elasticity != elasticity) {
                pose.spec = pose.spec.copy(elasticity = elasticity)
                changed = true
            }
        }
        if (changed) {
            reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        }
    }

    /**
     * Moves planet [id] directly to `(x, y)` in root pixel coordinates, zeroing its velocity and waking
     * connected neighbors at full interaction strength.
     */
    public fun setBodyPosition(id: String, x: Float, y: Float, scale: Float = environment.initScale) {
        val body = bodiesById[id] ?: return
        if (body.x != x || body.y != y) {
            body.x = x
            body.y = y
            body.vx = 0f
            body.vy = 0f
            markBodyMutated(id)
            refreshRubberPoses(scale)
            reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
        }
    }

    /**
     * Offsets planet [id] by `(dx, dy)` in root pixel coordinates and wakes the simulation at full strength.
     */
    public fun offsetBodyPosition(id: String, dx: Float, dy: Float, scale: Float = environment.initScale) {
        if (dx == 0f && dy == 0f) return
        val body = bodiesById[id] ?: return
        body.x += dx
        body.y += dy
        body.vx = 0f
        body.vy = 0f
        markBodyMutated(id)
        refreshRubberPoses(scale)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
    }

    public fun setBodyPressed(id: String, pressed: Boolean) {
        val body = bodiesById[id] ?: return
        body.isPressed = pressed
        body.vx = 0f
        body.vy = 0f
        markBodyMutated(id)
        reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
    }

    public fun setBodyPinned(id: String, pinned: Boolean) {
        val body = bodiesById[id] ?: return
        if (body.spec.pinned != pinned) {
            body.spec = body.spec.copy(pinned = pinned)
            markBodyMutated(id)
            if (pinned) {
                body.vx = 0f
                body.vy = 0f
            } else {
                reheat(DeponderDefaults.Simulation.INTERACTION_ALPHA)
            }
        }
    }

    /**
     * Synchronizes all active [RubberPose] transforms immediately for the given [scale].
     */
    public fun refreshRubberPoses(scale: Float) {
        val clampedScale = scale.coerceIn(environment.minScale, environment.maxScale)
        for (i in rubberList.indices) {
            val pose = rubberList[i]
            val spec = pose.spec
            if (spec.startId == spec.endId) {
                pose.isActive = false
                pose.length = 0f
                pose.centerDistance = 0f
                continue
            }
            val sIdx = indexById[spec.startId]
            val eIdx = indexById[spec.endId]
            if (sIdx == null || eIdx == null) {
                pose.isActive = false
                pose.length = 0f
                pose.centerDistance = 0f
                continue
            }
            val sBody = bodyList[sIdx]
            val eBody = bodyList[eIdx]
            updateSingleRubberPose(pose, sBody, eBody, clampedScale)
        }
    }

    private fun ensureBuffers(n: Int) {
        if (n <= bufCap) return
        bufCap = max(n, bufCap * 2)
        snapPx = FloatArray(bufCap)
        snapPy = FloatArray(bufCap)
        snapEffR = FloatArray(bufCap)
        snapEc = FloatArray(bufCap)
        outPairFx = FloatArray(bufCap)
        outPairFy = FloatArray(bufCap)
        accumEdgeFx = FloatArray(bufCap)
        accumEdgeFy = FloatArray(bufCap)
        frameStartX = FloatArray(bufCap)
        frameStartY = FloatArray(bufCap)
    }

    /**
     * Advances the physics world by [dtSeconds] at zoom [scale].
     *
     * Uses adaptive `1/120s` exact-division sub-stepping so 60Hz, 90Hz, 120Hz, and 240Hz displays
     * produce identical real-time velocities and spring trajectories, with at most one thread switch
     * per VSYNC when `N >= 256` operating purely on isolated primitive snapshots.
     *
     * @return `true` if the simulation remains active and should schedule another VSYNC frame;
     *         `false` once all free nodes satisfy global kinetic + acceleration calm over
     *         [EnvironmentSpec.settleDurationSeconds] so the caller can suspend at 0% CPU.
     */
    public suspend fun step(dtSeconds: Float, scale: Float): Boolean {
        val n = bodyList.size
        if (n == 0) return false
        ensureBuffers(n)

        val clampedDt = dtSeconds.coerceIn(1e-4f, DeponderDefaults.Simulation.MAX_DT_SECONDS)
        val clampedScale = scale.coerceIn(environment.minScale, environment.maxScale)
        val env = environment

        val subSteps = ceil(clampedDt / DeponderDefaults.Simulation.MAX_SUBSTEP_DT_SECONDS)
            .toInt()
            .coerceIn(1, DeponderDefaults.Simulation.MAX_SUBSTEPS)
        val subDt = clampedDt / subSteps.toFloat()

        var anyPressed = false
        for (i in 0 until n) {
            val b = bodyList[i]
            frameStartX[i] = b.x
            frameStartY[i] = b.y
            if (b.isPressed) anyPressed = true
        }

        if (anyPressed && alpha < DeponderDefaults.Simulation.INTERACTION_ALPHA) {
            alpha = DeponderDefaults.Simulation.INTERACTION_ALPHA
        }

        val maxSpeedSq: Float
        val maxAccelSq: Float

        if (n >= DeponderDefaults.Simulation.PARALLEL_DISPATCH_THRESHOLD) {
            // Tier 3 (N >= 256): Capture an immutable-structure primitive snapshot on the caller thread
            // BEFORE suspending to Dispatchers.Default so UI-thread mutations while step() is suspended
            // never race on bodyList, rubberList, indexById, or shared buffers.
            val capturedTopoVersion = topologyVersion
            val capturedClockBefore = mutationClock
            val ids = Array(n) { i -> bodyList[i].spec.id }
            val stamps = LongArray(n) { i -> bodyMutationStampById[ids[i]] ?: 0L }
            val startX = FloatArray(n) { i -> bodyList[i].x }
            val startY = FloatArray(n) { i -> bodyList[i].y }
            val px = startX.copyOf()
            val py = startY.copyOf()
            val vx = FloatArray(n) { i -> bodyList[i].vx }
            val vy = FloatArray(n) { i -> bodyList[i].vy }
            val invMass = FloatArray(n) { i -> 1f / max(1e-4f, bodyList[i].spec.mass) }
            val locked = BooleanArray(n) { i -> bodyList[i].isPressed || bodyList[i].spec.pinned }
            val effR = FloatArray(n) { i ->
                val b = bodyList[i]
                val rawR = b.spec.repulsionRadiusPx
                if (rawR < 0f) {
                    DeponderDefaults.INFINITE_RANGE
                } else if (rawR == 0f) {
                    0f
                } else {
                    (rawR + b.degree * env.degreeRadiusIncrementPx) * clampedScale
                }
            }
            val ec = FloatArray(n) { i -> bodyList[i].spec.elasticity }

            val rTotal = rubberList.size
            val sIdxArr = IntArray(rTotal)
            val eIdxArr = IntArray(rTotal)
            val natLenArr = FloatArray(rTotal)
            val rElastArr = FloatArray(rTotal)
            var activeSprings = 0
            for (rIdx in 0 until rTotal) {
                val spec = rubberList[rIdx].spec
                if (spec.startId == spec.endId || spec.elasticity == 0f) continue
                val sIdx = indexById[spec.startId] ?: continue
                val eIdx = indexById[spec.endId] ?: continue
                sIdxArr[activeSprings] = sIdx
                eIdxArr[activeSprings] = eIdx
                natLenArr[activeSprings] = spec.naturalLengthPx * clampedScale
                rElastArr[activeSprings] = spec.elasticity
                activeSprings += 1
            }

            val vpW = viewportWidthPx
            val vpH = viewportHeightPx
            val initialAlpha = alpha

            val tier3Metrics = withContext(Dispatchers.Default) {
                runTier3ParallelSubsteps(
                    n = n,
                    subSteps = subSteps,
                    subDt = subDt,
                    clampedScale = clampedScale,
                    vpW = vpW,
                    vpH = vpH,
                    env = env,
                    anyPressed = anyPressed,
                    initialAlpha = initialAlpha,
                    px = px,
                    py = py,
                    vx = vx,
                    vy = vy,
                    invMass = invMass,
                    locked = locked,
                    effR = effR,
                    ec = ec,
                    activeSprings = activeSprings,
                    sIdxArr = sIdxArr,
                    eIdxArr = eIdxArr,
                    natLenArr = natLenArr,
                    rElastArr = rElastArr,
                )
            }

            // Commit snapshot results back on the caller thread, skipping any body that was removed,
            // pressed, pinned, or explicitly moved on the UI thread while Dispatchers.Default was running.
            if (topologyVersion == capturedTopoVersion && mutationClock == capturedClockBefore) {
                alpha = tier3Metrics.finalAlpha
                for (i in 0 until n) {
                    val b = bodyList[i]
                    if (b.isPressed || b.spec.pinned) {
                        b.vx = 0f
                        b.vy = 0f
                        b.dx = 0f
                        b.dy = 0f
                    } else {
                        b.x = px[i]
                        b.y = py[i]
                        b.vx = vx[i]
                        b.vy = vy[i]
                        b.dx = px[i] - startX[i]
                        b.dy = py[i] - startY[i]
                    }
                }
            } else {
                if (tier3Metrics.finalAlpha > alpha) {
                    alpha = tier3Metrics.finalAlpha
                }
                for (i in 0 until n) {
                    val id = ids[i]
                    val currentBody = bodiesById[id] ?: continue
                    val currentStamp = bodyMutationStampById[id] ?: 0L
                    if (currentStamp != stamps[i] || currentBody.isPressed || currentBody.spec.pinned) {
                        continue
                    }
                    currentBody.x = px[i]
                    currentBody.y = py[i]
                    currentBody.vx = vx[i]
                    currentBody.vy = vy[i]
                    currentBody.dx = px[i] - startX[i]
                    currentBody.dy = py[i] - startY[i]
                }
                refreshRubberPoses(scale.coerceIn(environment.minScale, environment.maxScale))
                return true
            }

            maxSpeedSq = tier3Metrics.maxSpeedSq
            maxAccelSq = tier3Metrics.maxAccelSq
        } else {
            // Tier 1 (N < 64) & Tier 2 (64 <= N < 256): Zero coroutine context-switch overhead.
            var lastSpeedSq = 0f
            var lastAccelSq = 0f
            for (sub in 0 until subSteps) {
                val metrics = runSingleSubstepNonSuspend(
                    n = n,
                    subDt = subDt,
                    clampedScale = clampedScale,
                    env = env,
                    anyPressed = anyPressed,
                )
                if (sub == subSteps - 1) {
                    lastSpeedSq = metrics.x
                    lastAccelSq = metrics.y
                }
            }
            for (i in 0 until n) {
                val b = bodyList[i]
                b.dx = b.x - frameStartX[i]
                b.dy = b.y - frameStartY[i]
            }
            maxSpeedSq = lastSpeedSq
            maxAccelSq = lastAccelSq
        }

        // Update visual rubber poses once at the end of the VSYNC step.
        refreshRubberPoses(clampedScale)

        if (anyPressed || bodyList.any { it.isPressed }) {
            settleTimeSeconds = 0f
            return true
        }

        val restSpeed = env.restSpeedPxPerSec * clampedScale
        val restAccel = env.restAccelPxPerSecSq * clampedScale
        val isKineticCalm = maxSpeedSq <= restSpeed * restSpeed
        val isForceCalm = maxAccelSq <= restAccel * restAccel

        if (env.coolingEnabled && alpha < env.alphaMin && isKineticCalm && isForceCalm) {
            alpha = 0f
            zeroAllVelocities(bodyList.size)
            settleTimeSeconds = env.settleDurationSeconds
            return false
        }

        if (isKineticCalm && isForceCalm) {
            settleTimeSeconds += clampedDt
            if (settleTimeSeconds >= env.settleDurationSeconds) {
                zeroAllVelocities(bodyList.size)
                return false
            }
        } else {
            settleTimeSeconds = 0f
        }

        return true
    }

    private class Tier3StepMetrics(
        val finalAlpha: Float,
        val maxSpeedSq: Float,
        val maxAccelSq: Float,
    )

    private suspend fun runTier3ParallelSubsteps(
        n: Int,
        subSteps: Int,
        subDt: Float,
        clampedScale: Float,
        vpW: Float,
        vpH: Float,
        env: EnvironmentSpec,
        anyPressed: Boolean,
        initialAlpha: Float,
        px: FloatArray,
        py: FloatArray,
        vx: FloatArray,
        vy: FloatArray,
        invMass: FloatArray,
        locked: BooleanArray,
        effR: FloatArray,
        ec: FloatArray,
        activeSprings: Int,
        sIdxArr: IntArray,
        eIdxArr: IntArray,
        natLenArr: FloatArray,
        rElastArr: FloatArray,
    ): Tier3StepMetrics {
        val localQuadtree = HybridQuadtree()
        val chunks = min(DeponderDefaults.Simulation.PARALLEL_WORKER_CHUNKS, n)
        val localStacks = Array(chunks) { IntArray(HybridQuadtree.REQUIRED_STACK_SIZE) }
        val pairFx = FloatArray(n)
        val pairFy = FloatArray(n)
        val edgeFx = FloatArray(n)
        val edgeFy = FloatArray(n)
        val stepX = FloatArray(n)
        val stepY = FloatArray(n)

        val infStrength = env.infiniteChargeStrength * clampedScale
        val softeningSq = DeponderDefaults.Simulation.COULOMB_SOFTENING_SQ
        val thetaSq = DeponderDefaults.Simulation.BARNES_HUT_THETA * DeponderDefaults.Simulation.BARNES_HUT_THETA
        val dampFactor = exp(-max(0f, env.airDamping) * subDt)
        val chunkSize = (n + chunks - 1) / chunks

        var curAlpha = initialAlpha
        var lastMaxSpeedSq = 0f
        var lastMaxAccelSq = 0f

        for (sub in 0 until subSteps) {
            for (i in 0 until n) {
                stepX[i] = px[i]
                stepY[i] = py[i]
                pairFx[i] = 0f
                pairFy[i] = 0f
                edgeFx[i] = 0f
                edgeFy[i] = 0f
            }

            for (r in 0 until activeSprings) {
                val sIdx = sIdxArr[r]
                val eIdx = eIdxArr[r]
                val diffX = stepX[eIdx] - stepX[sIdx]
                val diffY = stepY[eIdx] - stepY[sIdx]
                val distSq = diffX * diffX + diffY * diffY
                if (distSq > 0f) {
                    val length = sqrt(distSq)
                    val invLen = 1f / length
                    val springDeform = natLenArr[r] - length
                    val mag = springDeform * rElastArr[r]
                    val fxOnS = -diffX * invLen * mag
                    val fyOnS = -diffY * invLen * mag
                    edgeFx[sIdx] += fxOnS
                    edgeFy[sIdx] += fyOnS
                    edgeFx[eIdx] -= fxOnS
                    edgeFy[eIdx] -= fyOnS
                }
            }

            localQuadtree.build(
                count = n,
                px = stepX,
                py = stepY,
                effectiveRadius = effR,
                elasticity = ec,
                infiniteChargeStrength = infStrength,
            )

            coroutineScope {
                for (c in 0 until chunks) {
                    val start = c * chunkSize
                    val end = min(n, start + chunkSize)
                    if (start >= end) break
                    val stack = localStacks[c]
                    launch {
                        for (s in start until end) {
                            val f = localQuadtree.queryForceOnNode(
                                s = s,
                                px = stepX,
                                py = stepY,
                                effectiveRadius = effR,
                                elasticity = ec,
                                infiniteChargeStrength = infStrength,
                                thetaSq = thetaSq,
                                softeningSq = softeningSq,
                                stack = stack,
                            )
                            pairFx[s] = f.x
                            pairFy[s] = f.y
                        }
                    }
                }
            }

            val effectiveAlpha = if (env.coolingEnabled) curAlpha else 1f
            var maxSpeedSq = 0f
            var maxAccelSq = 0f

            for (i in 0 until n) {
                if (locked[i]) {
                    vx[i] = 0f
                    vy[i] = 0f
                    continue
                }
                val wallF = computeWallForce(
                    px = stepX[i],
                    py = stepY[i],
                    scale = clampedScale,
                    width = vpW,
                    height = vpH,
                    env = env,
                )
                val totalFx = wallF.x + edgeFx[i] + pairFx[i]
                val totalFy = wallF.y + edgeFy[i] + pairFy[i]
                val ax = totalFx * invMass[i] * effectiveAlpha
                val ay = totalFy * invMass[i] * effectiveAlpha

                val nextVx = (vx[i] + ax * subDt) * dampFactor
                val nextVy = (vy[i] + ay * subDt) * dampFactor
                vx[i] = nextVx
                vy[i] = nextVy
                px[i] += nextVx * subDt
                py[i] += nextVy * subDt

                val speedSq = nextVx * nextVx + nextVy * nextVy
                val accelSq = ax * ax + ay * ay
                if (speedSq > maxSpeedSq) maxSpeedSq = speedSq
                if (accelSq > maxAccelSq) maxAccelSq = accelSq
            }

            if (env.coolingEnabled) {
                val target = if (anyPressed) DeponderDefaults.Simulation.INTERACTION_ALPHA else 0f
                val dtRatio = subDt / DeponderDefaults.Simulation.REFERENCE_DT_SECONDS
                val decay = 1f - (1f - env.alphaDecay.coerceIn(0f, 1f)).pow(dtRatio)
                curAlpha += (target - curAlpha) * decay
            }

            if (sub == subSteps - 1) {
                lastMaxSpeedSq = maxSpeedSq
                lastMaxAccelSq = maxAccelSq
            }
        }

        return Tier3StepMetrics(
            finalAlpha = curAlpha,
            maxSpeedSq = lastMaxSpeedSq,
            maxAccelSq = lastMaxAccelSq,
        )
    }

    private fun zeroAllVelocities(n: Int) {
        for (i in 0 until n) {
            val b = bodyList[i]
            b.vx = 0f
            b.vy = 0f
            b.dx = 0f
            b.dy = 0f
        }
    }

    private fun prepareSnapshotAndSprings(
        n: Int,
        clampedScale: Float,
        env: EnvironmentSpec,
    ) {
        // Pure Point-Mass Physics: neither radiation fields nor Hooke rest lengths couple to UI layout dimensions.
        for (i in 0 until n) {
            val b = bodyList[i]
            snapPx[i] = b.x
            snapPy[i] = b.y
            val rawR = b.spec.repulsionRadiusPx
            snapEffR[i] = if (rawR < 0f) {
                DeponderDefaults.INFINITE_RANGE
            } else if (rawR == 0f) {
                0f
            } else {
                (rawR + b.degree * env.degreeRadiusIncrementPx) * clampedScale
            }
            snapEc[i] = b.spec.elasticity
            outPairFx[i] = 0f
            outPairFy[i] = 0f
            accumEdgeFx[i] = 0f
            accumEdgeFy[i] = 0f
        }

        for (rIdx in rubberList.indices) {
            val spec = rubberList[rIdx].spec
            if (spec.startId == spec.endId || spec.elasticity == 0f) continue
            val sIdx = indexById[spec.startId] ?: continue
            val eIdx = indexById[spec.endId] ?: continue
            val diffX = snapPx[eIdx] - snapPx[sIdx]
            val diffY = snapPy[eIdx] - snapPy[sIdx]
            val distSq = diffX * diffX + diffY * diffY
            if (distSq > 0f) {
                val length = sqrt(distSq)
                val invLen = 1f / length
                val natural = spec.naturalLengthPx * clampedScale
                val springDeform = natural - length
                val mag = springDeform * spec.elasticity
                val fxOnS = -diffX * invLen * mag
                val fyOnS = -diffY * invLen * mag
                accumEdgeFx[sIdx] += fxOnS
                accumEdgeFy[sIdx] += fyOnS
                accumEdgeFx[eIdx] -= fxOnS
                accumEdgeFy[eIdx] -= fyOnS
            }
        }
    }

    private fun runSingleSubstepNonSuspend(
        n: Int,
        subDt: Float,
        clampedScale: Float,
        env: EnvironmentSpec,
        anyPressed: Boolean,
    ): Vec2 {
        prepareSnapshotAndSprings(n, clampedScale, env)

        val infStrength = env.infiniteChargeStrength * clampedScale
        val softeningSq = DeponderDefaults.Simulation.COULOMB_SOFTENING_SQ

        if (n < DeponderDefaults.Simulation.QUADTREE_NODE_THRESHOLD) {
            // Tier 1: Direct symmetric array loop.
            for (s in 0 until n) {
                val sx = snapPx[s]
                val sy = snapPy[s]
                val sR = snapEffR[s]
                val sEc = snapEc[s]
                val sInfQ = if (sR < 0f) sEc * infStrength else 0f

                for (e in s + 1 until n) {
                    val fOnS = HybridQuadtree.pairwiseForceOnS(
                        sx = sx,
                        sy = sy,
                        sR = sR,
                        sEc = sEc,
                        sInfCharge = sInfQ,
                        ex = snapPx[e],
                        ey = snapPy[e],
                        eR = snapEffR[e],
                        eEc = snapEc[e],
                        infiniteChargeStrength = infStrength,
                        softeningSq = softeningSq,
                    )
                    outPairFx[s] += fOnS.x
                    outPairFy[s] += fOnS.y
                    outPairFx[e] -= fOnS.x
                    outPairFy[e] -= fOnS.y
                }
            }
        } else {
            // Tier 2: Single-thread HybridQuadtree (64 <= N < 256).
            quadtree.build(
                count = n,
                px = snapPx,
                py = snapPy,
                effectiveRadius = snapEffR,
                elasticity = snapEc,
                infiniteChargeStrength = infStrength,
            )
            val thetaSq = DeponderDefaults.Simulation.BARNES_HUT_THETA * DeponderDefaults.Simulation.BARNES_HUT_THETA
            val stack = workerStacks[0]
            for (s in 0 until n) {
                val f = quadtree.queryForceOnNode(
                    s = s,
                    px = snapPx,
                    py = snapPy,
                    effectiveRadius = snapEffR,
                    elasticity = snapEc,
                    infiniteChargeStrength = infStrength,
                    thetaSq = thetaSq,
                    softeningSq = softeningSq,
                    stack = stack,
                )
                outPairFx[s] = f.x
                outPairFy[s] = f.y
            }
        }

        return integrateSubstep(n, subDt, clampedScale, env, anyPressed)
    }

    private fun integrateSubstep(
        n: Int,
        subDt: Float,
        clampedScale: Float,
        env: EnvironmentSpec,
        anyPressed: Boolean,
    ): Vec2 {
        val dampFactor = exp(-max(0f, env.airDamping) * subDt)
        val effectiveAlpha = if (env.coolingEnabled) alpha else 1f

        var maxSpeedSq = 0f
        var maxAccelSq = 0f

        for (i in 0 until n) {
            val b = bodyList[i]
            if (b.isPressed || b.spec.pinned) {
                b.vx = 0f
                b.vy = 0f
                continue
            }

            val wallF = computeWallForce(
                px = snapPx[i],
                py = snapPy[i],
                scale = clampedScale,
                width = viewportWidthPx,
                height = viewportHeightPx,
                env = env,
            )

            val totalFx = wallF.x + accumEdgeFx[i] + outPairFx[i]
            val totalFy = wallF.y + accumEdgeFy[i] + outPairFy[i]
            val invMass = 1f / max(1e-4f, b.spec.mass)

            val ax = totalFx * invMass * effectiveAlpha
            val ay = totalFy * invMass * effectiveAlpha

            // Semi-implicit Euler with exact exponential viscous drag — zero velocity deadband wiping!
            val nextVx = (b.vx + ax * subDt) * dampFactor
            val nextVy = (b.vy + ay * subDt) * dampFactor

            b.vx = nextVx
            b.vy = nextVy
            b.x += nextVx * subDt
            b.y += nextVy * subDt

            val speedSq = nextVx * nextVx + nextVy * nextVy
            val accelSq = ax * ax + ay * ay
            if (speedSq > maxSpeedSq) maxSpeedSq = speedSq
            if (accelSq > maxAccelSq) maxAccelSq = accelSq
        }

        if (env.coolingEnabled) {
            val target = if (anyPressed) DeponderDefaults.Simulation.INTERACTION_ALPHA else 0f
            val dtRatio = subDt / DeponderDefaults.Simulation.REFERENCE_DT_SECONDS
            val decay = 1f - (1f - env.alphaDecay.coerceIn(0f, 1f)).pow(dtRatio)
            alpha += (target - alpha) * decay
        }

        return Vec2(maxSpeedSq, maxAccelSq)
    }

    public companion object {
        private const val RAD_TO_DEG: Double = 180.0 / PI
        private const val GOLDEN_ANGLE_RAD: Double = 2.399963229728653
        private const val ROUNDED_CONTOUR_RATIO: Float = 0.35f
        private const val COLLINEAR_BAND_PX: Float = 6.0f
        private const val COLLINEAR_NUDGE_PX: Float = 3.5f

        /**
         * Computes viewport boundary force on `(px, py)`.
         *
         * - When `env.wallRepulsionPx >= 0f`: applies inward cushion within `wallRepulsionPx * scale` of each edge
         *   and unbounded linear recovery if the point crosses outside `[0, width] x [0, height]`.
         * - When `env.wallRepulsionPx < 0f` ([DeponderDefaults.INFINITE_RANGE]): all four walls push inward across
         *   the entire viewport, naturally forming a smooth equilibrium centering force.
         */
        public fun computeWallForce(
            px: Float,
            py: Float,
            scale: Float,
            width: Float,
            height: Float,
            env: EnvironmentSpec,
        ): Vec2 {
            if (width <= 0f && height <= 0f) return Vec2.Zero

            val rawRange = env.wallRepulsionPx
            if (rawRange < 0f) {
                val halfW = width * 0.5f
                val halfH = height * 0.5f
                val fx = (halfW - px) * (env.wallElasticityStart + env.wallElasticityEnd) * 0.5f
                val fy = (halfH - py) * (env.wallElasticityTop + env.wallElasticityBottom) * 0.5f
                return Vec2(fx, fy)
            }

            val range = rawRange * scale
            val l = if (px < range) range - px else 0f
            val t = if (py < range) range - py else 0f
            val r = if (width - px < range) range - (width - px) else 0f
            val b = if (height - py < range) range - (height - py) else 0f

            return Vec2(
                x = l * env.wallElasticityStart - r * env.wallElasticityEnd,
                y = t * env.wallElasticityTop - b * env.wallElasticityBottom,
            )
        }
    }
}
