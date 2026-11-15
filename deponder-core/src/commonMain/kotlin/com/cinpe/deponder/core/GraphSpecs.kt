package com.cinpe.deponder.core

import kotlin.math.max

/**
 * Resolved pixel-space environment configuration consumed by [SimulationWorld].
 *
 * All distance fields are already scaled to target viewport pixels (`dp * density`).
 * Pass a negative [wallRepulsionPx] (such as [DeponderDefaults.INFINITE_RANGE]) for unbounded centering walls.
 */
public data class EnvironmentSpec(
    public val initScale: Float = DeponderDefaults.Root.INIT_SCALE,
    public val maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
    public val minScale: Float = DeponderDefaults.Root.MIN_SCALE,
    public val airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
    public val wallRepulsionPx: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
    public val wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
    public val wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
    public val wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
    public val wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
    public val degreeRadiusIncrementPx: Float = DeponderDefaults.Planet.DEGREE_RADIUS_INCREMENT_DP,
    public val infiniteChargeStrength: Float = DeponderDefaults.Planet.INFINITE_CHARGE_STRENGTH,
    public val coolingEnabled: Boolean = DeponderDefaults.Root.COOLING_ENABLED,
    public val alphaDecay: Float = DeponderDefaults.Simulation.ALPHA_DECAY,
    public val alphaMin: Float = DeponderDefaults.Simulation.ALPHA_MIN,
    public val restSpeedPxPerSec: Float = DeponderDefaults.Simulation.REST_SPEED_PX_PER_SEC,
    public val restAccelPxPerSecSq: Float = DeponderDefaults.Simulation.REST_ACCEL_PX_PER_SEC_SQ,
    public val settleDurationSeconds: Float = DeponderDefaults.Simulation.SETTLE_DURATION_SECONDS,
)

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
    public var spec: PlanetSpec,
    x: Float,
    y: Float,
) {
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
    public var spec: RubberSpec,
) {
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
        val safeDensity = max(1e-3f, density)
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
        val safeDensity = max(1e-3f, density)
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
