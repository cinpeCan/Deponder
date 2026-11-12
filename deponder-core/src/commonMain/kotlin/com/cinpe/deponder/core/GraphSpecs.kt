package com.cinpe.deponder.core

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
 * Pure undirected spring link specification between [startId] and [endId] in resolved pixel units.
 *
 * Physics forces act symmetrically between the two point-mass centers, while [startId] -> [endId]
 * preserves the caller's visual orientation and [endpointGapPx] trims visual endpoints cleanly outside
 * each planet's measured contour.
 */
public data class RubberSpec(
    public val startId: String,
    public val endId: String,
    public val elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
    public val naturalLengthPx: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
    public val endpointGapPx: Float = 0f,
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
 * Computed visual geometry for a single rubber link on the current frame.
 *
 * [centerX], [centerY], and [length] represent the visible span between the outer contours of
 * `startId` and `endId` (after subtracting directional planet radii + [RubberSpec.endpointGapPx]),
 * while [centerDistance] exposes the raw center-to-center distance.
 */
public class RubberPose internal constructor(
    public var spec: RubberSpec,
) {
    public var centerX: Float = 0f
        internal set
    public var centerY: Float = 0f
        internal set
    public var length: Float = 0f
        internal set
    public var centerDistance: Float = 0f
        internal set
    public var rotationDeg: Float = 0f
        internal set
    public var scale: Float = 1f
        internal set
    public var isActive: Boolean = false
        internal set
    public var dirty: Boolean = true
        internal set
}
