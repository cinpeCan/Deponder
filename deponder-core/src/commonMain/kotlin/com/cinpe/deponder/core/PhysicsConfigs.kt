package com.cinpe.deponder.core

import kotlin.jvm.JvmOverloads
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
 * Endpoint safe-area clearance configuration for spring links (`Rubber`), inspired by `WindowInsets`.
 *
 * Decouples UI endpoint clearance from Hooke spring mechanics ([RubberPhysics]): [SimulationWorld]
 * outputs raw center-to-center coordinates alongside real-time directional planet contour radii on
 * [RubberPose], and UI link bindings consume [DeponderRubberInsets] on the graphics layer with zero
 * relayout overhead.
 *
 * Each endpoint (`startId` and `endId`) independently operates in one of three [Mode]s:
 * - [Mode.CONTOUR]: Automatic UI contour estimation plus an optional `dp` breathing offset (positive or negative).
 * - [Mode.FIXED]: Explicit user-declared `dp` safe-area distance from the planet center, bypassing contour estimation.
 * - [Mode.NONE]: Connects directly to the planet center (`0px` inset) and suppresses built-in default arrowheads on that end.
 */
public class DeponderRubberInsets internal constructor(
    public val startMode: Mode,
    public val endMode: Mode,
    startValueDp: Float,
    endValueDp: Float,
) {
    /**
     * Safe-area resolution mode for an individual rubber endpoint.
     */
    public enum class Mode {
        /** Connects directly to the planet center (`0px` inset; built-in default rubbers omit arrowheads on this end). */
        NONE,

        /** Trims outside the planet's estimated UI contour plus an optional `dp` gap offset. */
        CONTOUR,

        /** Trims by an explicit positive `dp` distance measured from the planet center. */
        FIXED,
    }

    public val startValueDp: Float = if (startMode == Mode.NONE || startValueDp == 0f) 0f else startValueDp
    public val endValueDp: Float = if (endMode == Mode.NONE || endValueDp == 0f) 0f else endValueDp

    init {
        require(this.startValueDp.isFinite()) {
            "DeponderRubberInsets.startValueDp must be finite, but was $startValueDp"
        }
        require(this.endValueDp.isFinite()) {
            "DeponderRubberInsets.endValueDp must be finite, but was $endValueDp"
        }
        if (startMode == Mode.FIXED) {
            require(this.startValueDp > 0f) {
                "DeponderRubberInsets FIXED startValueDp must be > 0f, but was $startValueDp"
            }
        }
        if (endMode == Mode.FIXED) {
            require(this.endValueDp > 0f) {
                "DeponderRubberInsets FIXED endValueDp must be > 0f, but was $endValueDp"
            }
        }
    }

    /** `true` when the `startId` endpoint defines a safe area ([Mode.CONTOUR] or [Mode.FIXED]). */
    public val hasStartSafeArea: Boolean
        get() = startMode != Mode.NONE

    /** `true` when the `endId` endpoint defines a safe area ([Mode.CONTOUR] or [Mode.FIXED]). */
    public val hasEndSafeArea: Boolean
        get() = endMode != Mode.NONE

    /** Returns a copy with the `startId` endpoint anchored directly to the planet center ([Mode.NONE]). */
    public fun withStartNone(): DeponderRubberInsets =
        DeponderRubberInsets(Mode.NONE, endMode, 0f, endValueDp)

    /** Returns a copy with the `endId` endpoint anchored directly to the planet center ([Mode.NONE]). */
    public fun withEndNone(): DeponderRubberInsets =
        DeponderRubberInsets(startMode, Mode.NONE, startValueDp, 0f)

    /** Returns a copy with automatic contour estimation and [gapDp] offset on the `startId` endpoint. */
    @JvmOverloads
    public fun withStartContour(
        gapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
    ): DeponderRubberInsets {
        requireFiniteGap("gapDp", gapDp)
        return DeponderRubberInsets(Mode.CONTOUR, endMode, gapDp, endValueDp)
    }

    /** Returns a copy with automatic contour estimation and [gapDp] offset on the `endId` endpoint. */
    @JvmOverloads
    public fun withEndContour(
        gapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
    ): DeponderRubberInsets {
        requireFiniteGap("gapDp", gapDp)
        return DeponderRubberInsets(startMode, Mode.CONTOUR, startValueDp, gapDp)
    }

    /**
     * Returns a copy with an explicit [insetDp] safe-area distance from the `startId` planet center
     * (`0f` normalizes to [Mode.NONE]).
     */
    public fun withStartFixed(insetDp: Float): DeponderRubberInsets {
        requireValidFixedInset("insetDp", insetDp)
        val sMode = if (insetDp == 0f) Mode.NONE else Mode.FIXED
        return DeponderRubberInsets(sMode, endMode, insetDp, endValueDp)
    }

    /**
     * Returns a copy with an explicit [insetDp] safe-area distance from the `endId` planet center
     * (`0f` normalizes to [Mode.NONE]).
     */
    public fun withEndFixed(insetDp: Float): DeponderRubberInsets {
        requireValidFixedInset("insetDp", insetDp)
        val eMode = if (insetDp == 0f) Mode.NONE else Mode.FIXED
        return DeponderRubberInsets(startMode, eMode, startValueDp, insetDp)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeponderRubberInsets) return false
        return startMode == other.startMode &&
            endMode == other.endMode &&
            startValueDp == other.startValueDp &&
            endValueDp == other.endValueDp
    }

    override fun hashCode(): Int {
        var result = startMode.hashCode()
        result = 31 * result + endMode.hashCode()
        result = 31 * result + startValueDp.hashCode()
        result = 31 * result + endValueDp.hashCode()
        return result
    }

    override fun toString(): String =
        "DeponderRubberInsets(startMode=$startMode, endMode=$endMode, " +
            "startValueDp=$startValueDp, endValueDp=$endValueDp)"

    public companion object {
        /**
         * Symmetric automatic contour estimation on both endpoints with
         * [DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP] (`4dp`).
         */
        public val Default: DeponderRubberInsets = DeponderRubberInsets(
            startMode = Mode.CONTOUR,
            endMode = Mode.CONTOUR,
            startValueDp = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
            endValueDp = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
        )

        /**
         * Edge-to-edge center-to-center anchoring (`Mode.NONE` on both endpoints).
         * Keeps continuous plain lines connected underneath planets and suppresses built-in default arrowheads.
         */
        public val None: DeponderRubberInsets = DeponderRubberInsets(
            startMode = Mode.NONE,
            endMode = Mode.NONE,
            startValueDp = 0f,
            endValueDp = 0f,
        )

        @JvmStatic
        public fun none(): DeponderRubberInsets = None

        /**
         * Symmetric automatic contour estimation on both endpoints with [gapDp] breathing offset
         * (supports negative values to trim inward inside padded touch targets).
         */
        @JvmStatic
        @JvmOverloads
        public fun contour(
            gapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
        ): DeponderRubberInsets {
            requireFiniteGap("gapDp", gapDp)
            if (gapDp == DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP) return Default
            return DeponderRubberInsets(
                startMode = Mode.CONTOUR,
                endMode = Mode.CONTOUR,
                startValueDp = gapDp,
                endValueDp = gapDp,
            )
        }

        /**
         * Asymmetric automatic contour estimation on both endpoints with independent `dp` offsets.
         */
        @JvmStatic
        public fun contour(startGapDp: Float, endGapDp: Float): DeponderRubberInsets {
            requireFiniteGap("startGapDp", startGapDp)
            requireFiniteGap("endGapDp", endGapDp)
            return DeponderRubberInsets(
                startMode = Mode.CONTOUR,
                endMode = Mode.CONTOUR,
                startValueDp = startGapDp,
                endValueDp = endGapDp,
            )
        }

        /**
         * Symmetric user-declared safe-area distance [insetDp] (`>= 0f`) measured directly from both
         * planet centers (`0f` returns [None]).
         */
        @JvmStatic
        public fun fixed(insetDp: Float): DeponderRubberInsets {
            requireValidFixedInset("insetDp", insetDp)
            if (insetDp == 0f) return None
            return DeponderRubberInsets(
                startMode = Mode.FIXED,
                endMode = Mode.FIXED,
                startValueDp = insetDp,
                endValueDp = insetDp,
            )
        }

        /**
         * Independent user-declared safe-area distances ([startDp], [endDp] `>= 0f`) measured from each
         * planet center; passing `0f` on either side normalizes that endpoint to [Mode.NONE].
         */
        @JvmStatic
        public fun fixed(startDp: Float, endDp: Float): DeponderRubberInsets {
            requireValidFixedInset("startDp", startDp)
            requireValidFixedInset("endDp", endDp)
            if (startDp == 0f && endDp == 0f) return None
            return DeponderRubberInsets(
                startMode = if (startDp == 0f) Mode.NONE else Mode.FIXED,
                endMode = if (endDp == 0f) Mode.NONE else Mode.FIXED,
                startValueDp = startDp,
                endValueDp = endDp,
            )
        }

        /**
         * Automatic contour clearance on `startId` only; `endId` connects directly to the planet center ([Mode.NONE]).
         */
        @JvmStatic
        @JvmOverloads
        public fun startOnly(
            gapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
        ): DeponderRubberInsets {
            requireFiniteGap("gapDp", gapDp)
            return DeponderRubberInsets(
                startMode = Mode.CONTOUR,
                endMode = Mode.NONE,
                startValueDp = gapDp,
                endValueDp = 0f,
            )
        }

        /**
         * Automatic contour clearance on `endId` only; `startId` connects directly to the planet center ([Mode.NONE]).
         */
        @JvmStatic
        @JvmOverloads
        public fun endOnly(
            gapDp: Float = DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP,
        ): DeponderRubberInsets {
            requireFiniteGap("gapDp", gapDp)
            return DeponderRubberInsets(
                startMode = Mode.NONE,
                endMode = Mode.CONTOUR,
                startValueDp = 0f,
                endValueDp = gapDp,
            )
        }

        private fun requireFiniteGap(name: String, value: Float) {
            require(value.isFinite()) {
                "DeponderRubberInsets.$name must be finite, but was $value"
            }
        }

        private fun requireValidFixedInset(name: String, value: Float) {
            require(value.isFinite() && value >= 0f) {
                "DeponderRubberInsets.$name must be finite and >= 0f, but was $value"
            }
        }
    }
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
 * Pure Hooke spring link (`Rubber`) physics configuration used across both
 * Compose Multiplatform (`Modifier.deponderRubber`) and Android Views (`view.deponderRubber`).
 *
 * Visual endpoint contour clearance is configured separately via [DeponderRubberInsets] on the UI
 * operator so physics specifications remain strictly free of rendering concerns.
 *
 * @property elasticity Hooke spring stiffness (`>= 0f` typical).
 * @property naturalLengthDp Rest center-to-center spring length in `dp` (`>= 0f`).
 */
public data class RubberPhysics(
    public val elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
    public val naturalLengthDp: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
) {
    init {
        require(naturalLengthDp >= 0f) {
            "RubberPhysics.naturalLengthDp must be >= 0f, but was $naturalLengthDp"
        }
    }

    public fun withElasticity(elasticity: Float): RubberPhysics = copy(elasticity = elasticity)

    public fun withNaturalLengthDp(naturalLengthDp: Float): RubberPhysics =
        copy(naturalLengthDp = naturalLengthDp)

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
