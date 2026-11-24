@preconcurrency @_exported import DeponderCore
import Foundation

extension DeponderDefaults.Presets {
    public static var balanced: DeponderDefaults.Preset { shared.Balanced }
    public static var crisp: DeponderDefaults.Preset { shared.Crisp }
    public static var galaxyInfinite: DeponderDefaults.Preset { shared.GalaxyInfinite }
}

extension DeponderArrowMode {
    public static var none: DeponderArrowMode { .NONE }
    public static var end: DeponderArrowMode { .END }
    public static var start: DeponderArrowMode { .START }
    public static var both: DeponderArrowMode { .BOTH }
}

extension DeponderRubberInsets.Mode {
    public static var none: DeponderRubberInsets.Mode { .NONE }
    public static var contour: DeponderRubberInsets.Mode { .CONTOUR }
    public static var fixed: DeponderRubberInsets.Mode { .FIXED }
}

extension RootPhysics {
    public static var `default`: RootPhysics {
        RootPhysics()
    }

    public static var balanced: RootPhysics {
        Companion.shared.fromPreset(preset: DeponderDefaults.Presets.shared.Balanced)
    }

    public static var crisp: RootPhysics {
        Companion.shared.fromPreset(preset: DeponderDefaults.Presets.shared.Crisp)
    }

    public static var galaxyInfinite: RootPhysics {
        Companion.shared.fromPreset(preset: DeponderDefaults.Presets.shared.GalaxyInfinite)
    }

    public static func fromPreset(_ preset: DeponderDefaults.Preset) -> RootPhysics {
        Companion.shared.fromPreset(preset: preset)
    }

    public static func make(
        initScale: Float = DeponderDefaults.Root.shared.INIT_SCALE,
        maxScale: Float = DeponderDefaults.Root.shared.MAX_SCALE,
        minScale: Float = DeponderDefaults.Root.shared.MIN_SCALE,
        airDamping: Float = DeponderDefaults.Root.shared.AIR_DAMPING,
        wallRepulsionDp: Float = DeponderDefaults.Root.shared.WALL_REPULSION_DP,
        wallElasticityStart: Float = DeponderDefaults.Root.shared.WALL_ELASTICITY_START,
        wallElasticityTop: Float = DeponderDefaults.Root.shared.WALL_ELASTICITY_TOP,
        wallElasticityEnd: Float = DeponderDefaults.Root.shared.WALL_ELASTICITY_END,
        wallElasticityBottom: Float = DeponderDefaults.Root.shared.WALL_ELASTICITY_BOTTOM
    ) -> RootPhysics {
        RootPhysics(
            initScale: initScale,
            maxScale: maxScale,
            minScale: minScale,
            airDamping: airDamping,
            wallRepulsionDp: wallRepulsionDp,
            wallElasticityStart: wallElasticityStart,
            wallElasticityTop: wallElasticityTop,
            wallElasticityEnd: wallElasticityEnd,
            wallElasticityBottom: wallElasticityBottom
        )
    }
}

extension PlanetPhysics {
    public static var `default`: PlanetPhysics {
        PlanetPhysics()
    }

    public convenience init() {
        self.init(
            mass: DeponderDefaults.Planet.shared.MASS,
            repulsionRadiusDp: DeponderDefaults.Planet.shared.REPULSION_RADIUS_DP,
            elasticity: DeponderDefaults.Planet.shared.ELASTICITY,
            pinned: false
        )
    }

    public convenience init(pinned: Bool) {
        self.init(
            mass: DeponderDefaults.Planet.shared.MASS,
            repulsionRadiusDp: DeponderDefaults.Planet.shared.REPULSION_RADIUS_DP,
            elasticity: DeponderDefaults.Planet.shared.ELASTICITY,
            pinned: pinned
        )
    }

    public static func fromPreset(
        _ preset: DeponderDefaults.Preset,
        pinned: Bool = false
    ) -> PlanetPhysics {
        Companion.shared.fromPreset(preset: preset, pinned: pinned)
    }

