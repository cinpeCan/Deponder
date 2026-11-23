package com.cinpe.deponder.core

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

private fun normalizeZero(value: Float): Float = if (value == 0f) 0f else value

private fun safeDensityOrOne(density: Float): Float =
    if (density.isFinite()) max(1e-3f, density) else 1f

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
 * Energy dissipation is governed solely by viscous [airDamping] (`1/s`), controlling how quickly
 * velocities settle into `0% CPU` idle sleep; passing `airDamping = 0f` yields undamped perpetual
 * motion. Pass a negative [wallRepulsionDp] (such as [DeponderDefaults.INFINITE_RANGE]) for an
 * unbounded viewport-centering boundary field. For tangled graphs, invoke `state.unfold()` (Compose)
 * or `controller.unfold()` (Android View) to run a one-shot topological unfolding pass before settling
 * into Newtonian equilibrium.
 */
public class RootPhysics internal constructor(
    initScale: Float,
    maxScale: Float,
    minScale: Float,
    airDamping: Float,
    wallRepulsionDp: Float,
    wallElasticityStart: Float,
    wallElasticityTop: Float,
    wallElasticityEnd: Float,
    wallElasticityBottom: Float,
    @Deprecated(
        message = "Topological unfolding is a transient one-shot action rather than a persistent physics property; invoke state.unfold() (Compose) or controller.unfold() (Android View) instead.",
    )
    public val unfoldEnabled: Boolean,
) {
    public val initScale: Float = normalizeZero(initScale)
    public val maxScale: Float = normalizeZero(maxScale)
    public val minScale: Float = normalizeZero(minScale)
    public val airDamping: Float = normalizeZero(airDamping)
    public val wallRepulsionDp: Float = normalizeZero(wallRepulsionDp)
    public val wallElasticityStart: Float = normalizeZero(wallElasticityStart)
    public val wallElasticityTop: Float = normalizeZero(wallElasticityTop)
    public val wallElasticityEnd: Float = normalizeZero(wallElasticityEnd)
    public val wallElasticityBottom: Float = normalizeZero(wallElasticityBottom)

    /**
     * Deprecated (no-op `false` since 0.6.1): simulated annealing (`alpha` cooling) has been
     * retired so it never conflicts with viscous [airDamping]. Always `false`.
     */
    @Deprecated(
        message = "No-op since 0.6.1: always false. Simulated annealing has been retired; configure airDamping instead (0f for perpetual motion).",
    )
    public val coolingEnabled: Boolean = false

    /**
     * Deprecated (no-op `0f` since 0.6.1): simulated annealing (`alpha` cooling) has been retired
     * in favor of pure Newtonian [airDamping]. Always `0f`.
     */
    @Deprecated(
        message = "No-op since 0.6.1: always 0f. Simulated annealing has been retired; configure airDamping instead.",
    )
    public val alphaDecay: Float = 0f

    public constructor() : this(
        initScale = DeponderDefaults.Root.INIT_SCALE,
        maxScale = DeponderDefaults.Root.MAX_SCALE,
        minScale = DeponderDefaults.Root.MIN_SCALE,
        airDamping = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionDp = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
        unfoldEnabled = false,
    )

    public constructor(
        initScale: Float = DeponderDefaults.Root.INIT_SCALE,
        maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
        minScale: Float = DeponderDefaults.Root.MIN_SCALE,
        airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionDp: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
    ) : this(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = false,
    )

    @Deprecated(
        message = "coolingEnabled and alphaDecay are no-ops since 0.6.1; use the 9-parameter RootPhysics constructor and configure airDamping instead (0f for perpetual motion).",
    )
    @Suppress("UNUSED_PARAMETER")
    public constructor(
        initScale: Float = DeponderDefaults.Root.INIT_SCALE,
        maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
        minScale: Float = DeponderDefaults.Root.MIN_SCALE,
        airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionDp: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
        coolingEnabled: Boolean,
        alphaDecay: Float = 0f,
    ) : this(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = false,
    )

    @Deprecated(
        message = "alphaDecay is a no-op since 0.6.1; use the 9-parameter RootPhysics constructor and configure airDamping instead.",
    )
    @Suppress("UNUSED_PARAMETER")
    public constructor(
        initScale: Float = DeponderDefaults.Root.INIT_SCALE,
        maxScale: Float = DeponderDefaults.Root.MAX_SCALE,
        minScale: Float = DeponderDefaults.Root.MIN_SCALE,
        airDamping: Float = DeponderDefaults.Root.AIR_DAMPING,
        wallRepulsionDp: Float = DeponderDefaults.Root.WALL_REPULSION_DP,
        wallElasticityStart: Float = DeponderDefaults.Root.WALL_ELASTICITY_START,
        wallElasticityTop: Float = DeponderDefaults.Root.WALL_ELASTICITY_TOP,
        wallElasticityEnd: Float = DeponderDefaults.Root.WALL_ELASTICITY_END,
        wallElasticityBottom: Float = DeponderDefaults.Root.WALL_ELASTICITY_BOTTOM,
        alphaDecay: Float,
    ) : this(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = false,
    )

    init {
        require(this.initScale.isFinite()) { "initScale must be finite, but was ${this.initScale}" }
        require(this.minScale.isFinite() && this.minScale > 0f) {
            "minScale must be finite and > 0f, but was ${this.minScale}"
        }
        require(this.maxScale.isFinite() && this.maxScale >= this.minScale) {
            "maxScale (${this.maxScale}) must be finite and >= minScale (${this.minScale})"
        }
        require(this.airDamping.isFinite()) { "airDamping must be finite, but was ${this.airDamping}" }
        require(this.wallRepulsionDp.isFinite()) {
            "wallRepulsionDp must be finite, but was ${this.wallRepulsionDp}"
        }
        require(this.wallElasticityStart.isFinite()) {
            "wallElasticityStart must be finite, but was ${this.wallElasticityStart}"
        }
        require(this.wallElasticityTop.isFinite()) {
            "wallElasticityTop must be finite, but was ${this.wallElasticityTop}"
        }
        require(this.wallElasticityEnd.isFinite()) {
            "wallElasticityEnd must be finite, but was ${this.wallElasticityEnd}"
        }
        require(this.wallElasticityBottom.isFinite()) {
            "wallElasticityBottom must be finite, but was ${this.wallElasticityBottom}"
        }
    }

    public operator fun component1(): Float = initScale
    public operator fun component2(): Float = maxScale
    public operator fun component3(): Float = minScale
    public operator fun component4(): Float = airDamping
    public operator fun component5(): Float = wallRepulsionDp
    public operator fun component6(): Float = wallElasticityStart
    public operator fun component7(): Float = wallElasticityTop
    public operator fun component8(): Float = wallElasticityEnd
    public operator fun component9(): Float = wallElasticityBottom

    @Deprecated(message = "No-op since 0.6.1: coolingEnabled has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component10(): Boolean = coolingEnabled

    @Deprecated(message = "No-op since 0.6.1: alphaDecay has been retired.")
    @Suppress("DEPRECATION")
    public operator fun component11(): Float = alphaDecay

    @Suppress("DEPRECATION")
    public fun copy(
        initScale: Float = this.initScale,
        maxScale: Float = this.maxScale,
        minScale: Float = this.minScale,
        airDamping: Float = this.airDamping,
        wallRepulsionDp: Float = this.wallRepulsionDp,
        wallElasticityStart: Float = this.wallElasticityStart,
        wallElasticityTop: Float = this.wallElasticityTop,
        wallElasticityEnd: Float = this.wallElasticityEnd,
        wallElasticityBottom: Float = this.wallElasticityBottom,
    ): RootPhysics = RootPhysics(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = this.unfoldEnabled,
    )

    @Deprecated(
        message = "coolingEnabled and alphaDecay are no-ops since 0.6.1; use the 9-parameter copy(...) overload instead.",
    )
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    public fun copy(
        initScale: Float = this.initScale,
        maxScale: Float = this.maxScale,
        minScale: Float = this.minScale,
        airDamping: Float = this.airDamping,
        wallRepulsionDp: Float = this.wallRepulsionDp,
        wallElasticityStart: Float = this.wallElasticityStart,
        wallElasticityTop: Float = this.wallElasticityTop,
        wallElasticityEnd: Float = this.wallElasticityEnd,
        wallElasticityBottom: Float = this.wallElasticityBottom,
        coolingEnabled: Boolean,
        alphaDecay: Float = 0f,
    ): RootPhysics = RootPhysics(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = this.unfoldEnabled,
    )

    @Deprecated(
        message = "alphaDecay is a no-op since 0.6.1; use the 9-parameter copy(...) overload instead.",
    )
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    public fun copy(
        initScale: Float = this.initScale,
        maxScale: Float = this.maxScale,
        minScale: Float = this.minScale,
        airDamping: Float = this.airDamping,
        wallRepulsionDp: Float = this.wallRepulsionDp,
        wallElasticityStart: Float = this.wallElasticityStart,
        wallElasticityTop: Float = this.wallElasticityTop,
        wallElasticityEnd: Float = this.wallElasticityEnd,
        wallElasticityBottom: Float = this.wallElasticityBottom,
        alphaDecay: Float,
    ): RootPhysics = RootPhysics(
        initScale = initScale,
        maxScale = maxScale,
        minScale = minScale,
        airDamping = airDamping,
        wallRepulsionDp = wallRepulsionDp,
        wallElasticityStart = wallElasticityStart,
        wallElasticityTop = wallElasticityTop,
        wallElasticityEnd = wallElasticityEnd,
        wallElasticityBottom = wallElasticityBottom,
        unfoldEnabled = this.unfoldEnabled,
    )

    public fun withScaleBounds(initScale: Float, minScale: Float, maxScale: Float): RootPhysics =
        copy(initScale = initScale, minScale = minScale, maxScale = maxScale)

    public fun withAirDamping(airDamping: Float): RootPhysics =
        copy(airDamping = airDamping)

    public fun withWallRepulsionDp(wallRepulsionDp: Float): RootPhysics =
        copy(wallRepulsionDp = wallRepulsionDp)

    /**
     * Deprecated (no-op since 0.6.1): simulated annealing (`alpha` cooling) has been retired so
     * it cannot conflict with Newtonian viscous drag. Returns `this` unchanged; configure
     * [withAirDamping] (`0f` for undamped perpetual motion) instead.
     */
    @Deprecated(
        message = "No-op since 0.6.1: simulated annealing has been retired; use withAirDamping(airDamping) instead (0f for perpetual motion).",
        replaceWith = ReplaceWith("this"),
    )
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    public fun withCooling(coolingEnabled: Boolean, alphaDecay: Float = this.alphaDecay): RootPhysics = this

    /**
     * Deprecated: topological unfolding is a transient one-shot action rather than a persistent
     * physics property. Invoke `state.unfold()` (Compose) or `controller.unfold()` (Android View) instead.
     */
    @Deprecated(
        message = "Topological unfolding is a transient one-shot action rather than a persistent physics property; invoke state.unfold() (Compose) or controller.unfold() (Android View) instead.",
    )
    @JvmOverloads
    public fun withUnfold(unfoldEnabled: Boolean = true): RootPhysics =
        RootPhysics(
            initScale = initScale,
            maxScale = maxScale,
            minScale = minScale,
            airDamping = airDamping,
            wallRepulsionDp = wallRepulsionDp,
            wallElasticityStart = wallElasticityStart,
            wallElasticityTop = wallElasticityTop,
            wallElasticityEnd = wallElasticityEnd,
            wallElasticityBottom = wallElasticityBottom,
            unfoldEnabled = unfoldEnabled,
        )

    /**
     * Converts this `dp`-based configuration into pixel-space [EnvironmentSpec] for [density] (`px / dp`).
     */
    @Suppress("DEPRECATION")
    public fun toEnvironmentSpec(density: Float): EnvironmentSpec {
        val safeDensity = safeDensityOrOne(density)
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
            degreeRadiusIncrementPx = 0f,
            infiniteChargeStrength = DeponderDefaults.Planet.INFINITE_CHARGE_STRENGTH,
            coolingEnabled = false,
            alphaDecay = 0f,
            alphaMin = 0f,
            restSpeedPxPerSec = DeponderDefaults.Simulation.REST_SPEED_PX_PER_SEC,
            restAccelPxPerSecSq = DeponderDefaults.Simulation.REST_ACCEL_PX_PER_SEC_SQ,
            settleDurationSeconds = DeponderDefaults.Simulation.SETTLE_DURATION_SECONDS,
            unfoldEnabled = unfoldEnabled,
        )
    }

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RootPhysics) return false
        return initScale == other.initScale &&
            maxScale == other.maxScale &&
            minScale == other.minScale &&
            airDamping == other.airDamping &&
            wallRepulsionDp == other.wallRepulsionDp &&
            wallElasticityStart == other.wallElasticityStart &&
            wallElasticityTop == other.wallElasticityTop &&
            wallElasticityEnd == other.wallElasticityEnd &&
            wallElasticityBottom == other.wallElasticityBottom &&
            unfoldEnabled == other.unfoldEnabled
    }

    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = initScale.hashCode()
        result = 31 * result + maxScale.hashCode()
        result = 31 * result + minScale.hashCode()
        result = 31 * result + airDamping.hashCode()
        result = 31 * result + wallRepulsionDp.hashCode()
        result = 31 * result + wallElasticityStart.hashCode()
        result = 31 * result + wallElasticityTop.hashCode()
        result = 31 * result + wallElasticityEnd.hashCode()
        result = 31 * result + wallElasticityBottom.hashCode()
        result = 31 * result + unfoldEnabled.hashCode()
        return result
    }

    @Suppress("DEPRECATION")
    override fun toString(): String =
        "RootPhysics(initScale=$initScale, maxScale=$maxScale, minScale=$minScale, " +
            "airDamping=$airDamping, wallRepulsionDp=$wallRepulsionDp, " +
            "wallElasticityStart=$wallElasticityStart, wallElasticityTop=$wallElasticityTop, " +
            "wallElasticityEnd=$wallElasticityEnd, wallElasticityBottom=$wallElasticityBottom, " +
            "unfoldEnabled=$unfoldEnabled)"

    public companion object {
        @JvmStatic
        public fun fromPreset(preset: DeponderDefaults.Preset): RootPhysics =
            RootPhysics(
                airDamping = preset.airDamping,
                wallRepulsionDp = preset.wallRepulsionDp,
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
        require(mass.isFinite() && mass > 0f) { "PlanetPhysics.mass must be finite and > 0f, but was $mass" }
        require(repulsionRadiusDp.isFinite()) {
            "PlanetPhysics.repulsionRadiusDp must be finite, but was $repulsionRadiusDp"
        }
        require(elasticity.isFinite()) { "PlanetPhysics.elasticity must be finite, but was $elasticity" }
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
        val safeDensity = safeDensityOrOne(density)
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
 * By default (`RubberPhysics()` or `naturalLengthDp = DeponderDefaults.AUTO_LENGTH`), natural spring
 * length adapts automatically to the rubber's measured non-stretched content width (`label` text,
 * built-in directional arrowheads, or custom non-stretched badges):
 * ```
 * resolvedNaturalLengthDp = clamp(contentWidthDp + contentPaddingDp, minLengthDp, maxLengthDp)
 * ```
 * Pass an explicit `naturalLengthDp >= 0f` (or [RubberPhysics.fixed]) to lock a fixed center-to-center
 * spring rest length, or call [RubberPhysics.auto] to customize `contentPaddingDp`, `minLengthDp`, and
 * `maxLengthDp`.
 *
 * Visual endpoint contour clearance is configured separately via [DeponderRubberInsets] on the UI
 * operator so physics specifications remain strictly free of rendering concerns.
 *
 * @property elasticity Hooke spring stiffness (`>= 0f` typical).
 * @property naturalLengthDp Explicit rest length in `dp` (`>= 0f`), or [DeponderDefaults.AUTO_LENGTH] (`-1f`) when content-adaptive.
 * @property contentPaddingDp Total horizontal breathing margin in `dp` added to content width in adaptive mode.
 * @property minLengthDp Minimum rest length floor in `dp` in adaptive mode (equals [naturalLengthDp] in fixed mode).
 * @property maxLengthDp Maximum rest length ceiling in `dp` in adaptive mode, or [DeponderDefaults.INFINITE_RANGE] (`-1f`) when unbounded.
 */
public class RubberPhysics internal constructor(
    elasticity: Float,
    naturalLengthDp: Float,
    contentPaddingDp: Float,
    minLengthDp: Float,
    maxLengthDp: Float,
) {
    public val elasticity: Float = normalizeZero(elasticity)
    public val naturalLengthDp: Float = normalizeZero(naturalLengthDp)
    public val contentPaddingDp: Float = normalizeZero(contentPaddingDp)
    public val minLengthDp: Float = normalizeZero(minLengthDp)
    public val maxLengthDp: Float = normalizeZero(maxLengthDp)

    /**
     * Creates a [RubberPhysics] configuration.
     *
     * Omitting [naturalLengthDp] (or passing [DeponderDefaults.AUTO_LENGTH]) enables content-adaptive
     * spring length (`contentPaddingDp = 80dp`, `minLengthDp = 110dp`); passing `naturalLengthDp >= 0f`
     * locks a fixed center-to-center spring rest length.
     */
    @JvmOverloads
    public constructor(
        elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
        naturalLengthDp: Float = DeponderDefaults.AUTO_LENGTH,
    ) : this(
        elasticity = elasticity,
        naturalLengthDp = naturalLengthDp,
        contentPaddingDp = if (naturalLengthDp == DeponderDefaults.AUTO_LENGTH) {
            DeponderDefaults.Rubber.CONTENT_PADDING_DP
        } else {
            0f
        },
        minLengthDp = if (naturalLengthDp == DeponderDefaults.AUTO_LENGTH) {
            DeponderDefaults.Rubber.NATURAL_LENGTH_DP
        } else {
            max(0f, naturalLengthDp)
        },
        maxLengthDp = if (naturalLengthDp == DeponderDefaults.AUTO_LENGTH) {
            DeponderDefaults.INFINITE_RANGE
        } else {
            max(0f, naturalLengthDp)
        },
    )

    init {
        require(elasticity.isFinite()) {
            "RubberPhysics.elasticity must be finite, but was $elasticity"
        }
        require(
            naturalLengthDp.isFinite() &&
                (naturalLengthDp >= 0f || naturalLengthDp == DeponderDefaults.AUTO_LENGTH),
        ) {
            "RubberPhysics.naturalLengthDp must be >= 0f or DeponderDefaults.AUTO_LENGTH (-1f), but was $naturalLengthDp"
        }
        require(contentPaddingDp.isFinite() && contentPaddingDp >= 0f) {
            "RubberPhysics.contentPaddingDp must be finite and >= 0f, but was $contentPaddingDp"
        }
        require(minLengthDp.isFinite() && minLengthDp >= 0f) {
            "RubberPhysics.minLengthDp must be finite and >= 0f, but was $minLengthDp"
        }
        require(
            maxLengthDp.isFinite() &&
                (maxLengthDp == DeponderDefaults.INFINITE_RANGE || maxLengthDp >= minLengthDp),
        ) {
            "RubberPhysics.maxLengthDp ($maxLengthDp) must be DeponderDefaults.INFINITE_RANGE (-1f) or >= minLengthDp ($minLengthDp)"
        }
    }

    /** `true` when rest length adapts dynamically to rubber content width ([naturalLengthDp] == [DeponderDefaults.AUTO_LENGTH]). */
    public val isAutoLength: Boolean
        get() = naturalLengthDp < 0f

    public operator fun component1(): Float = elasticity

    public operator fun component2(): Float = naturalLengthDp

    public fun copy(
        elasticity: Float = this.elasticity,
        naturalLengthDp: Float = this.naturalLengthDp,
    ): RubberPhysics {
        if (naturalLengthDp == DeponderDefaults.AUTO_LENGTH) {
            return if (this.isAutoLength) {
                RubberPhysics(
                    elasticity = elasticity,
                    naturalLengthDp = DeponderDefaults.AUTO_LENGTH,
                    contentPaddingDp = this.contentPaddingDp,
                    minLengthDp = this.minLengthDp,
                    maxLengthDp = this.maxLengthDp,
                )
            } else {
                RubberPhysics(elasticity = elasticity, naturalLengthDp = DeponderDefaults.AUTO_LENGTH)
            }
        }
        return RubberPhysics(elasticity = elasticity, naturalLengthDp = naturalLengthDp)
    }

    /** Returns a copy with updated [elasticity], preserving fixed or customized auto-length parameters. */
    public fun withElasticity(elasticity: Float): RubberPhysics =
        RubberPhysics(
            elasticity = elasticity,
            naturalLengthDp = this.naturalLengthDp,
            contentPaddingDp = this.contentPaddingDp,
            minLengthDp = this.minLengthDp,
            maxLengthDp = this.maxLengthDp,
        )

    /**
     * Returns a copy with [naturalLengthDp] (`>= 0f` for fixed length, or [DeponderDefaults.AUTO_LENGTH] for adaptive length).
     */
    public fun withNaturalLengthDp(naturalLengthDp: Float): RubberPhysics =
        copy(naturalLengthDp = naturalLengthDp)

    /**
     * Returns a copy configured for content-adaptive spring length with customized padding and bounds.
     */
    @JvmOverloads
    public fun withAutoLength(
        contentPaddingDp: Float = DeponderDefaults.Rubber.CONTENT_PADDING_DP,
        minLengthDp: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
        maxLengthDp: Float = DeponderDefaults.INFINITE_RANGE,
    ): RubberPhysics = auto(
        contentPaddingDp = contentPaddingDp,
        minLengthDp = minLengthDp,
        maxLengthDp = maxLengthDp,
        elasticity = this.elasticity,
    )

    /**
     * Resolves the effective rest length in `dp` for a measured unscaled [contentWidthDp] (`>= 0f`).
     */
    @JvmOverloads
    public fun resolveNaturalLengthDp(contentWidthDp: Float = 0f): Float {
        if (!isAutoLength) return naturalLengthDp
        val safeContentDp = if (contentWidthDp.isFinite() && contentWidthDp > 0f) contentWidthDp else 0f
        val floored = max(minLengthDp, safeContentDp + contentPaddingDp)
        return if (maxLengthDp < 0f) floored else min(floored, maxLengthDp)
    }

    /**
     * Resolves the effective rest length in pixels for [density] (`px / dp`) and unscaled [contentWidthPx].
     */
    @JvmOverloads
    public fun resolveNaturalLengthPx(density: Float, contentWidthPx: Float = 0f): Float {
        val safeDensity = safeDensityOrOne(density)
        val safeContentPx = if (contentWidthPx.isFinite() && contentWidthPx > 0f) contentWidthPx else 0f
        return resolveNaturalLengthDp(safeContentPx / safeDensity) * safeDensity
    }

    /**
     * Converts this `dp`-based configuration into pixel-space [RubberSpec] for [startId], [endId], and [density].
     */
    public fun toRubberSpec(startId: String, endId: String, density: Float): RubberSpec =
        toRubberSpec(startId = startId, endId = endId, density = density, contentWidthPx = 0f)

    /**
     * Converts this `dp`-based configuration into pixel-space [RubberSpec] for [startId], [endId],
     * [density], and measured unscaled [contentWidthPx].
     */
    public fun toRubberSpec(
        startId: String,
        endId: String,
        density: Float,
        contentWidthPx: Float,
    ): RubberSpec = RubberSpec(
        startId = startId,
        endId = endId,
        elasticity = elasticity,
        naturalLengthPx = resolveNaturalLengthPx(density = density, contentWidthPx = contentWidthPx),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RubberPhysics) return false
        return elasticity == other.elasticity &&
            naturalLengthDp == other.naturalLengthDp &&
            contentPaddingDp == other.contentPaddingDp &&
            minLengthDp == other.minLengthDp &&
            maxLengthDp == other.maxLengthDp
    }

    override fun hashCode(): Int {
        var result = elasticity.hashCode()
        result = 31 * result + naturalLengthDp.hashCode()
        result = 31 * result + contentPaddingDp.hashCode()
        result = 31 * result + minLengthDp.hashCode()
        result = 31 * result + maxLengthDp.hashCode()
        return result
    }

    override fun toString(): String =
        if (isAutoLength) {
            "RubberPhysics.Auto(elasticity=$elasticity, contentPaddingDp=$contentPaddingDp, " +
                "minLengthDp=$minLengthDp, maxLengthDp=$maxLengthDp)"
        } else {
            "RubberPhysics.Fixed(elasticity=$elasticity, naturalLengthDp=$naturalLengthDp)"
        }

    public companion object {
        /** Default content-adaptive [RubberPhysics] (`elasticity = 1.68f`, `contentPaddingDp = 80dp`, `minLengthDp = 110dp`). */
        public val Default: RubberPhysics = RubberPhysics()

        /**
         * Creates a content-adaptive [RubberPhysics] whose rest length expands with measured rubber content width:
         * `clamp(contentWidthDp + contentPaddingDp, minLengthDp, maxLengthDp)`.
         */
        @JvmStatic
        @JvmOverloads
        public fun auto(
            contentPaddingDp: Float = DeponderDefaults.Rubber.CONTENT_PADDING_DP,
            minLengthDp: Float = DeponderDefaults.Rubber.NATURAL_LENGTH_DP,
            maxLengthDp: Float = DeponderDefaults.INFINITE_RANGE,
            elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
        ): RubberPhysics = RubberPhysics(
            elasticity = elasticity,
            naturalLengthDp = DeponderDefaults.AUTO_LENGTH,
            contentPaddingDp = contentPaddingDp,
            minLengthDp = minLengthDp,
            maxLengthDp = maxLengthDp,
        )

        /**
         * Creates a fixed-length [RubberPhysics] locked to [naturalLengthDp] (`>= 0f`) regardless of label length.
         */
        @JvmStatic
        @JvmOverloads
        public fun fixed(
            naturalLengthDp: Float,
            elasticity: Float = DeponderDefaults.Rubber.ELASTICITY,
        ): RubberPhysics {
            require(naturalLengthDp.isFinite() && naturalLengthDp >= 0f) {
                "RubberPhysics.fixed naturalLengthDp must be finite and >= 0f, but was $naturalLengthDp"
            }
            return RubberPhysics(elasticity = elasticity, naturalLengthDp = naturalLengthDp)
        }

        /**
         * Creates a content-adaptive [RubberPhysics] seeded from [preset] (`minLengthDp = preset.rubberNaturalLengthDp`).
         */
        @JvmStatic
        public fun fromPreset(preset: DeponderDefaults.Preset): RubberPhysics =
            auto(
                minLengthDp = preset.rubberNaturalLengthDp,
                elasticity = preset.rubberElasticity,
            )
    }
}
