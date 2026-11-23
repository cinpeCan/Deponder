package com.cinpe.deponder.core

/**
 * Hierarchical, domain-scoped defaults and semantic constants for Deponder.
 *
 * Spatial distance defaults are expressed in density-independent pixels (`dp`) so physical
 * proportions remain consistent across Android DPI buckets, desktop monitors, iOS Retina displays,
 * and web viewports.
 */
public object DeponderDefaults {

    /**
     * Sentinel range value indicating an unbounded force field:
     * - On root walls: applies continuous linear centering forces across the entire viewport.
     * - On planets: switches from finite Hooke repulsion to an unbounded inverse-square field.
     */
    public const val INFINITE_RANGE: Float = -1f

    /**
     * Sentinel spring length value indicating content-adaptive natural length on [RubberPhysics]:
     * resolves rest center-to-center length from the rubber's measured non-stretched content width
     * plus [Rubber.CONTENT_PADDING_DP], clamped within `[minLengthDp, maxLengthDp]`.
     */
    public const val AUTO_LENGTH: Float = -1f

    /**
     * Default environment (container viewport) parameters.
     */
    public object Root {
        public const val INIT_SCALE: Float = 1.0f
        public const val MAX_SCALE: Float = 1.5f
        public const val MIN_SCALE: Float = 0.5f

        /**
         * Exponential viscous air damping rate (in `1/s`), calibrated to a ~0.48s velocity half-life
         * (`zeta ~ 0.5 - 0.75` medium-bouncy spring feel: 1 crisp elastic rebound before smooth settling).
         */
        public const val AIR_DAMPING: Float = 1.45f

        /**
         * Default wall repulsion sensing range from each container edge, in `dp` (`~300px` at 2.75x).
         * Set to [INFINITE_RANGE] (`-1f`) to enable full-canvas centering force.
         */
        public const val WALL_REPULSION_DP: Float = 110f

        public const val WALL_ELASTICITY_START: Float = 1.44f
        public const val WALL_ELASTICITY_TOP: Float = 1.44f
        public const val WALL_ELASTICITY_END: Float = 1.44f
        public const val WALL_ELASTICITY_BOTTOM: Float = 1.44f

        /**
         * Deprecated (no-op since 0.6.1): simulated annealing (`alpha` cooling) has been retired so
         * Hooke springs preserve pure Newtonian elasticity governed solely by [AIR_DAMPING] (`0f`
         * for undamped perpetual motion). Setting `coolingEnabled` has no effect.
         */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing has been retired in favor of pure Newtonian mechanics; configure RootPhysics(airDamping = ...) instead (0f for perpetual motion).",
        )
        public const val COOLING_ENABLED: Boolean = false