    public static func make(
        mass: Float = DeponderDefaults.Planet.shared.MASS,
        repulsionRadiusDp: Float = DeponderDefaults.Planet.shared.REPULSION_RADIUS_DP,
        elasticity: Float = DeponderDefaults.Planet.shared.ELASTICITY,
        pinned: Bool = false
    ) -> PlanetPhysics {
        PlanetPhysics(
            mass: mass,
            repulsionRadiusDp: repulsionRadiusDp,
            elasticity: elasticity,
            pinned: pinned
        )
    }
}

extension RubberPhysics {
    public static var `default`: RubberPhysics {
        Companion.shared.Default
    }

    public convenience init() {
        self.init(
            elasticity: DeponderDefaults.Rubber.shared.ELASTICITY,
            naturalLengthDp: DeponderDefaults.shared.AUTO_LENGTH
        )
    }

    public static func make(
        elasticity: Float = DeponderDefaults.Rubber.shared.ELASTICITY,
        naturalLengthDp: Float = DeponderDefaults.Rubber.shared.NATURAL_LENGTH_DP
    ) -> RubberPhysics {
        RubberPhysics(elasticity: elasticity, naturalLengthDp: naturalLengthDp)
    }

    public static func auto(
        contentPaddingDp: Float = DeponderDefaults.Rubber.shared.CONTENT_PADDING_DP,
        minLengthDp: Float = DeponderDefaults.Rubber.shared.NATURAL_LENGTH_DP,
        maxLengthDp: Float = DeponderDefaults.shared.INFINITE_RANGE,
        elasticity: Float = DeponderDefaults.Rubber.shared.ELASTICITY
    ) -> RubberPhysics {
        Companion.shared.auto(
            contentPaddingDp: contentPaddingDp,
            minLengthDp: minLengthDp,
            maxLengthDp: maxLengthDp,
            elasticity: elasticity
        )
    }

    public static func fixed(
        naturalLengthDp: Float,
        elasticity: Float = DeponderDefaults.Rubber.shared.ELASTICITY
    ) -> RubberPhysics {
        Companion.shared.fixed(naturalLengthDp: naturalLengthDp, elasticity: elasticity)
    }

    public static func fromPreset(_ preset: DeponderDefaults.Preset) -> RubberPhysics {
        Companion.shared.fromPreset(preset: preset)
    }
}

extension DeponderRubberInsets {
    public static var `default`: DeponderRubberInsets {
        Companion.shared.Default
    }

    public static var none: DeponderRubberInsets {
        Companion.shared.None
    }

    public static func contour(
        gapDp: Float = DeponderDefaults.Rubber.shared.DEFAULT_ENDPOINT_GAP_DP
    ) -> DeponderRubberInsets {
        Companion.shared.contour(gapDp: gapDp)
    }

    public static func contour(startGapDp: Float, endGapDp: Float) -> DeponderRubberInsets {
        Companion.shared.contour(startGapDp: startGapDp, endGapDp: endGapDp)
    }

    public static func fixed(insetDp: Float) -> DeponderRubberInsets {
        Companion.shared.fixed(insetDp: insetDp)
    }

    public static func fixed(startDp: Float, endDp: Float) -> DeponderRubberInsets {
        Companion.shared.fixed(startDp: startDp, endDp: endDp)
    }

    public static func startOnly(
        gapDp: Float = DeponderDefaults.Rubber.shared.DEFAULT_ENDPOINT_GAP_DP
    ) -> DeponderRubberInsets {
        Companion.shared.startOnly(gapDp: gapDp)
    }

    public static func endOnly(
        gapDp: Float = DeponderDefaults.Rubber.shared.DEFAULT_ENDPOINT_GAP_DP
    ) -> DeponderRubberInsets {
        Companion.shared.endOnly(gapDp: gapDp)
    }
}

extension SimulationWorld {
    public convenience init(
        physics: RootPhysics = .default,
        density: Float = 1.0
    ) {
        self.init(environment: physics.toEnvironmentSpec(density: density))
    }
}
