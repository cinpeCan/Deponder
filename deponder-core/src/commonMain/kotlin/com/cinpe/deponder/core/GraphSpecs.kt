package com.cinpe.deponder.core

import kotlin.jvm.JvmOverloads
import kotlin.math.max

private fun normalizeSpecZero(value: Float): Float = if (value == 0f) 0f else value

/**
 * Resolved pixel-space environment configuration consumed by [SimulationWorld].
 *
 * All distance fields are already scaled to target viewport pixels (`dp * density`).
 * Pass a negative [wallRepulsionPx] (such as [DeponderDefaults.INFINITE_RANGE]) for unbounded centering walls.
 */
@Suppress("UNUSED_PARAMETER")
public class EnvironmentSpec internal constructor(
    initScale: Float,
    maxScale: Float,
    minScale: Float,
    airDamping: Float,
    wallRepulsionPx: Float,
    wallElasticityStart: Float,
    wallElasticityTop: Float,
    wallElasticityEnd: Float,
    wallElasticityBottom: Float,
    degreeRadiusIncrementPx: Float,
    infiniteChargeStrength: Float,
    coolingEnabled: Boolean,
    alphaDecay: Float,
    alphaMin: Float,
    restSpeedPxPerSec: Float,
    restAccelPxPerSecSq: Float,
    settleDurationSeconds: Float,
    @Deprecated(
        message = "Topological unfolding is a transient one-shot action rather than a persistent environment property; invoke SimulationWorld.unfold() instead.",
    )
    public val unfoldEnabled: Boolean,
) {
    public val initScale: Float = normalizeSpecZero(initScale)
    public val maxScale: Float = normalizeSpecZero(maxScale)
    public val minScale: Float = normalizeSpecZero(minScale)
    public val airDamping: Float = normalizeSpecZero(airDamping)
    public val wallRepulsionPx: Float = normalizeSpecZero(wallRepulsionPx)
    public val wallElasticityStart: Float = normalizeSpecZero(wallElasticityStart)
    public val wallElasticityTop: Float = normalizeSpecZero(wallElasticityTop)
    public val wallElasticityEnd: Float = normalizeSpecZero(wallElasticityEnd)
    public val wallElasticityBottom: Float = normalizeSpecZero(wallElasticityBottom)

    /**
     * Deprecated (no-op `0f` since 0.6.1): implicit per-edge repulsion radius inflation has been
     * retired so repulsion strictly honors [PlanetSpec.repulsionRadiusPx].
     */
    @Deprecated(
        message = "No-op since 0.6.1: always 0f. Implicit degree-based radius inflation has been retired; configure PlanetPhysics.repulsionRadiusDp explicitly.",
    )
    public val degreeRadiusIncrementPx: Float = 0f

    public val infiniteChargeStrength: Float = normalizeSpecZero(infiniteChargeStrength)

    /**
     * Deprecated (no-op `false` since 0.6.1): simulated annealing has been retired in favor of
     * pure Newtonian viscous drag ([airDamping]).
     */
    @Deprecated(
        message = "No-op since 0.6.1: always false. Simulated annealing has been retired; configure airDamping instead.",
    )
    public val coolingEnabled: Boolean = false

    /**
     * Deprecated (no-op `0f` since 0.6.1): simulated annealing (`alpha` decay) has been retired in
     * favor of pure Newtonian viscous drag ([airDamping]).
     */
    @Deprecated(
        message = "No-op since 0.6.1: always 0f. Simulated annealing has been retired; configure airDamping instead.",
    )
    public val alphaDecay: Float = 0f

    /**
     * Deprecated (no-op `0f` since 0.6.1): simulated annealing (`alphaMin` gate) has been retired in
     * favor of Global Kinetic-Force Sleep (`restSpeedPxPerSec` + `restAccelPxPerSecSq`).
     */
    @Deprecated(
        message = "No-op since 0.6.1: always 0f. Simulated annealing has been retired; settling uses kinetic/acceleration thresholds instead.",
    )
    public val alphaMin: Float = 0f

    public val restSpeedPxPerSec: Float = normalizeSpecZero(restSpeedPxPerSec)
    public val restAccelPxPerSecSq: Float = normalizeSpecZero(restAccelPxPerSecSq)
    public val settleDurationSeconds: Float = normalizeSpecZero(settleDurationSeconds)

    @Suppress("DEPRECATION")
    public constructor() : this(
        initScale = DeponderDefaults.Root.INIT_SCALE,
        maxScale = DeponderDefaults.Root.MAX_SCALE,
        minScale = DeponderDefaults.Root.MIN_SCALE,
        airDamping = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionPx = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
        degreeRadiusIncrementPx = 0f,
        infiniteChargeStrength = DeponderDefaults.Planet.INFINITE_CHARGE_STRENGTH,
        coolingEnabled = false,
        alphaDecay = 0f,
        alphaMin = 0f,
        restSpeedPxPerSec = DeponderDefaults.Simulation.REST_SPEED_PX_PER_SEC,
        restAccelPxPerSecSq = DeponderDefaults.Simulation.REST_ACCEL_PX_PER_SEC_SQ,
        settleDurationSeconds = DeponderDefaults.Simulation.SETTLE_DURATION_SECONDS,
        unfoldEnabled = DeponderDefaults.Root.UNFOLD_ENABLED,
    )

    @Suppress("DEPRECATION")
    public constructor(
        initScale: Float = DeponderDefaults.Root.INIT_SCALE,
        maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
        minScale: Float = DeponderDefaults.Root.MIN_SCALE,
        airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionPx: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
        degreeRadiusIncrementPx: Float = DeponderDefaults.Planet.DEGREE_RADIUS_INCREMENT_DP,
        infiniteChargeStrength: Float = DeponderDefaults.Planet.INFINITE_CHARGE_STRENGTH,
        coolingEnabled: Boolean = DeponderDefaults.Root.COOLING_ENABLED,
        alphaDecay: Float = DeponderDefaults.Simulation.ALPHA_DECAY,
        alphaMin: Float = DeponderDefaults.Simulation.ALPHA_MIN,
        restSpeedPxPerSec: Float = DeponderDefaults.Simulation.REST_SPEED_PX_PER_SEC,
        restAccelPxPerSecSq: Float = DeponderDefaults.Simulation.REST_ACCEL_PX_PER_SEC_SQ,
        settleDurationSeconds: Float = DeponderDefaults.Simulation.SETTLE_DURATION_SECONDS,
    ) : this(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionPx = wallRepulsionPx,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        degreeRadiusIncrementPx = degreeRadiusIncrementPx,
        infiniteChargeStrength = infiniteChargeStrength,
        coolingEnabled = coolingEnabled,
        alphaDecay = alphaDecay,
        alphaMin = alphaMin,
        restSpeedPxPerSec = restSpeedPxPerSec,
        restAccelPxPerSecSq = restAccelPxPerSecSq,
        settleDurationSeconds = settleDurationSeconds,
        unfoldEnabled = DeponderDefaults.Root.UNFOLD_ENABLED,
    )

    public operator fun component1(): Float = initScale
    public operator fun component2(): Float = maxScale
    public operator fun component3(): Float = minScale
    public operator fun component4(): Float = airDamping
    public operator fun component5(): Float = wallRepulsionPx
    public operator fun component6(): Float = wallElasticityStart
    public operator fun component7(): Float = wallElasticityTop
    public operator fun component8(): Float = wallElasticityEnd
    public operator fun component9(): Float = wallElasticityBottom

    @Deprecated(message = "No-op since 0.6.1: degreeRadiusIncrementPx has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component10(): Float = degreeRadiusIncrementPx

    public operator fun component11(): Float = infiniteChargeStrength

    @Deprecated(message = "No-op since 0.6.1: coolingEnabled has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component12(): Boolean = coolingEnabled

    @Deprecated(message = "No-op since 0.6.1: alphaDecay has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component13(): Float = alphaDecay

    @Deprecated(message = "No-op since 0.6.1: alphaMin has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component14(): Float = alphaMin

    public operator fun component15(): Float = restSpeedPxPerSec
    public operator fun component16(): Float = restAccelPxPerSecSq
    public operator fun component17(): Float = settleDurationSeconds

    @Suppress("DEPRECATION")
    public fun copy(
        initScale: Float = this.initScale,
        maxScale: Float = this.maxScale,
        minScale: Float = this.minScale,
        airDamping: Float = this.airDamping,
        wallRepulsionPx: Float = this.wallRepulsionPx,
        wallElasticityStart: Float = this.wallElasticityStart,
        wallElasticityTop: Float = this.wallElasticityTop,
        wallElasticityEnd: Float = this.wallElasticityEnd,
        wallElasticityBottom: Float = this.wallElasticityBottom,
        degreeRadiusIncrementPx: Float = this.degreeRadiusIncrementPx,
        infiniteChargeStrength: Float = this.infiniteChargeStrength,
        coolingEnabled: Boolean = this.coolingEnabled,
        alphaDecay: Float = this.alphaDecay,
        alphaMin: Float = this.alphaMin,
        restSpeedPxPerSec: Float = this.restSpeedPxPerSec,
        restAccelPxPerSecSq: Float = this.restAccelPxPerSecSq,
        settleDurationSeconds: Float = this.settleDurationSeconds,
    ): EnvironmentSpec = EnvironmentSpec(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionPx = wallRepulsionPx,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        degreeRadiusIncrementPx = degreeRadiusIncrementPx,
        infiniteChargeStrength = infiniteChargeStrength,
        coolingEnabled = coolingEnabled,
        alphaDecay = alphaDecay,
        alphaMin = alphaMin,
        restSpeedPxPerSec = restSpeedPxPerSec,
        restAccelPxPerSecSq = restAccelPxPerSecSq,
        settleDurationSeconds = settleDurationSeconds,
        unfoldEnabled = this.unfoldEnabled,
    )

    @Deprecated(
        message = "Topological unfolding is a transient one-shot action rather than a persistent environment property; invoke SimulationWorld.unfold() instead.",
    )
    @JvmOverloads
    @Suppress("DEPRECATION")
    public fun withUnfold(unfoldEnabled: Boolean = true): EnvironmentSpec = EnvironmentSpec(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionPx = wallRepulsionPx,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        degreeRadiusIncrementPx = 0f,
        infiniteChargeStrength = infiniteChargeStrength,
        coolingEnabled = false,
        alphaDecay = 0f,
        alphaMin = 0f,
        restSpeedPxPerSec = restSpeedPxPerSec,
        restAccelPxPerSecSq = restAccelPxPerSecSq,
        settleDurationSeconds = settleDurationSeconds,
        unfoldEnabled = unfoldEnabled,
    )

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnvironmentSpec) return false
        return initScale == other.initScale &&
            maxScale == other.maxScale &&
            minScale == other.minScale &&
            airDamping == other.airDamping &&
            wallRepulsionPx == other.wallRepulsionPx &&
            wallElasticityStart == other.wallElasticityStart &&
            wallElasticityTop == other.wallElasticityTop &&
            wallElasticityEnd == other.wallElasticityEnd &&
            wallElasticityBottom == other.wallElasticityBottom &&
            degreeRadiusIncrementPx == other.degreeRadiusIncrementPx &&
            infiniteChargeStrength == other.infiniteChargeStrength &&
            coolingEnabled == other.coolingEnabled &&
            alphaDecay == other.alphaDecay &&
            alphaMin == other.alphaMin &&
            restSpeedPxPerSec == other.restSpeedPxPerSec &&
            restAccelPxPerSecSq == other.restAccelPxPerSecSq &&
            settleDurationSeconds == other.settleDurationSeconds &&
            unfoldEnabled == other.unfoldEnabled
    }

    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = initScale.hashCode()
        result = 31 * result + maxScale.hashCode()
        result = 31 * result + minScale.hashCode()
        result = 31 * result + airDamping.hashCode()
        result = 31 * result + wallRepulsionPx.hashCode()
        result = 31 * result + wallElasticityStart.hashCode()
        result = 31 * result + wallElasticityTop.hashCode()
        result = 31 * result + wallElasticityEnd.hashCode()
        result = 31 * result + wallElasticityBottom.hashCode()
        result = 31 * result + degreeRadiusIncrementPx.hashCode()
        result = 31 * result + infiniteChargeStrength.hashCode()
        result = 31 * result + coolingEnabled.hashCode()
        result = 31 * result + alphaDecay.hashCode()
        result = 31 * result + alphaMin.hashCode()
        result = 31 * result + restSpeedPxPerSec.hashCode()
        result = 31 * result + restAccelPxPerSecSq.hashCode()
        result = 31 * result + settleDurationSeconds.hashCode()
        result = 31 * result + unfoldEnabled.hashCode()
        return result
    }

    @Suppress("DEPRECATION")
    override fun toString(): String =
        "EnvironmentSpec(initScale=$initScale, maxScale=$maxScale, minScale=$minScale, " +
            "airDamping=$airDamping, wallRepulsionPx=$wallRepulsionPx, " +
            "wallElasticityStart=$wallElasticityStart, wallElasticityTop=$wallElasticityTop, " +
            "wallElasticityEnd=$wallElasticityEnd, wallElasticityBottom=$wallElasticityBottom, " +
            "degreeRadiusIncrementPx=$degreeRadiusIncrementPx, " +
            "infiniteChargeStrength=$infiniteChargeStrength, coolingEnabled=$coolingEnabled, " +
            "alphaDecay=$alphaDecay, alphaMin=$alphaMin, restSpeedPxPerSec=$restSpeedPxPerSec, " +
            "restAccelPxPerSecSq=$restAccelPxPerSecSq, " +
            "settleDurationSeconds=$settleDurationSeconds, unfoldEnabled=$unfoldEnabled)"
}

/**
 * Pure point-mass node specification in resolved pixel units.
 *
 * Pass a negative [repulsionRadiusPx] (such as [DeponderDefaults.INFINITE_RANGE]) for an unbounded
 * inverse-square electrostatic field.
 */
public data class PlanetSpec(
    public val id: String,
    public val mass: Float = DeponderDefaults.Planet.MASS,
    public val repulsionRadiusPx: Float = DeponderDefaults.Planet.REPULSION_RADIUS_DP,
    public val elasticity: Float = DeponderDefaults.Planet.ELASTICITY,
    public val pinned: Boolean = false,
)

/**
 * Pure undirected Hooke spring link specification between [startId] and [endId] in resolved pixel units.
 *
 * Physics forces act symmetrically between the two point-mass centers, while [startId] -> [endId]
 * preserves the caller's visual orientation on [RubberPose].
 */
public data class RubberSpec(
    public val startId: String,
    public val endId: String,
    public val elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
    public val naturalLengthPx: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
) {
    public val key: EdgeKey = EdgeKey(startId, endId)
}

/**
 * Mutable runtime state of a single point-mass planet inside [SimulationWorld].
 */
public class PlanetBody internal constructor(
    initialSpec: PlanetSpec,
    x: Float,
    y: Float,
) {
    private var _spec: PlanetSpec = initialSpec

    /**
     * Current point-mass configuration for this planet.
     *
     * Reading [spec] returns the active configuration inside [SimulationWorld]; the public setter
     * is `@Deprecated` and a strict no-op since `0.6.1` to prevent out-of-band mutations from
     * bypassing topology validation and spatial indexing. Update node physics via
     * `Modifier.deponderPlanet`, `view.deponderPlanet`, or [SimulationWorld.upsertPlanet] instead.
     */
    public var spec: PlanetSpec
        get() = _spec
        @Deprecated(
            message = "No-op since 0.6.1: direct mutation of PlanetBody.spec bypasses topology validation and is ignored. Update node physics via Modifier.deponderPlanet, view.deponderPlanet, or SimulationWorld.upsertPlanet(...) instead.",
        )
        set(_) {
            // Intentional no-op since 0.6.1 to preserve 0.6.0 JVM/Klib binary compatibility without mutating engine invariants.
        }

    internal fun updateSpecInternal(newSpec: PlanetSpec) {
        _spec = newSpec
    }

    public var x: Float = x
        internal set
    public var y: Float = y
        internal set
    public var vx: Float = 0f
        internal set
    public var vy: Float = 0f
        internal set
    public var dx: Float = 0f
        internal set
    public var dy: Float = 0f
        internal set
    public var halfWidthPx: Float = 0f
        internal set
    public var halfHeightPx: Float = 0f
        internal set
    public var isPressed: Boolean = false
        internal set
    public var degree: Int = 0
        internal set
    internal var hasEstablishedPose: Boolean = false
    internal var hasUserPose: Boolean = false
}

/**
 * Raw center-to-center link geometry and real-time directional contour insets on the current frame.
 *
 * [centerX], [centerY], and [centerDistance] always represent the unclipped Edge-to-Edge span
 * between `startId` and `endId` centers, while [startContourPx] and [endContourPx] report each
 * endpoint planet's scaled directional contour radius along `(` [directionX] `,` [directionY] `)`.
 * Call [resolvedLength], [resolvedCenterX], and [resolvedCenterY] to evaluate safe-area geometry
 * for a given [DeponderRubberInsets] without heap allocations.
 */
public class RubberPose internal constructor(
    initialSpec: RubberSpec,
) {
    private var _spec: RubberSpec = initialSpec

    /**
     * Current spring link configuration for this rubber edge.
     *
     * Reading [spec] returns the active specification inside [SimulationWorld]; the public setter is
     * `@Deprecated` and a strict no-op since `0.6.1` so direct writes cannot desynchronize spring
     * topology indices or wake scheduling. Update link physics via `Modifier.deponderRubber`,
     * `view.deponderRubber`, or [SimulationWorld.upsertRubber] instead.
     */
    public var spec: RubberSpec
        get() = _spec
        @Deprecated(
            message = "No-op since 0.6.1: direct mutation of RubberPose.spec bypasses spring topology indexing and is ignored. Update link physics via Modifier.deponderRubber, view.deponderRubber, or SimulationWorld.upsertRubber(...) instead.",
        )
        set(_) {
            // Intentional no-op since 0.6.1 to preserve 0.6.0 JVM/Klib binary compatibility without mutating engine invariants.
        }

    internal fun updateSpecInternal(newSpec: RubberSpec) {
        _spec = newSpec
    }

    public var centerX: Float = 0f
        internal set
    public var centerY: Float = 0f
        internal set
    public var centerDistance: Float = 0f
        internal set
    public var directionX: Float = 0f
        internal set
    public var directionY: Float = 0f
        internal set
    public var startContourPx: Float = 0f
        internal set
    public var endContourPx: Float = 0f
        internal set
    public var rotationDeg: Float = 0f
        internal set
    public var scale: Float = 1f
        internal set
    public var isActive: Boolean = false
        internal set
    public var dirty: Boolean = true
        internal set

    /**
     * Computes the total pixel inset at the `startId` endpoint for [insets] and [density] (`px / dp`).
     */
    public fun resolvedStartInsetPx(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return 0f
        val safeDensity = if (density.isFinite()) max(1e-3f, density) else 1f
        return when (insets.startMode) {
            DeponderRubberInsets.Mode.NONE -> 0f
            DeponderRubberInsets.Mode.CONTOUR -> {
                if (startContourPx > 0f) {
                    max(0f, startContourPx + insets.startValueDp * safeDensity * scale)
                } else {
                    0f
                }
            }
            DeponderRubberInsets.Mode.FIXED -> max(0f, insets.startValueDp) * safeDensity * scale
        }
    }

    /**
     * Computes the total pixel inset at the `endId` endpoint for [insets] and [density] (`px / dp`).
     */
    public fun resolvedEndInsetPx(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return 0f
        val safeDensity = if (density.isFinite()) max(1e-3f, density) else 1f
        return when (insets.endMode) {
            DeponderRubberInsets.Mode.NONE -> 0f
            DeponderRubberInsets.Mode.CONTOUR -> {
                if (endContourPx > 0f) {
                    max(0f, endContourPx + insets.endValueDp * safeDensity * scale)
                } else {
                    0f
                }
            }
            DeponderRubberInsets.Mode.FIXED -> max(0f, insets.endValueDp) * safeDensity * scale
        }
    }

    /**
     * Evaluates the active pixel span length after applying [insets] at [density] (`px / dp`).
     * Returns `0f` when the combined start and end insets meet or exceed [centerDistance].
     */
    public fun resolvedLength(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return 0f
        val sInset = resolvedStartInsetPx(insets, density)
        val eInset = resolvedEndInsetPx(insets, density)
        return max(0f, centerDistance - sInset - eInset)
    }

    /**
     * Signed axial center shift along the link's local X-axis relative to `(` [centerX] `,` [centerY] `)`,
     * proportionally clamped when combined insets exceed [centerDistance] so the resolved midpoint
     * always stays within the `[startId, endId]` segment.
     */
    public fun localSpanOffsetX(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return 0f
        val sInset = resolvedStartInsetPx(insets, density)
        val eInset = resolvedEndInsetPx(insets, density)
        val totalInset = sInset + eInset
        if (totalInset <= 0f) return 0f
        val clampRatio = if (totalInset > centerDistance) centerDistance / totalInset else 1f
        return (sInset - eInset) * clampRatio * 0.5f
    }

    /**
     * Evaluates the resolved midpoint X in root pixel coordinates for [insets] at [density] (`px / dp`).
     */
    public fun resolvedCenterX(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return centerX
        return centerX + directionX * localSpanOffsetX(insets, density)
    }

    /**
     * Evaluates the resolved midpoint Y in root pixel coordinates for [insets] at [density] (`px / dp`).
     */
    public fun resolvedCenterY(insets: DeponderRubberInsets, density: Float): Float {
        if (!isActive || centerDistance <= 1e-4f) return centerY
        return centerY + directionY * localSpanOffsetX(insets, density)
    }
}