        /**
         * Deprecated: topological unfolding is a transient one-shot action rather than a persistent
         * physics law. Invoke [SimulationWorld.unfold], `DeponderState.unfold()`, or
         * `DeponderController.unfold()` instead.
         */
        @Deprecated(
            message = "Topological unfolding is a transient one-shot action; invoke DeponderState.unfold(), DeponderController.unfold(), or SimulationWorld.unfold() instead.",
        )
        public const val UNFOLD_ENABLED: Boolean = false
    }

    /**
     * Default point-mass node (planet) parameters.
     */
    public object Planet {
        /**
         * Default point-mass inertia (`mass`).
         */
        public const val MASS: Float = 2.293f

        /**
         * Default repulsion radiation range in `dp` (`~220px` at 2.75x).
         * Set to [INFINITE_RANGE] (`-1f`) for an unbounded inverse-square electrostatic field.
         */
        public const val REPULSION_RADIUS_DP: Float = 80f

        /**
         * Default repulsion stiffness (`> 0` repels, `< 0` attracts).
         */
        public const val ELASTICITY: Float = 1.33f

        /**
         * Deprecated (no-op since 0.6.1): implicit per-edge repulsion radius inflation has been
         * retired so node repulsion range strictly honors [PlanetPhysics.repulsionRadiusDp].
         * Configure `PlanetPhysics(repulsionRadiusDp = ...)` explicitly when hub nodes need a wider field.
         */
        @Deprecated(
            message = "No-op since 0.6.1: implicit degree-based radius inflation has been retired so repulsion strictly honors PlanetPhysics.repulsionRadiusDp.",
        )
        public const val DEGREE_RADIUS_INCREMENT_DP: Float = 0.15f

        /**
         * Base Coulomb charge multiplier used when a planet's repulsion radius is [INFINITE_RANGE].
         */
        public const val INFINITE_CHARGE_STRENGTH: Float = 180_000f
    }

    /**
     * Default spring link (rubber) parameters and visual styling defaults.
     */
    public object Rubber {
        public const val ELASTICITY: Float = 1.68f
        public const val NATURAL_LENGTH_DP: Float = 110f
        public const val CONTENT_PADDING_DP: Float = 80f
        public const val DEFAULT_ENDPOINT_GAP_DP: Float = 4f
        public const val DEFAULT_THICKNESS_DP: Float = 2f
        public const val DEFAULT_ARROW_SIZE_DP: Float = 7.5f
        public const val DEFAULT_LABEL_OFFSET_DP: Float = 2f
        public const val DEFAULT_LABEL_TEXT_SIZE_SP: Float = 11f
    }

    /**
     * Numerical integrator, Global Kinetic-Force Sleep thresholds, and spatial acceleration constants.
     */
    public object Simulation {
        /** Deprecated (no-op since 0.6.1): simulated annealing has been retired in favor of pure Newtonian [Root.AIR_DAMPING]. */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.",
        )
        public const val INITIAL_ALPHA: Float = 1.0f

        /** Deprecated (no-op since 0.6.1): simulated annealing has been retired in favor of pure Newtonian [Root.AIR_DAMPING]. */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.",
        )
        public const val INTERACTION_ALPHA: Float = 1.0f

        /** Deprecated (no-op since 0.6.1): simulated annealing has been retired in favor of pure Newtonian [Root.AIR_DAMPING]. */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.",
        )
        public const val ALPHA_MIN: Float = 0.001f

        /** Deprecated (no-op since 0.6.1): simulated annealing has been retired in favor of pure Newtonian [Root.AIR_DAMPING]. */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.",
        )
        public const val ALPHA_DECAY: Float = 0.015f

        /** Deprecated (no-op since 0.6.1): reference step constant for retired `alphaDecay` normalization. */
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; integration uses true physical dtSeconds with MAX_SUBSTEP_DT_SECONDS.",
        )
        public const val REFERENCE_DT_SECONDS: Float = 0.016f
        public const val MAX_DT_SECONDS: Float = 0.032f
        public const val MAX_SUBSTEP_DT_SECONDS: Float = 1f / 120f
        public const val MAX_SUBSTEPS: Int = 4

        public const val REST_SPEED_PX_PER_SEC: Float = 1.5f
        public const val REST_ACCEL_PX_PER_SEC_SQ: Float = 8.0f
        public const val SETTLE_DURATION_SECONDS: Float = 0.10f

        public const val QUADTREE_NODE_THRESHOLD: Int = 64
        public const val PARALLEL_DISPATCH_THRESHOLD: Int = 256
        public const val PARALLEL_WORKER_CHUNKS: Int = 4

        public const val BARNES_HUT_THETA: Float = 0.9f
        public const val COULOMB_SOFTENING_SQ: Float = 64f

        public const val COINCIDENT_EPSILON_SQ: Float = 1e-4f
        public const val COINCIDENT_PERTURBATION_RADIUS_PX: Float = 0.5f
    }

    /**
     * Ready-to-use environment tuning presets.
     */
    public data class Preset(
        public val airDamping: Float,
        public val wallRepulsionDp: Float,
        public val planetRepulsionDp: Float,
        public val planetElasticity: Float,
        public val rubberNaturalLengthDp: Float,
        public val rubberElasticity: Float,
        @Deprecated(
            message = "No-op since 0.6.1: simulated annealing has been retired; use the 6-parameter Preset constructor and configure airDamping instead.",
        )
        public val alphaDecay: Float,
    ) {
        /**
         * Creates a pure Newtonian [Preset] without retired simulated-annealing parameters.
         */
        @Suppress("DEPRECATION")
        public constructor(
            airDamping: Float,
            wallRepulsionDp: Float,
            planetRepulsionDp: Float,
            planetElasticity: Float,
            rubberNaturalLengthDp: Float,
            rubberElasticity: Float,
        ) : this(
            airDamping = airDamping,
            wallRepulsionDp = wallRepulsionDp,
            planetRepulsionDp = planetRepulsionDp,
            planetElasticity = planetElasticity,
            rubberNaturalLengthDp = rubberNaturalLengthDp,
            rubberElasticity = rubberElasticity,
            alphaDecay = 0f,
        )
    }

    public object Presets {
        /** Balanced springiness and smooth settling suitable for general relationship graphs. */
        public val Balanced: Preset = Preset(
            airDamping = Root.AIR_DAMPING,
            wallRepulsionDp = Root.WALL_REPULSION_DP,
            planetRepulsionDp = Planet.REPULSION_RADIUS_DP,
            planetElasticity = Planet.ELASTICITY,
            rubberNaturalLengthDp = Rubber.NATURAL_LENGTH_DP,
            rubberElasticity = Rubber.ELASTICITY,
        )

        /** High-damping preset that converges quickly with minimal oscillation. */
        public val Crisp: Preset = Preset(
            airDamping = 2.6f,
            wallRepulsionDp = Root.WALL_REPULSION_DP,
            planetRepulsionDp = Planet.REPULSION_RADIUS_DP,
            planetElasticity = 2.0f,
            rubberNaturalLengthDp = Rubber.NATURAL_LENGTH_DP,
            rubberElasticity = 2.4f,
        )

        /** Full-viewport electrostatic galaxy layout using [INFINITE_RANGE] on both walls and planets. */
        public val GalaxyInfinite: Preset = Preset(
            airDamping = 1.6f,
            wallRepulsionDp = INFINITE_RANGE,
            planetRepulsionDp = INFINITE_RANGE,
            planetElasticity = 1.0f,
            rubberNaturalLengthDp = 100f,
            rubberElasticity = 1.5f,
        )
    }
}
