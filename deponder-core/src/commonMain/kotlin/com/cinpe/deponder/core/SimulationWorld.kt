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
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin Multiplatform force-directed physics world.
 *
 * Implements:
 * - Strict pure point-mass force dynamics (center coordinates + mass + independent radiation field,
 *   zero rigid-body collision coupling) paired with real-time directional contour radii on
 *   [RubberPose] so UI link layers can apply [DeponderRubberInsets] with zero relayout overhead.
 * - Deterministic golden-angle spiral symmetry breaking when stacked nodes start at identical `(x, y)` coordinates.
 * - Adaptive 3-tier execution:
 *   1. Small graphs (`N < 64`): direct symmetric array loop on current thread.
 *   2. Medium graphs (`64 <= N < 256`): single-thread zero-allocation [HybridQuadtree] without coroutine dispatch overhead.
 *   3. Large graphs (`N >= 256`): single outer [Dispatchers.Default] switch per VSYNC wrapping all substeps
 *      operating exclusively on isolated primitive snapshots (zero cross-thread collection/body access).
 * - Adaptive `1/120s` exact-division sub-stepping (`MAX_SUBSTEPS = 4`), pure Newtonian viscous air
 *   damping (`airDamping`, `0f` for undamped perpetual motion), and frame-rate-independent Global Kinetic-Force Sleep.
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

    /**
     * Deprecated (constant `1.0f` since 0.6.1): simulated annealing temperature scaling has been
     * retired so forces always act at full Newtonian strength; energy dissipation is governed solely
     * by [EnvironmentSpec.airDamping].
     */
    @Deprecated(
        message = "No-op since 0.6.1: always 1.0f. Simulated annealing has been retired; configure airDamping instead.",
    )
    public var alpha: Float = 1.0f
        private set

    private sealed interface CausalIntent {
        class BodyPoseOverride(val id: String, var x: Float, var y: Float) : CausalIntent
        data class BodyVelocityZero(val id: String) : CausalIntent
        data object ResetSettle : CausalIntent
    }

    private var settleTimeSeconds: Float = 0f
    private var lastRefreshedScale: Float = Float.NaN
    private var perturbationCounter: Int = 0
    private var asyncStepInFlight: Boolean = false
    private val inFlightJournal = ArrayList<CausalIntent>(4)

    private val unfolder = TopologicalUnfolder()
    private var unfoldRequested: Boolean = false
    private var unfoldAwaitingViewport: Boolean = false
    private var unfoldDirty: Boolean = false
    private var unfoldRemainingSeconds: Float = 0f
    private var cancelInFlightUnfold: Boolean = false

    private val bodiesById = LinkedHashMap<String, PlanetBody>()
    private val bodyList = ArrayList<PlanetBody>()
    private val indexById = HashMap<String, Int>()

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
    private var unfoldTargetX = FloatArray(bufCap)
    private var unfoldTargetY = FloatArray(bufCap)
    private var unfoldLocked = BooleanArray(bufCap)
    private var unfoldEstablished = BooleanArray(bufCap)

    private var unfoldEdgeCap = 64
    private var unfoldEdgeStart = IntArray(unfoldEdgeCap)
    private var unfoldEdgeEnd = IntArray(unfoldEdgeCap)
    private var unfoldEdgeLen = FloatArray(unfoldEdgeCap)

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

    private fun removeInFlightBodyEntries(id: String) {
        var i = inFlightJournal.size - 1
        while (i >= 0) {
            when (val entry = inFlightJournal[i]) {
                is CausalIntent.BodyPoseOverride -> if (entry.id == id) inFlightJournal.removeAt(i)
                is CausalIntent.BodyVelocityZero -> if (entry.id == id) inFlightJournal.removeAt(i)
                CausalIntent.ResetSettle -> Unit
            }
            i -= 1
        }
    }

    private fun recordBodyPoseOverride(id: String, x: Float, y: Float) {
        val last = inFlightJournal.lastOrNull()
        if (last is CausalIntent.BodyPoseOverride && last.id == id) {
            last.x = x
            last.y = y
            return
        }
        removeInFlightBodyEntries(id)
        inFlightJournal.add(CausalIntent.BodyPoseOverride(id, x, y))
    }

    private fun recordBodyVelocityZero(id: String) {
        val last = inFlightJournal.lastOrNull()
        if ((last is CausalIntent.BodyVelocityZero && last.id == id) ||
            (last is CausalIntent.BodyPoseOverride && last.id == id)
        ) {
            return
        }
        inFlightJournal.add(CausalIntent.BodyVelocityZero(id))
    }

    private fun resetSettleTimer() {
        settleTimeSeconds = 0f
        if (asyncStepInFlight) {
            val last = inFlightJournal.lastOrNull()
            if (last !is CausalIntent.ResetSettle) {
                inFlightJournal.add(CausalIntent.ResetSettle)
            }
        }
    }

    private fun reduceLockBodyPose(body: PlanetBody, x: Float, y: Float) {
        body.x = x
        body.y = y
        body.vx = 0f
        body.vy = 0f
        body.hasEstablishedPose = true
        body.hasUserPose = true
        if (body.isPressed) {
            unfoldRemainingSeconds = 0f
            unfoldAwaitingViewport = false
        }
        indexById[body.spec.id]?.let { idx ->
            if (idx < unfoldTargetX.size) {
                unfoldTargetX[idx] = x
                unfoldTargetY[idx] = y
            }
        }
        if (asyncStepInFlight) {
            recordBodyPoseOverride(body.spec.id, x, y)
        }
    }

    private fun reduceZeroBodyVelocity(body: PlanetBody) {
        body.vx = 0f
        body.vy = 0f
        if (asyncStepInFlight) {
            recordBodyVelocityZero(body.spec.id)
        }
    }

    private fun clampScale(scale: Float): Float {
        val fallback = environment.initScale.coerceIn(environment.minScale, environment.maxScale)
        return if (scale.isFinite()) scale.coerceIn(environment.minScale, environment.maxScale) else fallback
    }

    public fun updateEnvironment(spec: EnvironmentSpec, reheatSimulation: Boolean = true) {
        if (environment != spec) {
            @Suppress("DEPRECATION")
            val wasUnfold = environment.unfoldEnabled
            @Suppress("DEPRECATION")
            val nextUnfold = spec.unfoldEnabled
            environment = spec
            if (!wasUnfold && nextUnfold) {
                unfoldDirty = true
                cancelInFlightUnfold = false
                resetSettleTimer()
            } else if (wasUnfold && !nextUnfold) {
                unfoldDirty = false
                if (!unfoldRequested) {
                    unfoldRemainingSeconds = 0f
                    if (asyncStepInFlight) {
                        cancelInFlightUnfold = true
                    }
                }
            }
            if (reheatSimulation) {
                resetSettleTimer()
            }
        }
    }

    public fun updateViewport(widthPx: Float, heightPx: Float) {
        if (!widthPx.isFinite() || !heightPx.isFinite()) return
        val w = max(0f, widthPx)
        val h = max(0f, heightPx)
        if (viewportWidthPx != w || viewportHeightPx != h) {
            val wasEmpty = viewportWidthPx <= 0f && viewportHeightPx <= 0f
            viewportWidthPx = w
            viewportHeightPx = h
            if (wasEmpty && (w > 0f || h > 0f)) {
                for (i in bodyList.indices) {
                    val b = bodyList[i]
                    if (!b.hasUserPose && !b.spec.pinned && !b.isPressed) {
                        b.hasEstablishedPose = false
                    }
                }
                @Suppress("DEPRECATION")
                if (environment.unfoldEnabled) {
                    unfoldDirty = true
                }
                if (unfoldAwaitingViewport) {
                    unfoldAwaitingViewport = false
                    unfoldRequested = true
                }
            }
            resetSettleTimer()
        }
    }

    /**
     * Deprecated (no-op since 0.6.1): simulated annealing (`alpha` reheating) has been retired so
     * Hooke springs obey pure Newtonian mechanics governed solely by [EnvironmentSpec.airDamping].
     * Real topology, physics, viewport, scale, and drag mutations reset idle sleep automatically;
     * invoke [unfold] when untangling graph topology.
     */
    @Deprecated(
        message = "No-op since 0.6.1: simulated annealing has been retired. Real mutations wake the world automatically; invoke unfold() to untangle topology.",
    )
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    public fun reheat(minAlpha: Float = DeponderDefaults.Simulation.INITIAL_ALPHA) {
        // Intentional no-op since 0.6.1.
    }

    /**
     * Schedules a one-shot sparse topological unfold pass on the next simulation frame and wakes
     * the simulation immediately.
     *
     * Safe to call before planets, rubbers, or non-zero viewport bounds are laid out: the request
     * remains armed until the next active frame (and re-aligns once initial `0x0` viewport bounds
     * resolve before user interaction), then fades out completely into pure Newtonian spring physics.
     */
    public fun unfold() {
        unfoldRequested = true
        unfoldAwaitingViewport = viewportWidthPx <= 0f && viewportHeightPx <= 0f
        cancelInFlightUnfold = false
        resetSettleTimer()
    }

    /**
     * Deprecated (no-op since 0.6.1): manual annealing temperature wakeups have been retired.
     * Real mutations (`upsertPlanet`, `upsertRubber`, `updateEnvironment`, `updateViewport`,
     * scale-changing [refreshRubberPoses], and pointer drags) wake the simulation automatically.
     */
    @Deprecated(
        message = "No-op since 0.6.1: manual alpha wakeups have been retired; topology, physics, scale, and drag updates wake the world automatically.",
    )
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    @kotlin.jvm.JvmOverloads
    public fun wake(minAlpha: Float = DeponderDefaults.Simulation.INTERACTION_ALPHA) {
        // Intentional no-op since 0.6.1.
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

    private fun validatePlanetSpec(spec: PlanetSpec) {
        require(spec.mass.isFinite() && spec.mass > 0f) {
            "PlanetSpec.mass must be finite and > 0f, but was ${spec.mass}"
        }
        require(spec.repulsionRadiusPx.isFinite()) {
            "PlanetSpec.repulsionRadiusPx must be finite, but was ${spec.repulsionRadiusPx}"
        }
        require(spec.elasticity.isFinite()) {
            "PlanetSpec.elasticity must be finite, but was ${spec.elasticity}"
        }
    }

    private fun validateRubberSpec(spec: RubberSpec) {
        require(spec.elasticity.isFinite()) {
            "RubberSpec.elasticity must be finite, but was ${spec.elasticity}"
        }
        require(spec.naturalLengthPx.isFinite() && spec.naturalLengthPx >= 0f) {
            "RubberSpec.naturalLengthPx must be finite and >= 0f, but was ${spec.naturalLengthPx}"
        }
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
        validatePlanetSpec(spec)
        require(initialX.isFinite() && initialY.isFinite()) {
            "Planet initial coordinates must be finite, but were ($initialX, $initialY)"
        }
        val existing = bodiesById[spec.id]
        if (existing != null) {
            if (existing.spec.pinned != spec.pinned) {
                unfoldDirty = true
            }
            existing.updateSpecInternal(spec)
            if (spec.pinned) {
                existing.hasEstablishedPose = true
                existing.hasUserPose = true
                reduceZeroBodyVelocity(existing)
            }
        } else {
            val resolved = resolveInitialPosition(initialX, initialY, spec.pinned)
            val created = PlanetBody(spec, resolved.x, resolved.y)
            created.hasEstablishedPose = spec.pinned
            created.hasUserPose = spec.pinned
            bodiesById[spec.id] = created
            bodyList.add(created)
            unfoldDirty = true
            if (asyncStepInFlight) {
                recordBodyPoseOverride(created.spec.id, resolved.x, resolved.y)
            }
        }
        rebuildTopologyIndex(scale)
        resetSettleTimer()
    }

    /**
     * Unregisters a planet by [id] and automatically gates off any incident rubbers.
     */
    public fun removePlanet(id: String, scale: Float = environment.initScale): Boolean {
        if (bodiesById.remove(id) == null) return false
        unfoldDirty = true
        if (asyncStepInFlight) {
            removeInFlightBodyEntries(id)
        }
        rebuildTopologyIndex(scale)
        resetSettleTimer()
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
        validateRubberSpec(spec)
        val key = spec.key
        val existing = declaredRubbersByKey[key]
        if (existing != null &&
            existing.spec.startId == spec.startId &&
            existing.spec.endId == spec.endId
        ) {
            if (abs(existing.spec.naturalLengthPx - spec.naturalLengthPx) > 1.0f ||
                (existing.spec.elasticity <= 0f) != (spec.elasticity <= 0f)
            ) {
                unfoldDirty = true
            }
            existing.updateSpecInternal(spec)
            existing.dirty = true
            refreshRubberPoses(scale)
            resetSettleTimer()
            return existing
        }
        if (existing == null ||
            existing.spec.startId != spec.startId ||
            existing.spec.endId != spec.endId ||
            abs(existing.spec.naturalLengthPx - spec.naturalLengthPx) > 1.0f ||
            (existing.spec.elasticity <= 0f) != (spec.elasticity <= 0f)
        ) {
            unfoldDirty = true
        }
        val pose = declaredRubbersByKey.getOrPut(key) { RubberPose(spec) }
        pose.updateSpecInternal(spec)
        pose.dirty = true
        rebuildTopologyIndex(scale)
        resetSettleTimer()
        return pose
    }

    /**
     * Unregisters an undirected spring rubber link by [key].
     */
    public fun removeRubber(key: EdgeKey, scale: Float = environment.initScale): Boolean {
        if (declaredRubbersByKey.remove(key) == null) return false
        unfoldDirty = true
        rebuildTopologyIndex(scale)
        resetSettleTimer()
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
        // Validate all incoming specs & initial coordinates upfront so invalid inputs fail atomically.
        var pendingNewPositions: ArrayList<Vec2>? = null
        for (i in planets.indices) {
            val spec = planets[i]
            validatePlanetSpec(spec)
            if (!bodiesById.containsKey(spec.id)) {
                val rawPos = initialPositionProvider(spec)
                require(rawPos.x.isFinite() && rawPos.y.isFinite()) {
                    "Planet initial coordinates must be finite, but were (${rawPos.x}, ${rawPos.y})"
                }
                val list = pendingNewPositions ?: ArrayList<Vec2>().also { pendingNewPositions = it }
                list.add(rawPos)
            }
        }
        for (i in edges.indices) {
            validateRubberSpec(edges[i])
        }

        val nextIds = HashSet<String>(planets.size * 2)
        for (spec in planets) {
            nextIds.add(spec.id)
        }

        var topologyChanged = false

        val iter = bodiesById.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.key !in nextIds) {
                if (asyncStepInFlight) {
                    removeInFlightBodyEntries(entry.key)
                }
                iter.remove()
                topologyChanged = true
            }
        }

        bodyList.clear()
        bodyList.addAll(bodiesById.values)

        var newPosIdx = 0
        for (spec in planets) {
            val existing = bodiesById[spec.id]
            if (existing != null) {
                if (existing.spec.pinned != spec.pinned) {
                    topologyChanged = true
                }
                existing.updateSpecInternal(spec)
                if (spec.pinned) {
                    existing.hasEstablishedPose = true
                    existing.hasUserPose = true
                    reduceZeroBodyVelocity(existing)
                }
            } else {
                topologyChanged = true
                val rawPos = pendingNewPositions!![newPosIdx]
                newPosIdx += 1
                val resolved = resolveInitialPosition(rawPos.x, rawPos.y, spec.pinned)
                val created = PlanetBody(spec, resolved.x, resolved.y)
                created.hasEstablishedPose = spec.pinned
                created.hasUserPose = spec.pinned
                bodiesById[spec.id] = created
                bodyList.add(created)
                if (asyncStepInFlight) {
                    recordBodyPoseOverride(created.spec.id, resolved.x, resolved.y)
                }
            }
        }

        val nextKeys = HashSet<EdgeKey>(edges.size * 2)
        for (edge in edges) {
            val key = edge.key
            if (!nextKeys.add(key)) continue
            val existingPose = declaredRubbersByKey[key]
            if (existingPose != null) {
                val prevSpec = existingPose.spec
                if (prevSpec.startId != edge.startId ||
                    prevSpec.endId != edge.endId ||
                    abs(prevSpec.naturalLengthPx - edge.naturalLengthPx) > 1.0f ||
                    (prevSpec.elasticity <= 0f) != (edge.elasticity <= 0f)
                ) {
                    topologyChanged = true
                }
                existingPose.updateSpecInternal(edge)
                existingPose.dirty = true
            } else {
                declaredRubbersByKey[key] = RubberPose(edge)
                topologyChanged = true
            }
        }

        val rIter = declaredRubbersByKey.entries.iterator()
        while (rIter.hasNext()) {
            if (rIter.next().key !in nextKeys) {
                rIter.remove()
                topologyChanged = true
            }
        }

        if (topologyChanged) {
            unfoldDirty = true
        }
        rebuildTopologyIndex(environment.initScale)
        resetSettleTimer()
    }

    private fun rebuildTopologyIndex(scale: Float) {
        val clampedScale = clampScale(scale)
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
                if (pose.isActive || pose.centerDistance != 0f) {
                    pose.isActive = false
                    pose.centerDistance = 0f
                    pose.directionX = 0f
                    pose.directionY = 0f
                    pose.startContourPx = 0f
                    pose.endContourPx = 0f
                    pose.dirty = true
                }
                continue
            }
            val sIdx = indexById[spec.startId]
            val eIdx = indexById[spec.endId]
            if (sIdx == null || eIdx == null) {
                if (pose.isActive || pose.centerDistance != 0f) {
                    pose.isActive = false
                    pose.centerDistance = 0f
                    pose.directionX = 0f
                    pose.directionY = 0f
                    pose.startContourPx = 0f
                    pose.endContourPx = 0f
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
        clampedScale: Float,
        ux: Float,
        uy: Float,
    ): Float {
        if (halfWidthPx <= 0f && halfHeightPx <= 0f) return 0f
        val w = max(1e-3f, max(0f, halfWidthPx) * clampedScale)
        val h = max(1e-3f, max(0f, halfHeightPx) * clampedScale)
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
        val rawCx = (sx + ex) * 0.5f
        val rawCy = (sy + ey) * 0.5f

        val rotDeg: Float
        val ux: Float
        val uy: Float
        val sContourPx: Float
        val eContourPx: Float

        if (centerDist > 1e-4f) {
            val angle = atan2(diffX.toDouble(), diffY.toDouble())
            rotDeg = (90.0 - angle * RAD_TO_DEG).toFloat()
            val invDist = 1f / centerDist
            ux = diffX * invDist
            uy = diffY * invDist
            sContourPx = computeBodyContourClearance(
                halfWidthPx = sBody.halfWidthPx,
                halfHeightPx = sBody.halfHeightPx,
                clampedScale = clampedScale,
                ux = ux,
                uy = uy,
            )
            eContourPx = computeBodyContourClearance(
                halfWidthPx = eBody.halfWidthPx,
                halfHeightPx = eBody.halfHeightPx,
                clampedScale = clampedScale,
                ux = ux,
                uy = uy,
            )
        } else {
            rotDeg = pose.rotationDeg
            ux = 0f
            uy = 0f
            sContourPx = 0f
            eContourPx = 0f
        }

        if (!pose.isActive ||
            pose.centerX != rawCx ||
            pose.centerY != rawCy ||
            pose.centerDistance != centerDist ||
            pose.directionX != ux ||
            pose.directionY != uy ||
            pose.startContourPx != sContourPx ||
            pose.endContourPx != eContourPx ||
            pose.rotationDeg != rotDeg ||
            pose.scale != clampedScale
        ) {
            pose.isActive = true
            pose.centerX = rawCx
            pose.centerY = rawCy
            pose.centerDistance = centerDist
            pose.directionX = ux
            pose.directionY = uy
            pose.startContourPx = sContourPx
            pose.endContourPx = eContourPx
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
        if (!widthPx.isFinite() || !heightPx.isFinite()) return
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
     * Updates the spring [elasticity] across all declared rubbers and wakes the simulation.
     */
    public fun updateAllRubbersElasticity(elasticity: Float) {
        require(elasticity.isFinite()) { "elasticity must be finite, but was $elasticity" }
        var changed = false
        for (i in rubberList.indices) {
            val pose = rubberList[i]
            if (pose.spec.elasticity != elasticity) {
                pose.updateSpecInternal(pose.spec.copy(elasticity = elasticity))
                changed = true
            }
        }
        if (changed) {
            resetSettleTimer()
        }
    }

    /**
     * Moves planet [id] directly to `(x, y)` in root pixel coordinates, zeroing its velocity and waking
     * connected neighbors.
     */
    public fun setBodyPosition(id: String, x: Float, y: Float, scale: Float = environment.initScale) {
        require(x.isFinite() && y.isFinite()) { "Body position must be finite, but were ($x, $y)" }
        val body = bodiesById[id] ?: return
        if (body.x != x || body.y != y) {
            reduceLockBodyPose(body, x, y)
            refreshRubberPoses(scale)
            resetSettleTimer()
        }
    }

    /**
     * Offsets planet [id] by `(dx, dy)` in root pixel coordinates and wakes the simulation.
     */
    public fun offsetBodyPosition(id: String, dx: Float, dy: Float, scale: Float = environment.initScale) {
        require(dx.isFinite() && dy.isFinite()) { "Body offset must be finite, but were ($dx, $dy)" }
        if (dx == 0f && dy == 0f) return
        val body = bodiesById[id] ?: return
        reduceLockBodyPose(body, body.x + dx, body.y + dy)
        refreshRubberPoses(scale)
        resetSettleTimer()
    }

    public fun setBodyPressed(id: String, pressed: Boolean) {
        val body = bodiesById[id] ?: return
        body.isPressed = pressed
        reduceZeroBodyVelocity(body)
        resetSettleTimer()
    }

    public fun setBodyPinned(id: String, pinned: Boolean) {
        val body = bodiesById[id] ?: return
        if (body.spec.pinned != pinned) {
            body.updateSpecInternal(body.spec.copy(pinned = pinned))
            unfoldDirty = true
            if (pinned) {
                body.hasEstablishedPose = true
                body.hasUserPose = true
                reduceZeroBodyVelocity(body)
            } else {
                resetSettleTimer()
            }
        }
    }

    /**
     * Synchronizes all active [RubberPose] transforms immediately for the given [scale].
     * When [scale] changes across updates, resets the sleep settle timer automatically.
     */
    public fun refreshRubberPoses(scale: Float) {
        val clampedScale = clampScale(scale)
        if (!lastRefreshedScale.isNaN() && lastRefreshedScale != clampedScale) {
            resetSettleTimer()
        }
        lastRefreshedScale = clampedScale
        for (i in rubberList.indices) {
            val pose = rubberList[i]
            val spec = pose.spec
            if (spec.startId == spec.endId) {
                pose.isActive = false
                pose.centerDistance = 0f
                pose.directionX = 0f
                pose.directionY = 0f
                pose.startContourPx = 0f
                pose.endContourPx = 0f
                continue
            }
            val sIdx = indexById[spec.startId]
            val eIdx = indexById[spec.endId]
            if (sIdx == null || eIdx == null) {
                pose.isActive = false
                pose.centerDistance = 0f
                pose.directionX = 0f
                pose.directionY = 0f
                pose.startContourPx = 0f
                pose.endContourPx = 0f
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
        unfoldTargetX = FloatArray(bufCap)
        unfoldTargetY = FloatArray(bufCap)
        unfoldLocked = BooleanArray(bufCap)
        unfoldEstablished = BooleanArray(bufCap)
    }

    private fun ensureUnfoldEdgeCapacity(m: Int) {
        if (m <= unfoldEdgeCap) return
        unfoldEdgeCap = max(m, unfoldEdgeCap * 2)
        unfoldEdgeStart = IntArray(unfoldEdgeCap)
        unfoldEdgeEnd = IntArray(unfoldEdgeCap)
        unfoldEdgeLen = FloatArray(unfoldEdgeCap)
    }

    private fun collectUnfoldInputs(n: Int, clampedScale: Float): Int {
        for (i in 0 until n) {
            val b = bodyList[i]
            snapPx[i] = b.x
            snapPy[i] = b.y
            unfoldLocked[i] = b.isPressed || b.spec.pinned
            unfoldEstablished[i] = b.hasEstablishedPose
        }
        val rTotal = rubberList.size
        ensureUnfoldEdgeCapacity(rTotal)
        var edgeCount = 0
        for (rIdx in 0 until rTotal) {
            val spec = rubberList[rIdx].spec
            if (spec.startId == spec.endId || spec.elasticity <= 0f) continue
            val sIdx = indexById[spec.startId] ?: continue
            val eIdx = indexById[spec.endId] ?: continue
            if (sIdx == eIdx) continue
            unfoldEdgeStart[edgeCount] = sIdx
            unfoldEdgeEnd[edgeCount] = eIdx
            unfoldEdgeLen[edgeCount] = spec.naturalLengthPx * clampedScale
            edgeCount += 1
        }
        return edgeCount
    }

    private fun computeUnfoldAlpha(remainingSeconds: Float): Float {
        if (remainingSeconds <= 0f) return 0f
        val u = (remainingSeconds / UNFOLD_DURATION_SECONDS).coerceIn(0f, 1f)
        return u * u * (3f - 2f * u)
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
        if (asyncStepInFlight) return true
        val n = bodyList.size
        if (n == 0) return false
        ensureBuffers(n)

        val clampedDt = if (dtSeconds.isFinite()) {
            dtSeconds.coerceIn(1e-4f, DeponderDefaults.Simulation.MAX_DT_SECONDS)
        } else {
            1e-4f
        }
        val clampedScale = clampScale(scale)
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

        val maxSpeedSq: Float
        val maxAccelSq: Float

        if (n >= DeponderDefaults.Simulation.PARALLEL_DISPATCH_THRESHOLD) {
            // Tier 3 (N >= 256): Capture an immutable-structure primitive snapshot on the caller thread
            // BEFORE suspending to Dispatchers.Default, and replay any caller-thread CausalIntents recorded
            // during the async window strictly in FIFO order upon return.
            val ids = Array(n) { i -> bodyList[i].spec.id }
            val startX = FloatArray(n) { i -> bodyList[i].x }
            val startY = FloatArray(n) { i -> bodyList[i].y }
            val px = startX.copyOf()
            val py = startY.copyOf()
            val vx = FloatArray(n) { i -> bodyList[i].vx }
            val vy = FloatArray(n) { i -> bodyList[i].vy }
            val invMass = FloatArray(n) { i -> 1f / max(1e-4f, bodyList[i].spec.mass) }
            val locked = BooleanArray(n) { i -> bodyList[i].isPressed || bodyList[i].spec.pinned }
            val effR = FloatArray(n) { i ->
                val rawR = bodyList[i].spec.repulsionRadiusPx
                if (rawR < 0f) {
                    DeponderDefaults.INFINITE_RANGE
                } else if (rawR == 0f) {
                    0f
                } else {
                    rawR * clampedScale
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
            val wasExplicitUnfold = unfoldRequested
            @Suppress("DEPRECATION")
            val wasLegacyAutoUnfold = env.unfoldEnabled && unfoldDirty
            val needUnfoldSolve = wasExplicitUnfold || wasLegacyAutoUnfold
            if (wasExplicitUnfold) unfoldRequested = false
            if (wasLegacyAutoUnfold) unfoldDirty = false
            if (needUnfoldSolve && (vpW > 0f || vpH > 0f)) {
                unfoldAwaitingViewport = false
            }
            val established = BooleanArray(n) { i -> bodyList[i].hasEstablishedPose }
            val massArr = FloatArray(n) { i -> bodyList[i].spec.mass }
            val initialUnfoldRem = unfoldRemainingSeconds
            val hadUnfoldWork = needUnfoldSolve || initialUnfoldRem > 0f
            cancelInFlightUnfold = false
            val snapTargetX = FloatArray(n) { i ->
                if (i < unfoldTargetX.size) unfoldTargetX[i] else bodyList[i].x
            }
            val snapTargetY = FloatArray(n) { i ->
                if (i < unfoldTargetY.size) unfoldTargetY[i] else bodyList[i].y
            }

            inFlightJournal.clear()
            asyncStepInFlight = true
            val tier3Metrics = try {
                withContext(Dispatchers.Default) {
                    runTier3ParallelSubsteps(
                        n = n,
                        subSteps = subSteps,
                        subDt = subDt,
                        clampedScale = clampedScale,
                        vpW = vpW,
                        vpH = vpH,
                        env = env,
                        needUnfoldSolve = needUnfoldSolve,
                        initialUnfoldRem = initialUnfoldRem,
                        established = established,
                        massArr = massArr,
                        targetX = snapTargetX,
                        targetY = snapTargetY,
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
            } catch (t: Throwable) {
                if (wasExplicitUnfold) {
                    unfoldRequested = true
                    if (vpW <= 0f && vpH <= 0f) {
                        unfoldAwaitingViewport = true
                    }
                }
                @Suppress("DEPRECATION")
                if (wasLegacyAutoUnfold && environment.unfoldEnabled) {
                    unfoldDirty = true
                }
                inFlightJournal.clear()
                throw t
            } finally {
                asyncStepInFlight = false
            }

            // Commit pure Tier-3 frame results on the caller thread, then replay any in-flight causal
            // intents recorded during suspension in strict FIFO order.
            val canceledUnfold = cancelInFlightUnfold
            cancelInFlightUnfold = false
            val shouldCommitUnfold = !canceledUnfold && hadUnfoldWork
            if (inFlightJournal.isEmpty()) {
                if (canceledUnfold) {
                    unfoldRemainingSeconds = 0f
                } else if (hadUnfoldWork) {
                    unfoldRemainingSeconds = tier3Metrics.finalUnfoldRem
                }
                for (i in 0 until n) {
                    val b = bodyList[i]
                    if (shouldCommitUnfold) {
                        unfoldTargetX[i] = tier3Metrics.targetX[i]
                        unfoldTargetY[i] = tier3Metrics.targetY[i]
                    }
                    b.hasEstablishedPose = true
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
                if (canceledUnfold) {
                    unfoldRemainingSeconds = 0f
                } else if (hadUnfoldWork) {
                    unfoldRemainingSeconds = tier3Metrics.finalUnfoldRem
                }
                for (i in 0 until n) {
                    val currentBody = bodiesById[ids[i]] ?: continue
                    currentBody.hasEstablishedPose = true
                    if (shouldCommitUnfold) {
                        val idx = indexById[ids[i]]
                        if (idx != null && idx < unfoldTargetX.size) {
                            unfoldTargetX[idx] = tier3Metrics.targetX[i]
                            unfoldTargetY[idx] = tier3Metrics.targetY[i]
                        }
                    }
                    if (currentBody.isPressed || currentBody.spec.pinned) {
                        currentBody.vx = 0f
                        currentBody.vy = 0f
                        currentBody.dx = 0f
                        currentBody.dy = 0f
                    } else {
                        currentBody.x = px[i]
                        currentBody.y = py[i]
                        currentBody.vx = vx[i]
                        currentBody.vy = vy[i]
                        currentBody.dx = px[i] - startX[i]
                        currentBody.dy = py[i] - startY[i]
                    }
                }
                var keepAwakeFromIntent = false
                for (idx in 0 until inFlightJournal.size) {
                    when (val entry = inFlightJournal[idx]) {
                        is CausalIntent.BodyPoseOverride -> {
                            bodiesById[entry.id]?.let { b ->
                                b.x = entry.x
                                b.y = entry.y
                                b.vx = 0f
                                b.vy = 0f
                                b.dx = 0f
                                b.dy = 0f
                                b.hasEstablishedPose = true
                                b.hasUserPose = true
                                if (b.isPressed) {
                                    unfoldRemainingSeconds = 0f
                                    unfoldAwaitingViewport = false
                                }
                                indexById[entry.id]?.let { bodyIdx ->
                                    if (bodyIdx < unfoldTargetX.size) {
                                        unfoldTargetX[bodyIdx] = entry.x
                                        unfoldTargetY[bodyIdx] = entry.y
                                    }
                                }
                            }
                            settleTimeSeconds = 0f
                            keepAwakeFromIntent = true
                        }

                        is CausalIntent.BodyVelocityZero -> {
                            bodiesById[entry.id]?.let { b ->
                                b.vx = 0f
                                b.vy = 0f
                                b.dx = 0f
                                b.dy = 0f
                            }
                        }

                        CausalIntent.ResetSettle -> {
                            settleTimeSeconds = 0f
                            keepAwakeFromIntent = true
                        }
                    }
                }
                inFlightJournal.clear()
                if (bodyList.isEmpty()) {
                    return false
                }
                if (keepAwakeFromIntent) {
                    refreshRubberPoses(clampedScale)
                    return true
                }
            }

            maxSpeedSq = tier3Metrics.maxSpeedSq
            maxAccelSq = tier3Metrics.maxAccelSq
        } else {
            // Tier 1 (N < 64) & Tier 2 (64 <= N < 256): Zero coroutine context-switch overhead.
            val wasExplicitUnfold = unfoldRequested
            @Suppress("DEPRECATION")
            val wasLegacyAutoUnfold = env.unfoldEnabled && unfoldDirty
            if (wasExplicitUnfold || wasLegacyAutoUnfold) {
                unfoldRequested = false
                if (wasLegacyAutoUnfold) {
                    unfoldDirty = false
                }
                if (viewportWidthPx > 0f || viewportHeightPx > 0f) {
                    unfoldAwaitingViewport = false
                }
                val edgeCount = collectUnfoldInputs(n, clampedScale)
                val solved = unfolder.solve(
                    n = n,
                    curX = snapPx,
                    curY = snapPy,
                    locked = unfoldLocked,
                    established = unfoldEstablished,
                    edgeCount = edgeCount,
                    edgeStart = unfoldEdgeStart,
                    edgeEnd = unfoldEdgeEnd,
                    edgeLength = unfoldEdgeLen,
                    viewportWidth = viewportWidthPx,
                    viewportHeight = viewportHeightPx,
                    wallRepulsionPx = env.wallRepulsionPx,
                    clampedScale = clampedScale,
                    outTargetX = unfoldTargetX,
                    outTargetY = unfoldTargetY,
                )
                if (solved) {
                    unfoldRemainingSeconds = UNFOLD_DURATION_SECONDS
                    for (i in 0 until n) {
                        bodyList[i].hasEstablishedPose = true
                    }
                } else {
                    unfoldRemainingSeconds = 0f
                }
            }

            var lastSpeedSq = 0f
            var lastAccelSq = 0f
            for (sub in 0 until subSteps) {
                val metrics = runSingleSubstepNonSuspend(
                    n = n,
                    subDt = subDt,
                    clampedScale = clampedScale,
                    env = env,
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
                b.hasEstablishedPose = true
            }
            maxSpeedSq = lastSpeedSq
            maxAccelSq = lastAccelSq
        }

        if (bodyList.isEmpty()) {
            return false
        }

        // Update visual rubber poses once at the end of the VSYNC step.
        refreshRubberPoses(clampedScale)

        if (anyPressed || bodyList.any { it.isPressed }) {
            settleTimeSeconds = 0f
            return true
        }

        if (unfoldRemainingSeconds > 0f) {
            settleTimeSeconds = 0f
            return true
        }

        val restSpeed = env.restSpeedPxPerSec * clampedScale
        val restAccel = env.restAccelPxPerSecSq * clampedScale
        val isKineticCalm = maxSpeedSq <= restSpeed * restSpeed
        val isForceCalm = maxAccelSq <= restAccel * restAccel

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
        val maxSpeedSq: Float,
        val maxAccelSq: Float,
        val finalUnfoldRem: Float,
        val targetX: FloatArray,
        val targetY: FloatArray,
    )

    private suspend fun runTier3ParallelSubsteps(
        n: Int,
        subSteps: Int,
        subDt: Float,
        clampedScale: Float,
        vpW: Float,
        vpH: Float,
        env: EnvironmentSpec,
        needUnfoldSolve: Boolean,
        initialUnfoldRem: Float,
        established: BooleanArray,
        massArr: FloatArray,
        targetX: FloatArray,
        targetY: FloatArray,
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

        var curUnfoldRem = initialUnfoldRem
        if (needUnfoldSolve) {
            var unfoldEdgeCount = 0
            for (r in 0 until activeSprings) {
                if (rElastArr[r] > 0f) unfoldEdgeCount += 1
            }
            val uStart = IntArray(unfoldEdgeCount)
            val uEnd = IntArray(unfoldEdgeCount)
            val uLen = FloatArray(unfoldEdgeCount)
            var uCursor = 0
            for (r in 0 until activeSprings) {
                if (rElastArr[r] > 0f) {
                    uStart[uCursor] = sIdxArr[r]
                    uEnd[uCursor] = eIdxArr[r]
                    uLen[uCursor] = natLenArr[r]
                    uCursor += 1
                }
            }
            val localUnfolder = TopologicalUnfolder()
            val solved = localUnfolder.solve(
                n = n,
                curX = px,
                curY = py,
                locked = locked,
                established = established,
                edgeCount = unfoldEdgeCount,
                edgeStart = uStart,
                edgeEnd = uEnd,
                edgeLength = uLen,
                viewportWidth = vpW,
                viewportHeight = vpH,
                wallRepulsionPx = env.wallRepulsionPx,
                clampedScale = clampedScale,
                outTargetX = targetX,
                outTargetY = targetY,
            )
            curUnfoldRem = if (solved) UNFOLD_DURATION_SECONDS else 0f
        }

        val infStrength = env.infiniteChargeStrength * clampedScale
        val softeningSq = DeponderDefaults.Simulation.COULOMB_SOFTENING_SQ
        val thetaSq = DeponderDefaults.Simulation.BARNES_HUT_THETA * DeponderDefaults.Simulation.BARNES_HUT_THETA
        val chunkSize = (n + chunks - 1) / chunks

        var lastMaxSpeedSq = 0f
        var lastMaxAccelSq = 0f

        for (sub in 0 until subSteps) {
            val alphaUnfold = computeUnfoldAlpha(curUnfoldRem)
            if (curUnfoldRem > 0f) {
                curUnfoldRem = max(0f, curUnfoldRem - subDt)
            }
            val repScale = if (alphaUnfold > 0f) 1f - UNFOLD_REPULSION_ATTENUATION * alphaUnfold else 1f
            val effectiveDamping = max(0f, env.airDamping) + UNFOLD_EXTRA_DAMPING * alphaUnfold
            val dampFactor = exp(-effectiveDamping * subDt)

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
                val guideFx: Float
                val guideFy: Float
                if (alphaUnfold > 0f) {
                    val guideScale = massArr[i] * UNFOLD_GUIDE_OMEGA_SQ * alphaUnfold
                    guideFx = (targetX[i] - stepX[i]) * guideScale
                    guideFy = (targetY[i] - stepY[i]) * guideScale
                } else {
                    guideFx = 0f
                    guideFy = 0f
                }
                val totalFx = wallF.x + edgeFx[i] + pairFx[i] * repScale + guideFx
                val totalFy = wallF.y + edgeFy[i] + pairFy[i] * repScale + guideFy
                val ax = totalFx * invMass[i]
                val ay = totalFy * invMass[i]

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

            if (sub == subSteps - 1) {
                lastMaxSpeedSq = maxSpeedSq
                lastMaxAccelSq = maxAccelSq
            }
        }

        return Tier3StepMetrics(
            maxSpeedSq = lastMaxSpeedSq,
            maxAccelSq = lastMaxAccelSq,
            finalUnfoldRem = curUnfoldRem,
            targetX = targetX,
            targetY = targetY,
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
                rawR * clampedScale
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
    ): Vec2 {
        prepareSnapshotAndSprings(n, clampedScale)

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

        return integrateSubstep(n, subDt, clampedScale, env)
    }

    private fun integrateSubstep(
        n: Int,
        subDt: Float,
        clampedScale: Float,
        env: EnvironmentSpec,
    ): Vec2 {
        val alphaUnfold = computeUnfoldAlpha(unfoldRemainingSeconds)
        if (unfoldRemainingSeconds > 0f) {
            unfoldRemainingSeconds = max(0f, unfoldRemainingSeconds - subDt)
        }
        val repScale = if (alphaUnfold > 0f) 1f - UNFOLD_REPULSION_ATTENUATION * alphaUnfold else 1f
        val effectiveDamping = max(0f, env.airDamping) + UNFOLD_EXTRA_DAMPING * alphaUnfold
        val dampFactor = exp(-effectiveDamping * subDt)

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

            val guideFx: Float
            val guideFy: Float
            if (alphaUnfold > 0f) {
                val guideScale = b.spec.mass * UNFOLD_GUIDE_OMEGA_SQ * alphaUnfold
                guideFx = (unfoldTargetX[i] - snapPx[i]) * guideScale
                guideFy = (unfoldTargetY[i] - snapPy[i]) * guideScale
            } else {
                guideFx = 0f
                guideFy = 0f
            }
            val totalFx = wallF.x + accumEdgeFx[i] + outPairFx[i] * repScale + guideFx
            val totalFy = wallF.y + accumEdgeFy[i] + outPairFy[i] * repScale + guideFy
            val invMass = 1f / max(1e-4f, b.spec.mass)

            val ax = totalFx * invMass
            val ay = totalFy * invMass

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

        return Vec2(maxSpeedSq, maxAccelSq)
    }

    public companion object {
        private const val RAD_TO_DEG: Double = 180.0 / PI
        private const val GOLDEN_ANGLE_RAD: Double = 2.399963229728653
        private const val ROUNDED_CONTOUR_RATIO: Float = 0.35f
        private const val COLLINEAR_BAND_PX: Float = 6.0f
        private const val COLLINEAR_NUDGE_PX: Float = 3.5f
        private const val UNFOLD_DURATION_SECONDS: Float = 0.85f
        private const val UNFOLD_GUIDE_OMEGA_SQ: Float = 110.0f
        private const val UNFOLD_EXTRA_DAMPING: Float = 16.0f
        private const val UNFOLD_REPULSION_ATTENUATION: Float = 0.85f

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
