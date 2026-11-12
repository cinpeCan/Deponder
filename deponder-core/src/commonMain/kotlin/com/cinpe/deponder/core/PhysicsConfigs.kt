package com.cinpe.deponder.core

import kotlin.jvm.JvmStatic
import kotlin.math.max

/**
 * Directional arrowhead rendering mode for built-in default rubber components
 * (`DeponderDefaultRubber` in Compose and `DeponderRubberView` in Android Views).
 */
public enum class DeponderArrowMode {
    /** Plain spring line without arrowheads. */
    NONE,

    /** Single arrowhead pointing toward `endId` (`startId -> endId`). */
    END,

    /** Single arrowhead pointing toward `startId` (`startId <- endId`). */
    START,

    /** Bidirectional arrowheads pointing toward both `startId` and `endId` (`startId <-> endId`). */
    BOTH,
}

/**
 * Shared, immutable `dp`-based root container physics configuration used across both
 * Compose Multiplatform (`rememberDeponderState`) and Android Views (`viewGroup.deponderRoot`).
 *
 * Pass a negative [wallRepulsionDp] (such as [DeponderDefaults.INFINITE_RANGE]) for an unbounded
 * viewport-centering boundary field.
 */
public data class RootPhysics(
    public val initScale: Float = DeponderDefaults.Root.INIT_SCALE,
    public val maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
    public val minScale: Float = DeponderDefaults.Root.MIN_SCALE,
    public val airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
    public val wallRepulsionDp: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
    public val wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
    public val wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
    public val wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
    public val wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
    public val coolingEnabled: Boolean = DeponderDefaults.Root.COOLING_ENABLED,
    public val alphaDecay: Float = DeponderDefaults.Simulation.ALPHA_DECAY,
) {
    init {
        require(minScale > 0f) { "minScale must be > 0f, but was $minScale" }
        require(maxScale >= minScale) { "maxScale ($maxScale) must be >= minScale ($minScale)" }
    }

    public fun withScaleBounds(initScale: Float, minScale: Float, maxScale: Float): RootPhysics =
        copy(initScale = initScale, minScale = minScale, maxScale = maxScale)

    public fun withAirDamping(airDamping: Float): RootPhysics =
        copy(airDamping = airDamping)

    public fun withWallRepulsionDp(wallRepulsionDp: Float): RootPhysics =
        copy(wallRepulsionDp = wallRepulsionDp)

    public fun withCooling(coolingEnabled: Boolean, alphaDecay: Float = this.alphaDecay): RootPhysics =
        copy(coolingEnabled = coolingEnabled, alphaDecay = alphaDecay)

    /**
     * Converts this `dp`-based configuration into pixel-space [EnvironmentSpec] for [density] (`px / dp`).
     */
    public fun toEnvironmentSpec(density: Float): EnvironmentSpec {
        val safeDensity = max(1e-3f, density)
        val clampedInit = initScale.coerceIn(minScale, maxScale)
        return EnvironmentSpec(
            initScale = clampedInit,
            maxScale = maxScale,
            minScale = minScale,
            airDamping = airDamping,
            wallRepulsionPx = if (wallRepulsionDp < 0f) {
                DeponderDefaults.INFINITE_RANGE
            } else {
                wallRepulsionDp * safeDensity
            },
            wallElasticityStart = wallElasticityStart,
            wallElasticityTop = wallElasticityTop,
            wallElasticityEnd = wallElasticityEnd,
            wallElasticityBottom = wallElasticityBottom,
            degreeRadiusIncrementPx = DeponderDefaults.Planet.DEGREE_RADIUS_INCREMENT_DP * safeDensity,
            coolingEnabled = coolingEnabled,
            alphaDecay = alphaDecay,
        )
    }

    public companion object {
        @JvmStatic
        public fun fromPreset(preset: DeponderDefaults.Preset): RootPhysics =
            RootPhysics(
                airDamping = preset.airDamping,
                wallRepulsionDp = preset.wallRepulsionDp,
                alphaDecay = preset.alphaDecay,
            )
    }
}

/**
 * Shared, immutable `dp`-based point-mass node (`Planet`) configuration used across both
 * Compose Multiplatform (`Modifier.deponderPlanet`) and Android Views (`view.deponderPlanet`).
 *
 * Pass [repulsionRadiusDp] = [DeponderDefaults.INFINITE_RANGE] (`-1f`) for an unbounded electrostatic field.
 */
public data class PlanetPhysics(
    public val mass: Float = DeponderDefaults.Planet.MASS,
    public val repulsionRadiusDp: Float = DeponderDefaults.Planet.REPULSION_RADIUS_DP,
    public val elasticity: Float = DeponderDefaults.Planet.ELASTICITY,
    public val pinned: Boolean = false,
) {
    init {
        require(mass > 0f) { "PlanetPhysics.mass must be > 0f, but was $mass" }
    }

    public fun withMass(mass: Float): PlanetPhysics = copy(mass = mass)

    public fun withRepulsionRadiusDp(repulsionRadiusDp: Float): PlanetPhysics =
        copy(repulsionRadiusDp = repulsionRadiusDp)

    public fun withElasticity(elasticity: Float): PlanetPhysics = copy(elasticity = elasticity)

    public fun withPinned(pinned: Boolean): PlanetPhysics = copy(pinned = pinned)

    /**
     * Converts this `dp`-based configuration into pixel-space [PlanetSpec] for [id] and [density].
     */
    public fun toPlanetSpec(id: String, density: Float): PlanetSpec {
        val safeDensity = max(1e-3f, density)
        return PlanetSpec(
            id = id,
            mass = mass,
            repulsionRadiusPx = if (repulsionRadiusDp < 0f) {
                DeponderDefaults.INFINITE_RANGE
            } else {
                repulsionRadiusDp * safeDensity
            },
            elasticity = elasticity,
            pinned = pinned,
        )
    }

    public companion object {
        @JvmStatic
        public fun fromPreset(preset: DeponderDefaults.Preset): PlanetPhysics =
            fromPreset(preset = preset, pinned = false)

        @JvmStatic
        public fun fromPreset(
            preset: DeponderDefaults.Preset,
            pinned: Boolean,
        ): PlanetPhysics = PlanetPhysics(
            repulsionRadiusDp = preset.planetRepulsionDp,
            elasticity = preset.planetElasticity,
            pinned = pinned,
        )
    }
}

/**
 * Shared, immutable `dp`-based undirected spring link (`Rubber`) configuration used across both
 * Compose Multiplatform (`Modifier.deponderRubber`) and Android Views (`view.deponderRubber`).
 *
 * @property elasticity Hooke spring stiffness (`>= 0f` typical).
 * @property naturalLengthDp Rest center-to-center spring length in `dp` (`>= 0f`).
 * @property endpointGapDp Visual breathing clearance outside each endpoint planet's contour in `dp` (`>= 0f`).
 */
public data class RubberPhysics(
    public val elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
    public val naturalLengthDp: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
    public val endpointGapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
) {
    init {
        require(naturalLengthDp >= 0f) {
            "RubberPhysics.naturalLengthDp must be >= 0f, but was $naturalLengthDp"
        }
        require(endpointGapDp >= 0f) {
            "RubberPhysics.endpointGapDp must be >= 0f, but was $endpointGapDp"
        }
    }

    public fun withElasticity(elasticity: Float): RubberPhysics = copy(elasticity = elasticity)

    public fun withNaturalLengthDp(naturalLengthDp: Float): RubberPhysics =
        copy(naturalLengthDp = naturalLengthDp)

    public fun withEndpointGapDp(endpointGapDp: Float): RubberPhysics =
        copy(endpointGapDp = endpointGapDp)

    /**
     * Converts this `dp`-based configuration into pixel-space [RubberSpec] for [startId], [endId], and [density].
     */
    public fun toRubberSpec(startId: String, endId: String, density: Float): RubberSpec {
        val safeDensity = max(1e-3f, density)
        return RubberSpec(
            startId = startId,
            endId = endId,
            elasticity = elasticity,
            naturalLengthPx = naturalLengthDp * safeDensity,
            endpointGapPx = endpointGapDp * safeDensity,
        )
    }

    public companion object {
        @JvmStatic
        public fun fromPreset(preset: DeponderDefaults.Preset): RubberPhysics =
            RubberPhysics(
                elasticity = preset.rubberElasticity,
                naturalLengthDp = preset.rubberNaturalLengthDp,
            )
    }
}
