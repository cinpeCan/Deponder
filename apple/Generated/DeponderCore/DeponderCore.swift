@_exported import ExportedKotlinPackages
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
internal import SharedBridge_DeponderCore

public typealias DeponderArrowMode = ExportedKotlinPackages.com.cinpe.deponder.core.DeponderArrowMode
public typealias DeponderDefaults = ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults
public typealias DeponderRubberInsets = ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets
public typealias EdgeKey = ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey
public typealias EnvironmentSpec = ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec
@available(*, deprecated, message: "Unused carrier retained for 0.6.0 compatibility; use EdgeKey for undirected edge keys or RubberSpec for link specifications.")
public typealias NodePair = ExportedKotlinPackages.com.cinpe.deponder.core.NodePair
public typealias PlanetBody = ExportedKotlinPackages.com.cinpe.deponder.core.PlanetBody
public typealias PlanetPhysics = ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics
public typealias PlanetSpec = ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec
public typealias RootPhysics = ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics
public typealias RubberPhysics = ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics
public typealias RubberPose = ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose
public typealias RubberSpec = ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec
public typealias SimulationWorld = ExportedKotlinPackages.com.cinpe.deponder.core.SimulationWorld
public typealias Vec2 = ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
extension ExportedKotlinPackages.com.cinpe.deponder.core {
    public enum DeponderArrowMode: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case NONE
        case END
        case START
        case BOTH
        public var description: Swift.String {
            get {
                switch self {
                case .NONE: "NONE"
                case .END: "END"
                case .START: "START"
                case .BOTH: "BOTH"
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .NONE: 0
                case .END: 1
                case .START: 2
                case .BOTH: 3
                }
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "NONE": self = .NONE
            case "END": self = .END
            case "START": self = .START
            case "BOTH": self = .BOTH
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<4 ~= rawValue else { return nil }
            self = DeponderArrowMode.allCases[Int(rawValue)]
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch com_cinpe_deponder_core_DeponderArrowMode_ordinal(__externalRCRefUnsafe) {
            case 0: self = .NONE
            case 1: self = .END
            case 2: self = .START
            case 3: self = .BOTH
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .NONE: com_cinpe_deponder_core_DeponderArrowMode_NONE()
            case .END: com_cinpe_deponder_core_DeponderArrowMode_END()
            case .START: com_cinpe_deponder_core_DeponderArrowMode_START()
            case .BOTH: com_cinpe_deponder_core_DeponderArrowMode_BOTH()
            }
        }
    }
    public final class DeponderDefaults: KotlinRuntime.KotlinBase {
        public final class Planet: KotlinRuntime.KotlinBase {
            @available(*, deprecated, message: "No-op since 0.6.1: implicit degree-based radius inflation has been retired so repulsion strictly honors PlanetPhysics.repulsionRadiusDp.")
            public var DEGREE_RADIUS_INCREMENT_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Planet_DEGREE_RADIUS_INCREMENT_DP_get(self.__externalRCRef())
                }
            }
            public var ELASTICITY: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Planet_ELASTICITY_get(self.__externalRCRef())
                }
            }
            public var INFINITE_CHARGE_STRENGTH: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Planet_INFINITE_CHARGE_STRENGTH_get(self.__externalRCRef())
                }
            }
            public var MASS: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Planet_MASS_get(self.__externalRCRef())
                }
            }
            public var REPULSION_RADIUS_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Planet_REPULSION_RADIUS_DP_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Planet {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Planet.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Planet_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public final class Preset: KotlinRuntime.KotlinBase {
            public var airDamping: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_airDamping_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing has been retired; use the 6-parameter Preset constructor and configure airDamping instead.")
            public var alphaDecay: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_alphaDecay_get(self.__externalRCRef())
                }
            }
            public var planetElasticity: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_planetElasticity_get(self.__externalRCRef())
                }
            }
            public var planetRepulsionDp: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_planetRepulsionDp_get(self.__externalRCRef())
                }
            }
            public var rubberElasticity: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_rubberElasticity_get(self.__externalRCRef())
                }
            }
            public var rubberNaturalLengthDp: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_rubberNaturalLengthDp_get(self.__externalRCRef())
                }
            }
            public var wallRepulsionDp: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Preset_wallRepulsionDp_get(self.__externalRCRef())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public init(
                airDamping: Swift.Float,
                wallRepulsionDp: Swift.Float,
                planetRepulsionDp: Swift.Float,
                planetElasticity: Swift.Float,
                rubberNaturalLengthDp: Swift.Float,
                rubberElasticity: Swift.Float
            ) {
                let __kt = com_cinpe_deponder_core_DeponderDefaults_Preset_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
                { com_cinpe_deponder_core_DeponderDefaults_Preset_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(__kt, airDamping, wallRepulsionDp, planetRepulsionDp, planetElasticity, rubberNaturalLengthDp, rubberElasticity); return () }()
            }
            public init(
                airDamping: Swift.Float,
                wallRepulsionDp: Swift.Float,
                planetRepulsionDp: Swift.Float,
                planetElasticity: Swift.Float,
                rubberNaturalLengthDp: Swift.Float,
                rubberElasticity: Swift.Float,
                alphaDecay: Swift.Float
            ) {
                let __kt = com_cinpe_deponder_core_DeponderDefaults_Preset_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
                { com_cinpe_deponder_core_DeponderDefaults_Preset_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(__kt, airDamping, wallRepulsionDp, planetRepulsionDp, planetElasticity, rubberNaturalLengthDp, rubberElasticity, alphaDecay); return () }()
            }
            public static func ==(
                this: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset,
                other: (any KotlinRuntimeSupport._KotlinBridgeable)?
            ) -> Swift.Bool {
                this.equals(other: other)
            }
            public func copy(
                airDamping: Swift.Float,
                wallRepulsionDp: Swift.Float,
                planetRepulsionDp: Swift.Float,
                planetElasticity: Swift.Float,
                rubberNaturalLengthDp: Swift.Float,
                rubberElasticity: Swift.Float,
                alphaDecay: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Preset_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), airDamping, wallRepulsionDp, planetRepulsionDp, planetElasticity, rubberNaturalLengthDp, rubberElasticity, alphaDecay))
            }
            public func equals(
                other: (any KotlinRuntimeSupport._KotlinBridgeable)?
            ) -> Swift.Bool {
                return com_cinpe_deponder_core_DeponderDefaults_Preset_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
            }
            public func hashCode() -> Swift.Int32 {
                return com_cinpe_deponder_core_DeponderDefaults_Preset_hashCode(self.__externalRCRef())
            }
            public func toString() -> Swift.String {
                return com_cinpe_deponder_core_DeponderDefaults_Preset_toString(self.__externalRCRef())
            }
        }
        public final class Presets: KotlinRuntime.KotlinBase {
            public var Balanced: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Presets_Balanced_get(self.__externalRCRef()))
                }
            }
            public var Crisp: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Presets_Crisp_get(self.__externalRCRef()))
                }
            }
            public var GalaxyInfinite: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Presets_GalaxyInfinite_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Presets {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Presets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Presets_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public final class Root: KotlinRuntime.KotlinBase {
            public var AIR_DAMPING: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_AIR_DAMPING_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing has been retired in favor of pure Newtonian mechanics; configure RootPhysics(airDamping = ...) instead (0f for perpetual motion).")
            public var COOLING_ENABLED: Swift.Bool {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_COOLING_ENABLED_get(self.__externalRCRef())
                }
            }
            public var INIT_SCALE: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_INIT_SCALE_get(self.__externalRCRef())
                }
            }
            public var MAX_SCALE: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_MAX_SCALE_get(self.__externalRCRef())
                }
            }
            public var MIN_SCALE: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_MIN_SCALE_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "Topological unfolding is a transient one-shot action; invoke DeponderState.unfold(), DeponderController.unfold(), or SimulationWorld.unfold() instead.")
            public var UNFOLD_ENABLED: Swift.Bool {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_UNFOLD_ENABLED_get(self.__externalRCRef())
                }
            }
            public var WALL_ELASTICITY_BOTTOM: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_BOTTOM_get(self.__externalRCRef())
                }
            }
            public var WALL_ELASTICITY_END: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_END_get(self.__externalRCRef())
                }
            }
            public var WALL_ELASTICITY_START: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_START_get(self.__externalRCRef())
                }
            }
            public var WALL_ELASTICITY_TOP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_TOP_get(self.__externalRCRef())
                }
            }
            public var WALL_REPULSION_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Root_WALL_REPULSION_DP_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Root {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Root.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Root_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public final class Rubber: KotlinRuntime.KotlinBase {
            public var CONTENT_PADDING_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_CONTENT_PADDING_DP_get(self.__externalRCRef())
                }
            }
            public var DEFAULT_ARROW_SIZE_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_ARROW_SIZE_DP_get(self.__externalRCRef())
                }
            }
            public var DEFAULT_ENDPOINT_GAP_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_ENDPOINT_GAP_DP_get(self.__externalRCRef())
                }
            }
            public var DEFAULT_LABEL_OFFSET_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_LABEL_OFFSET_DP_get(self.__externalRCRef())
                }
            }
            public var DEFAULT_LABEL_TEXT_SIZE_SP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_LABEL_TEXT_SIZE_SP_get(self.__externalRCRef())
                }
            }
            public var DEFAULT_THICKNESS_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_THICKNESS_DP_get(self.__externalRCRef())
                }
            }
            public var ELASTICITY: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_ELASTICITY_get(self.__externalRCRef())
                }
            }
            public var NATURAL_LENGTH_DP: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Rubber_NATURAL_LENGTH_DP_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Rubber {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Rubber.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Rubber_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public final class Simulation: KotlinRuntime.KotlinBase {
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.")
            public var ALPHA_DECAY: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_ALPHA_DECAY_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.")
            public var ALPHA_MIN: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_ALPHA_MIN_get(self.__externalRCRef())
                }
            }
            public var BARNES_HUT_THETA: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_BARNES_HUT_THETA_get(self.__externalRCRef())
                }
            }
            public var COINCIDENT_EPSILON_SQ: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_COINCIDENT_EPSILON_SQ_get(self.__externalRCRef())
                }
            }
            public var COINCIDENT_PERTURBATION_RADIUS_PX: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_COINCIDENT_PERTURBATION_RADIUS_PX_get(self.__externalRCRef())
                }
            }
            public var COULOMB_SOFTENING_SQ: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_COULOMB_SOFTENING_SQ_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.")
            public var INITIAL_ALPHA: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_INITIAL_ALPHA_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; velocity decay is governed solely by RootPhysics.airDamping.")
            public var INTERACTION_ALPHA: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_INTERACTION_ALPHA_get(self.__externalRCRef())
                }
            }
            public var MAX_DT_SECONDS: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_DT_SECONDS_get(self.__externalRCRef())
                }
            }
            public var MAX_SUBSTEPS: Swift.Int32 {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_SUBSTEPS_get(self.__externalRCRef())
                }
            }
            public var MAX_SUBSTEP_DT_SECONDS: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_SUBSTEP_DT_SECONDS_get(self.__externalRCRef())
                }
            }
            public var PARALLEL_DISPATCH_THRESHOLD: Swift.Int32 {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_PARALLEL_DISPATCH_THRESHOLD_get(self.__externalRCRef())
                }
            }
            public var PARALLEL_WORKER_CHUNKS: Swift.Int32 {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_PARALLEL_WORKER_CHUNKS_get(self.__externalRCRef())
                }
            }
            public var QUADTREE_NODE_THRESHOLD: Swift.Int32 {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_QUADTREE_NODE_THRESHOLD_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing (alpha cooling) has been retired; integration uses true physical dtSeconds with MAX_SUBSTEP_DT_SECONDS.")
            public var REFERENCE_DT_SECONDS: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_REFERENCE_DT_SECONDS_get(self.__externalRCRef())
                }
            }
            public var REST_ACCEL_PX_PER_SEC_SQ: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_REST_ACCEL_PX_PER_SEC_SQ_get(self.__externalRCRef())
                }
            }
            public var REST_SPEED_PX_PER_SEC: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_REST_SPEED_PX_PER_SEC_get(self.__externalRCRef())
                }
            }
            public var SETTLE_DURATION_SECONDS: Swift.Float {
                get {
                    return com_cinpe_deponder_core_DeponderDefaults_Simulation_SETTLE_DURATION_SECONDS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Simulation {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Simulation.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_Simulation_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public var AUTO_LENGTH: Swift.Float {
            get {
                return com_cinpe_deponder_core_DeponderDefaults_AUTO_LENGTH_get(self.__externalRCRef())
            }
        }
        public var INFINITE_RANGE: Swift.Float {
            get {
                return com_cinpe_deponder_core_DeponderDefaults_INFINITE_RANGE_get(self.__externalRCRef())
            }
        }
        public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderDefaults_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class DeponderRubberInsets: KotlinRuntime.KotlinBase {
        public enum Mode: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
            case NONE
            case CONTOUR
            case FIXED
            public var description: Swift.String {
                get {
                    switch self {
                    case .NONE: "NONE"
                    case .CONTOUR: "CONTOUR"
                    case .FIXED: "FIXED"
                    }
                }
            }
            public var rawValue: Swift.Int32 {
                get {
                    switch self {
                    case .NONE: 0
                    case .CONTOUR: 1
                    case .FIXED: 2
                    }
                }
            }
            public init?(
                _ description: Swift.String
            ) {
                switch description {
                case "NONE": self = .NONE
                case "CONTOUR": self = .CONTOUR
                case "FIXED": self = .FIXED
                default: return nil
                }
            }
            public init?(
                rawValue: Swift.Int32
            ) {
                guard 0..<3 ~= rawValue else { return nil }
                self = Mode.allCases[Int(rawValue)]
            }
            public init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                switch com_cinpe_deponder_core_DeponderRubberInsets_Mode_ordinal(__externalRCRefUnsafe) {
                case 0: self = .NONE
                case 1: self = .CONTOUR
                case 2: self = .FIXED
                default: fatalError()
                }
            }
            public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
                return switch self {
                case .NONE: com_cinpe_deponder_core_DeponderRubberInsets_Mode_NONE()
                case .CONTOUR: com_cinpe_deponder_core_DeponderRubberInsets_Mode_CONTOUR()
                case .FIXED: com_cinpe_deponder_core_DeponderRubberInsets_Mode_FIXED()
                }
            }
        }
        public final class Companion: KotlinRuntime.KotlinBase {
            public var Default: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_Default_get(self.__externalRCRef()))
                }
            }
            public var None: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_None_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func contour(
                gapDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_contour__TypesOfArguments__Swift_Float__(self.__externalRCRef(), gapDp))
            }
            public func contour(
                startGapDp: Swift.Float,
                endGapDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_contour__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), startGapDp, endGapDp))
            }
            public func endOnly(
                gapDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_endOnly__TypesOfArguments__Swift_Float__(self.__externalRCRef(), gapDp))
            }
            public func fixed(
                insetDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_fixed__TypesOfArguments__Swift_Float__(self.__externalRCRef(), insetDp))
            }
            public func fixed(
                startDp: Swift.Float,
                endDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_fixed__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), startDp, endDp))
            }
            public func none() -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_none(self.__externalRCRef()))
            }
            public func startOnly(
                gapDp: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_Companion_startOnly__TypesOfArguments__Swift_Float__(self.__externalRCRef(), gapDp))
            }
        }
        public var endMode: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Mode {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Mode(__externalRCRefUnsafe: com_cinpe_deponder_core_DeponderRubberInsets_endMode_get(self.__externalRCRef()), options: .asBestFittingWrapper)
            }
        }
        public var endValueDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_DeponderRubberInsets_endValueDp_get(self.__externalRCRef())
            }
        }
        public var hasEndSafeArea: Swift.Bool {
            get {
                return com_cinpe_deponder_core_DeponderRubberInsets_hasEndSafeArea_get(self.__externalRCRef())
            }
        }
        public var hasStartSafeArea: Swift.Bool {
            get {
                return com_cinpe_deponder_core_DeponderRubberInsets_hasStartSafeArea_get(self.__externalRCRef())
            }
        }
        public var startMode: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Mode {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.Mode(__externalRCRefUnsafe: com_cinpe_deponder_core_DeponderRubberInsets_startMode_get(self.__externalRCRef()), options: .asBestFittingWrapper)
            }
        }
        public var startValueDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_DeponderRubberInsets_startValueDp_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_DeponderRubberInsets_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_DeponderRubberInsets_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_DeponderRubberInsets_toString(self.__externalRCRef())
        }
        public func withEndContour(
            gapDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withEndContour__TypesOfArguments__Swift_Float__(self.__externalRCRef(), gapDp))
        }
        public func withEndFixed(
            insetDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withEndFixed__TypesOfArguments__Swift_Float__(self.__externalRCRef(), insetDp))
        }
        public func withEndNone() -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withEndNone(self.__externalRCRef()))
        }
        public func withStartContour(
            gapDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withStartContour__TypesOfArguments__Swift_Float__(self.__externalRCRef(), gapDp))
        }
        public func withStartFixed(
            insetDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withStartFixed__TypesOfArguments__Swift_Float__(self.__externalRCRef(), insetDp))
        }
        public func withStartNone() -> ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets {
            return ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_DeponderRubberInsets_withStartNone(self.__externalRCRef()))
        }
    }
    public final class EdgeKey: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_EdgeKey_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func encode(
                sId: Swift.String,
                eId: Swift.String
            ) -> Swift.String {
                return com_cinpe_deponder_core_EdgeKey_Companion_encode__TypesOfArguments__Swift_String_Swift_String__(self.__externalRCRef(), sId, eId)
            }
        }
        public var encoded: Swift.String {
            get {
                return com_cinpe_deponder_core_EdgeKey_encoded_get(self.__externalRCRef())
            }
        }
        public var first: Swift.String {
            get {
                return com_cinpe_deponder_core_EdgeKey_first_get(self.__externalRCRef())
            }
        }
        public var second: Swift.String {
            get {
                return com_cinpe_deponder_core_EdgeKey_second_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            a: Swift.String,
            b: Swift.String
        ) {
            let __kt = com_cinpe_deponder_core_EdgeKey_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_EdgeKey_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(__kt, a, b); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_EdgeKey_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_EdgeKey_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_EdgeKey_toString(self.__externalRCRef())
        }
    }
    public final class EnvironmentSpec: KotlinRuntime.KotlinBase {
        public var airDamping: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_airDamping_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always 0f. Simulated annealing has been retired; configure airDamping instead.")
        public var alphaDecay: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_alphaDecay_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always 0f. Simulated annealing has been retired; settling uses kinetic/acceleration thresholds instead.")
        public var alphaMin: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_alphaMin_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always false. Simulated annealing has been retired; configure airDamping instead.")
        public var coolingEnabled: Swift.Bool {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_coolingEnabled_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always 0f. Implicit degree-based radius inflation has been retired; configure PlanetPhysics.repulsionRadiusDp explicitly.")
        public var degreeRadiusIncrementPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_degreeRadiusIncrementPx_get(self.__externalRCRef())
            }
        }
        public var infiniteChargeStrength: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_infiniteChargeStrength_get(self.__externalRCRef())
            }
        }
        public var initScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_initScale_get(self.__externalRCRef())
            }
        }
        public var maxScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_maxScale_get(self.__externalRCRef())
            }
        }
        public var minScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_minScale_get(self.__externalRCRef())
            }
        }
        public var restAccelPxPerSecSq: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_restAccelPxPerSecSq_get(self.__externalRCRef())
            }
        }
        public var restSpeedPxPerSec: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_restSpeedPxPerSec_get(self.__externalRCRef())
            }
        }
        public var settleDurationSeconds: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_settleDurationSeconds_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Topological unfolding is a transient one-shot action rather than a persistent environment property; invoke SimulationWorld.unfold() instead.")
        public var unfoldEnabled: Swift.Bool {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_unfoldEnabled_get(self.__externalRCRef())
            }
        }
        public var wallElasticityBottom: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_wallElasticityBottom_get(self.__externalRCRef())
            }
        }
        public var wallElasticityEnd: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_wallElasticityEnd_get(self.__externalRCRef())
            }
        }
        public var wallElasticityStart: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_wallElasticityStart_get(self.__externalRCRef())
            }
        }
        public var wallElasticityTop: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_wallElasticityTop_get(self.__externalRCRef())
            }
        }
        public var wallRepulsionPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_EnvironmentSpec_wallRepulsionPx_get(self.__externalRCRef())
            }
        }
        public init() {
            let __kt = com_cinpe_deponder_core_EnvironmentSpec_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_EnvironmentSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionPx: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            degreeRadiusIncrementPx: Swift.Float,
            infiniteChargeStrength: Swift.Float,
            coolingEnabled: Swift.Bool,
            alphaDecay: Swift.Float,
            alphaMin: Swift.Float,
            restSpeedPxPerSec: Swift.Float,
            restAccelPxPerSecSq: Swift.Float,
            settleDurationSeconds: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_EnvironmentSpec_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_EnvironmentSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(__kt, initScale, maxScale, minScale, airDamping, wallRepulsionPx, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, degreeRadiusIncrementPx, infiniteChargeStrength, coolingEnabled, alphaDecay, alphaMin, restSpeedPxPerSec, restAccelPxPerSecSq, settleDurationSeconds); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionPx: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            degreeRadiusIncrementPx: Swift.Float,
            infiniteChargeStrength: Swift.Float,
            coolingEnabled: Swift.Bool,
            alphaDecay: Swift.Float,
            alphaMin: Swift.Float,
            restSpeedPxPerSec: Swift.Float,
            restAccelPxPerSecSq: Swift.Float,
            settleDurationSeconds: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_EnvironmentSpec_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), initScale, maxScale, minScale, airDamping, wallRepulsionPx, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, degreeRadiusIncrementPx, infiniteChargeStrength, coolingEnabled, alphaDecay, alphaMin, restSpeedPxPerSec, restAccelPxPerSecSq, settleDurationSeconds))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_EnvironmentSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_EnvironmentSpec_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_EnvironmentSpec_toString(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Topological unfolding is a transient one-shot action rather than a persistent environment property; invoke SimulationWorld.unfold() instead.")
        public func withUnfold(
            unfoldEnabled: Swift.Bool
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_EnvironmentSpec_withUnfold__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), unfoldEnabled))
        }
    }
    @available(*, deprecated, message: "Unused carrier retained for 0.6.0 compatibility; use EdgeKey for undirected edge keys or RubberSpec for link specifications.")
    public final class NodePair: KotlinRuntime.KotlinBase {
        public var endId: Swift.String {
            get {
                return com_cinpe_deponder_core_NodePair_endId_get(self.__externalRCRef())
            }
        }
        public var startId: Swift.String {
            get {
                return com_cinpe_deponder_core_NodePair_startId_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            startId: Swift.String,
            endId: Swift.String
        ) {
            let __kt = com_cinpe_deponder_core_NodePair_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_NodePair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(__kt, startId, endId); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.NodePair,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            startId: Swift.String,
            endId: Swift.String
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.NodePair {
            return ExportedKotlinPackages.com.cinpe.deponder.core.NodePair.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_NodePair_copy__TypesOfArguments__Swift_String_Swift_String__(self.__externalRCRef(), startId, endId))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_NodePair_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_NodePair_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_NodePair_toString(self.__externalRCRef())
        }
    }
    public final class PlanetBody: KotlinRuntime.KotlinBase {
        public var degree: Swift.Int32 {
            get {
                return com_cinpe_deponder_core_PlanetBody_degree_get(self.__externalRCRef())
            }
        }
        public var dx: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_dx_get(self.__externalRCRef())
            }
        }
        public var dy: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_dy_get(self.__externalRCRef())
            }
        }
        public var halfHeightPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_halfHeightPx_get(self.__externalRCRef())
            }
        }
        public var halfWidthPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_halfWidthPx_get(self.__externalRCRef())
            }
        }
        public var isPressed: Swift.Bool {
            get {
                return com_cinpe_deponder_core_PlanetBody_isPressed_get(self.__externalRCRef())
            }
        }
        public var spec: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetBody_spec_get(self.__externalRCRef()))
            }
            @available(*, deprecated, message: "No-op since 0.6.1: direct mutation of PlanetBody.spec bypasses topology validation and is ignored. Update node physics via Modifier.deponderPlanet, view.deponderPlanet, or SimulationWorld.upsertPlanet(...) instead.")
            set {
                return { com_cinpe_deponder_core_PlanetBody_spec_set__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__(self.__externalRCRef(), newValue.__externalRCRef()); return () }()
            }
        }
        public var vx: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_vx_get(self.__externalRCRef())
            }
        }
        public var vy: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_vy_get(self.__externalRCRef())
            }
        }
        public var x: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_x_get(self.__externalRCRef())
            }
        }
        public var y: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetBody_y_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class PlanetPhysics: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func fromPreset(
                preset: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(self.__externalRCRef(), preset.__externalRCRef()))
            }
            public func fromPreset(
                preset: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset,
                pinned: Swift.Bool
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset_Swift_Bool__(self.__externalRCRef(), preset.__externalRCRef(), pinned))
            }
        }
        public var elasticity: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetPhysics_elasticity_get(self.__externalRCRef())
            }
        }
        public var mass: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetPhysics_mass_get(self.__externalRCRef())
            }
        }
        public var pinned: Swift.Bool {
            get {
                return com_cinpe_deponder_core_PlanetPhysics_pinned_get(self.__externalRCRef())
            }
        }
        public var repulsionRadiusDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetPhysics_repulsionRadiusDp_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            mass: Swift.Float,
            repulsionRadiusDp: Swift.Float,
            elasticity: Swift.Float,
            pinned: Swift.Bool
        ) {
            let __kt = com_cinpe_deponder_core_PlanetPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_PlanetPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(__kt, mass, repulsionRadiusDp, elasticity, pinned); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            mass: Swift.Float,
            repulsionRadiusDp: Swift.Float,
            elasticity: Swift.Float,
            pinned: Swift.Bool
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(self.__externalRCRef(), mass, repulsionRadiusDp, elasticity, pinned))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_PlanetPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_PlanetPhysics_hashCode(self.__externalRCRef())
        }
        public func toPlanetSpec(
            id: Swift.String,
            density: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_toPlanetSpec__TypesOfArguments__Swift_String_Swift_Float__(self.__externalRCRef(), id, density))
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_PlanetPhysics_toString(self.__externalRCRef())
        }
        public func withElasticity(
            elasticity: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_withElasticity__TypesOfArguments__Swift_Float__(self.__externalRCRef(), elasticity))
        }
        public func withMass(
            mass: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_withMass__TypesOfArguments__Swift_Float__(self.__externalRCRef(), mass))
        }
        public func withPinned(
            pinned: Swift.Bool
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_withPinned__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), pinned))
        }
        public func withRepulsionRadiusDp(
            repulsionRadiusDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetPhysics_withRepulsionRadiusDp__TypesOfArguments__Swift_Float__(self.__externalRCRef(), repulsionRadiusDp))
        }
    }
    public final class PlanetSpec: KotlinRuntime.KotlinBase {
        public var elasticity: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetSpec_elasticity_get(self.__externalRCRef())
            }
        }
        public var id: Swift.String {
            get {
                return com_cinpe_deponder_core_PlanetSpec_id_get(self.__externalRCRef())
            }
        }
        public var mass: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetSpec_mass_get(self.__externalRCRef())
            }
        }
        public var pinned: Swift.Bool {
            get {
                return com_cinpe_deponder_core_PlanetSpec_pinned_get(self.__externalRCRef())
            }
        }
        public var repulsionRadiusPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_PlanetSpec_repulsionRadiusPx_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            id: Swift.String,
            mass: Swift.Float,
            repulsionRadiusPx: Swift.Float,
            elasticity: Swift.Float,
            pinned: Swift.Bool
        ) {
            let __kt = com_cinpe_deponder_core_PlanetSpec_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_PlanetSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(__kt, id, mass, repulsionRadiusPx, elasticity, pinned); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            id: Swift.String,
            mass: Swift.Float,
            repulsionRadiusPx: Swift.Float,
            elasticity: Swift.Float,
            pinned: Swift.Bool
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_PlanetSpec_copy__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(self.__externalRCRef(), id, mass, repulsionRadiusPx, elasticity, pinned))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_PlanetSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_PlanetSpec_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_PlanetSpec_toString(self.__externalRCRef())
        }
    }
    public final class RootPhysics: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func fromPreset(
                preset: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(self.__externalRCRef(), preset.__externalRCRef()))
            }
        }
        public var airDamping: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_airDamping_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always 0f. Simulated annealing has been retired; configure airDamping instead.")
        public var alphaDecay: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_alphaDecay_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always false. Simulated annealing has been retired; configure airDamping instead (0f for perpetual motion).")
        public var coolingEnabled: Swift.Bool {
            get {
                return com_cinpe_deponder_core_RootPhysics_coolingEnabled_get(self.__externalRCRef())
            }
        }
        public var initScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_initScale_get(self.__externalRCRef())
            }
        }
        public var maxScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_maxScale_get(self.__externalRCRef())
            }
        }
        public var minScale: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_minScale_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Topological unfolding is a transient one-shot action rather than a persistent physics property; invoke state.unfold() (Compose) or controller.unfold() (Android View) instead.")
        public var unfoldEnabled: Swift.Bool {
            get {
                return com_cinpe_deponder_core_RootPhysics_unfoldEnabled_get(self.__externalRCRef())
            }
        }
        public var wallElasticityBottom: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_wallElasticityBottom_get(self.__externalRCRef())
            }
        }
        public var wallElasticityEnd: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_wallElasticityEnd_get(self.__externalRCRef())
            }
        }
        public var wallElasticityStart: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_wallElasticityStart_get(self.__externalRCRef())
            }
        }
        public var wallElasticityTop: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_wallElasticityTop_get(self.__externalRCRef())
            }
        }
        public var wallRepulsionDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_RootPhysics_wallRepulsionDp_get(self.__externalRCRef())
            }
        }
        public init() {
            let __kt = com_cinpe_deponder_core_RootPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_RootPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(__kt, initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom); return () }()
        }
        @available(*, deprecated, message: "alphaDecay is a no-op since 0.6.1; use the 9-parameter RootPhysics constructor and configure airDamping instead.")
        public init(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            alphaDecay: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_RootPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(__kt, initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, alphaDecay); return () }()
        }
        @available(*, deprecated, message: "coolingEnabled and alphaDecay are no-ops since 0.6.1; use the 9-parameter RootPhysics constructor and configure airDamping instead (0f for perpetual motion).")
        public init(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            coolingEnabled: Swift.Bool,
            alphaDecay: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_RootPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float__(__kt, initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, coolingEnabled, alphaDecay); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom))
        }
        @available(*, deprecated, message: "alphaDecay is a no-op since 0.6.1; use the 9-parameter copy(...) overload instead.")
        public func copy(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            alphaDecay: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, alphaDecay))
        }
        @available(*, deprecated, message: "coolingEnabled and alphaDecay are no-ops since 0.6.1; use the 9-parameter copy(...) overload instead.")
        public func copy(
            initScale: Swift.Float,
            maxScale: Swift.Float,
            minScale: Swift.Float,
            airDamping: Swift.Float,
            wallRepulsionDp: Swift.Float,
            wallElasticityStart: Swift.Float,
            wallElasticityTop: Swift.Float,
            wallElasticityEnd: Swift.Float,
            wallElasticityBottom: Swift.Float,
            coolingEnabled: Swift.Bool,
            alphaDecay: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float__(self.__externalRCRef(), initScale, maxScale, minScale, airDamping, wallRepulsionDp, wallElasticityStart, wallElasticityTop, wallElasticityEnd, wallElasticityBottom, coolingEnabled, alphaDecay))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_RootPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_RootPhysics_hashCode(self.__externalRCRef())
        }
        public func toEnvironmentSpec(
            density: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_toEnvironmentSpec__TypesOfArguments__Swift_Float__(self.__externalRCRef(), density))
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_RootPhysics_toString(self.__externalRCRef())
        }
        public func withAirDamping(
            airDamping: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_withAirDamping__TypesOfArguments__Swift_Float__(self.__externalRCRef(), airDamping))
        }
        @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing has been retired; use withAirDamping(airDamping) instead (0f for perpetual motion).. Replacement: this")
        public func withCooling(
            coolingEnabled: Swift.Bool,
            alphaDecay: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_withCooling__TypesOfArguments__Swift_Bool_Swift_Float__(self.__externalRCRef(), coolingEnabled, alphaDecay))
        }
        public func withScaleBounds(
            initScale: Swift.Float,
            minScale: Swift.Float,
            maxScale: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_withScaleBounds__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), initScale, minScale, maxScale))
        }
        @available(*, deprecated, message: "Topological unfolding is a transient one-shot action rather than a persistent physics property; invoke state.unfold() (Compose) or controller.unfold() (Android View) instead.")
        public func withUnfold(
            unfoldEnabled: Swift.Bool
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_withUnfold__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), unfoldEnabled))
        }
        public func withWallRepulsionDp(
            wallRepulsionDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RootPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RootPhysics_withWallRepulsionDp__TypesOfArguments__Swift_Float__(self.__externalRCRef(), wallRepulsionDp))
        }
    }
    public final class RubberPhysics: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var Default: ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_Companion_Default_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func auto(
                contentPaddingDp: Swift.Float,
                minLengthDp: Swift.Float,
                maxLengthDp: Swift.Float,
                elasticity: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_Companion_auto__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), contentPaddingDp, minLengthDp, maxLengthDp, elasticity))
            }
            public func fixed(
                naturalLengthDp: Swift.Float,
                elasticity: Swift.Float
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_Companion_fixed__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), naturalLengthDp, elasticity))
            }
            public func fromPreset(
                preset: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderDefaults.Preset
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
                return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(self.__externalRCRef(), preset.__externalRCRef()))
            }
        }
        public var contentPaddingDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPhysics_contentPaddingDp_get(self.__externalRCRef())
            }
        }
        public var elasticity: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPhysics_elasticity_get(self.__externalRCRef())
            }
        }
        public var isAutoLength: Swift.Bool {
            get {
                return com_cinpe_deponder_core_RubberPhysics_isAutoLength_get(self.__externalRCRef())
            }
        }
        public var maxLengthDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPhysics_maxLengthDp_get(self.__externalRCRef())
            }
        }
        public var minLengthDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPhysics_minLengthDp_get(self.__externalRCRef())
            }
        }
        public var naturalLengthDp: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPhysics_naturalLengthDp_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            elasticity: Swift.Float,
            naturalLengthDp: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_RubberPhysics_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RubberPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float__(__kt, elasticity, naturalLengthDp); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            elasticity: Swift.Float,
            naturalLengthDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), elasticity, naturalLengthDp))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_RubberPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_RubberPhysics_hashCode(self.__externalRCRef())
        }
        public func resolveNaturalLengthDp(
            contentWidthDp: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPhysics_resolveNaturalLengthDp__TypesOfArguments__Swift_Float__(self.__externalRCRef(), contentWidthDp)
        }
        public func resolveNaturalLengthPx(
            density: Swift.Float,
            contentWidthPx: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPhysics_resolveNaturalLengthPx__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), density, contentWidthPx)
        }
        public func toRubberSpec(
            startId: Swift.String,
            endId: Swift.String,
            density: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_toRubberSpec__TypesOfArguments__Swift_String_Swift_String_Swift_Float__(self.__externalRCRef(), startId, endId, density))
        }
        public func toRubberSpec(
            startId: Swift.String,
            endId: Swift.String,
            density: Swift.Float,
            contentWidthPx: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_toRubberSpec__TypesOfArguments__Swift_String_Swift_String_Swift_Float_Swift_Float__(self.__externalRCRef(), startId, endId, density, contentWidthPx))
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_RubberPhysics_toString(self.__externalRCRef())
        }
        public func withAutoLength(
            contentPaddingDp: Swift.Float,
            minLengthDp: Swift.Float,
            maxLengthDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_withAutoLength__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), contentPaddingDp, minLengthDp, maxLengthDp))
        }
        public func withElasticity(
            elasticity: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_withElasticity__TypesOfArguments__Swift_Float__(self.__externalRCRef(), elasticity))
        }
        public func withNaturalLengthDp(
            naturalLengthDp: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPhysics.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPhysics_withNaturalLengthDp__TypesOfArguments__Swift_Float__(self.__externalRCRef(), naturalLengthDp))
        }
    }
    public final class RubberPose: KotlinRuntime.KotlinBase {
        public var centerDistance: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_centerDistance_get(self.__externalRCRef())
            }
        }
        public var centerX: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_centerX_get(self.__externalRCRef())
            }
        }
        public var centerY: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_centerY_get(self.__externalRCRef())
            }
        }
        public var directionX: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_directionX_get(self.__externalRCRef())
            }
        }
        public var directionY: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_directionY_get(self.__externalRCRef())
            }
        }
        public var dirty: Swift.Bool {
            get {
                return com_cinpe_deponder_core_RubberPose_dirty_get(self.__externalRCRef())
            }
        }
        public var endContourPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_endContourPx_get(self.__externalRCRef())
            }
        }
        public var isActive: Swift.Bool {
            get {
                return com_cinpe_deponder_core_RubberPose_isActive_get(self.__externalRCRef())
            }
        }
        public var rotationDeg: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_rotationDeg_get(self.__externalRCRef())
            }
        }
        public var scale: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_scale_get(self.__externalRCRef())
            }
        }
        public var spec: ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberPose_spec_get(self.__externalRCRef()))
            }
            @available(*, deprecated, message: "No-op since 0.6.1: direct mutation of RubberPose.spec bypasses spring topology indexing and is ignored. Update link physics via Modifier.deponderRubber, view.deponderRubber, or SimulationWorld.upsertRubber(...) instead.")
            set {
                return { com_cinpe_deponder_core_RubberPose_spec_set__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__(self.__externalRCRef(), newValue.__externalRCRef()); return () }()
            }
        }
        public var startContourPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberPose_startContourPx_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func localSpanOffsetX(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_localSpanOffsetX__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
        public func resolvedCenterX(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_resolvedCenterX__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
        public func resolvedCenterY(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_resolvedCenterY__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
        public func resolvedEndInsetPx(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_resolvedEndInsetPx__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
        public func resolvedLength(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_resolvedLength__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
        public func resolvedStartInsetPx(
            insets: ExportedKotlinPackages.com.cinpe.deponder.core.DeponderRubberInsets,
            density: Swift.Float
        ) -> Swift.Float {
            return com_cinpe_deponder_core_RubberPose_resolvedStartInsetPx__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(self.__externalRCRef(), insets.__externalRCRef(), density)
        }
    }
    public final class RubberSpec: KotlinRuntime.KotlinBase {
        public var elasticity: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberSpec_elasticity_get(self.__externalRCRef())
            }
        }
        public var endId: Swift.String {
            get {
                return com_cinpe_deponder_core_RubberSpec_endId_get(self.__externalRCRef())
            }
        }
        public var key: ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberSpec_key_get(self.__externalRCRef()))
            }
        }
        public var naturalLengthPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_RubberSpec_naturalLengthPx_get(self.__externalRCRef())
            }
        }
        public var startId: Swift.String {
            get {
                return com_cinpe_deponder_core_RubberSpec_startId_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            startId: Swift.String,
            endId: Swift.String,
            elasticity: Swift.Float,
            naturalLengthPx: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_RubberSpec_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_RubberSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String_Swift_Float_Swift_Float__(__kt, startId, endId, elasticity, naturalLengthPx); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            startId: Swift.String,
            endId: Swift.String,
            elasticity: Swift.Float,
            naturalLengthPx: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_RubberSpec_copy__TypesOfArguments__Swift_String_Swift_String_Swift_Float_Swift_Float__(self.__externalRCRef(), startId, endId, elasticity, naturalLengthPx))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_RubberSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_RubberSpec_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_RubberSpec_toString(self.__externalRCRef())
        }
    }
    public final class SimulationWorld: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.SimulationWorld.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.SimulationWorld.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_SimulationWorld_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func computeWallForce(
                px: Swift.Float,
                py: Swift.Float,
                scale: Swift.Float,
                width: Swift.Float,
                height: Swift.Float,
                env: ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec
            ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
                return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_SimulationWorld_Companion_computeWallForce__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec__(self.__externalRCRef(), px, py, scale, width, height, env.__externalRCRef()))
            }
        }
        @available(*, deprecated, message: "No-op since 0.6.1: always 1.0f. Simulated annealing has been retired; configure airDamping instead.")
        public var alpha: Swift.Float {
            get {
                return com_cinpe_deponder_core_SimulationWorld_alpha_get(self.__externalRCRef())
            }
        }
        public var bodies: [ExportedKotlinPackages.com.cinpe.deponder.core.PlanetBody] {
            get {
                return com_cinpe_deponder_core_SimulationWorld_bodies_get(self.__externalRCRef()) as! Swift.Array<ExportedKotlinPackages.com.cinpe.deponder.core.PlanetBody>
            }
        }
        public var environment: ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec {
            get {
                return ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_SimulationWorld_environment_get(self.__externalRCRef()))
            }
        }
        public var rubbers: [ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose] {
            get {
                return com_cinpe_deponder_core_SimulationWorld_rubbers_get(self.__externalRCRef()) as! Swift.Array<ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose>
            }
        }
        public var viewportHeightPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_SimulationWorld_viewportHeightPx_get(self.__externalRCRef())
            }
        }
        public var viewportWidthPx: Swift.Float {
            get {
                return com_cinpe_deponder_core_SimulationWorld_viewportWidthPx_get(self.__externalRCRef())
            }
        }
        public init(
            environment: ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec
        ) {
            let __kt = com_cinpe_deponder_core_SimulationWorld_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_SimulationWorld_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec__(__kt, environment.__externalRCRef()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func bodyOf(
            id: Swift.String
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.PlanetBody? {
            return { switch com_cinpe_deponder_core_SimulationWorld_bodyOf__TypesOfArguments__Swift_String__(self.__externalRCRef(), id) { case nil: .none; case let res?: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetBody.__createClassWrapper(externalRCRef: res); } }()
        }
        public func offsetBodyPosition(
            id: Swift.String,
            dx: Swift.Float,
            dy: Swift.Float,
            scale: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_offsetBodyPosition__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), id, dx, dy, scale); return () }()
        }
        public func refreshRubberPoses(
            scale: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_refreshRubberPoses__TypesOfArguments__Swift_Float__(self.__externalRCRef(), scale); return () }()
        }
        @available(*, deprecated, message: "No-op since 0.6.1: simulated annealing has been retired. Real mutations wake the world automatically; invoke unfold() to untangle topology.")
        public func reheat(
            minAlpha: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_reheat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), minAlpha); return () }()
        }
        public func removePlanet(
            id: Swift.String,
            scale: Swift.Float
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_SimulationWorld_removePlanet__TypesOfArguments__Swift_String_Swift_Float__(self.__externalRCRef(), id, scale)
        }
        public func removeRubber(
            key: ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey,
            scale: Swift.Float
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_SimulationWorld_removeRubber__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EdgeKey_Swift_Float__(self.__externalRCRef(), key.__externalRCRef(), scale)
        }
        public func rubberOf(
            key: ExportedKotlinPackages.com.cinpe.deponder.core.EdgeKey
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose? {
            return { switch com_cinpe_deponder_core_SimulationWorld_rubberOf__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EdgeKey__(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose.__createClassWrapper(externalRCRef: res); } }()
        }
        public func setBodyPinned(
            id: Swift.String,
            pinned: Swift.Bool
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_setBodyPinned__TypesOfArguments__Swift_String_Swift_Bool__(self.__externalRCRef(), id, pinned); return () }()
        }
        public func setBodyPosition(
            id: Swift.String,
            x: Swift.Float,
            y: Swift.Float,
            scale: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_setBodyPosition__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), id, x, y, scale); return () }()
        }
        public func setBodyPressed(
            id: Swift.String,
            pressed: Swift.Bool
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_setBodyPressed__TypesOfArguments__Swift_String_Swift_Bool__(self.__externalRCRef(), id, pressed); return () }()
        }
        public func setBodySize(
            id: Swift.String,
            widthPx: Swift.Float,
            heightPx: Swift.Float,
            scale: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_setBodySize__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), id, widthPx, heightPx, scale); return () }()
        }
        public func step(
            dtSeconds: Swift.Float,
            scale: Swift.Float
        ) async throws -> Swift.Bool {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = com_cinpe_deponder_core_SimulationWorld_step__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), dtSeconds, scale, {
                    let originalBlock: (Swift.Bool) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Bool = arg0
                        originalBlock(_arg0)
                        return true
                    }
                }(), {
                    let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                        originalBlock(_arg0)
                        return true
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        public func stepSync(
            dtSeconds: Swift.Float,
            scale: Swift.Float
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_SimulationWorld_stepSync__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), dtSeconds, scale)
        }
        public func syncTopology(
            planets: [ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec],
            edges: [ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec],
            initialPositionProvider: @escaping (ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_syncTopology__TypesOfArguments__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__U28ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpecU29202D_U20ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(self.__externalRCRef(), planets, edges, {
                let originalBlock: (ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 = initialPositionProvider
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec = ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec.__createClassWrapper(externalRCRef: arg0)
                    let _result = originalBlock(_arg0)
                    return _result.__externalRCRef()
                }
            }()); return () }()
        }
        public func syncTopology(
            planets: [ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec],
            edges: [ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec],
            defaultInitialX: Swift.Float,
            defaultInitialY: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_syncTopology__TypesOfArguments__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__Swift_Float_Swift_Float__(self.__externalRCRef(), planets, edges, defaultInitialX, defaultInitialY); return () }()
        }
        public func unfold() -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_unfold(self.__externalRCRef()); return () }()
        }
        public func updateAllRubbersElasticity(
            elasticity: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_updateAllRubbersElasticity__TypesOfArguments__Swift_Float__(self.__externalRCRef(), elasticity); return () }()
        }
        public func updateEnvironment(
            spec: ExportedKotlinPackages.com.cinpe.deponder.core.EnvironmentSpec,
            reheatSimulation: Swift.Bool
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_updateEnvironment__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec_Swift_Bool__(self.__externalRCRef(), spec.__externalRCRef(), reheatSimulation); return () }()
        }
        public func updateViewport(
            widthPx: Swift.Float,
            heightPx: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_updateViewport__TypesOfArguments__Swift_Float_Swift_Float__(self.__externalRCRef(), widthPx, heightPx); return () }()
        }
        public func upsertPlanet(
            spec: ExportedKotlinPackages.com.cinpe.deponder.core.PlanetSpec,
            initialX: Swift.Float,
            initialY: Swift.Float,
            scale: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_upsertPlanet__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec_Swift_Float_Swift_Float_Swift_Float__(self.__externalRCRef(), spec.__externalRCRef(), initialX, initialY, scale); return () }()
        }
        public func upsertRubber(
            spec: ExportedKotlinPackages.com.cinpe.deponder.core.RubberSpec,
            scale: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose {
            return ExportedKotlinPackages.com.cinpe.deponder.core.RubberPose.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_SimulationWorld_upsertRubber__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec_Swift_Float__(self.__externalRCRef(), spec.__externalRCRef(), scale))
        }
        @available(*, deprecated, message: "No-op since 0.6.1: manual alpha wakeups have been retired; topology, physics, scale, and drag updates wake the world automatically.")
        public func wake(
            minAlpha: Swift.Float
        ) -> Swift.Void {
            return { com_cinpe_deponder_core_SimulationWorld_wake__TypesOfArguments__Swift_Float__(self.__externalRCRef(), minAlpha); return () }()
        }
    }
    public final class Vec2: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var Zero: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_Vec2_Companion_Zero_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.Companion {
                get {
                    return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.Companion.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_Vec2_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public var packed: Swift.Int64 {
            get {
                return com_cinpe_deponder_core_Vec2_packed_get(self.__externalRCRef())
            }
        }
        public var x: Swift.Float {
            get {
                return com_cinpe_deponder_core_Vec2_x_get(self.__externalRCRef())
            }
        }
        public var y: Swift.Float {
            get {
                return com_cinpe_deponder_core_Vec2_y_get(self.__externalRCRef())
            }
        }
        public init(
            packed: Swift.Int64
        ) {
            let __kt = com_cinpe_deponder_core_Vec2_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_Vec2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64__(__kt, packed); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            x: Swift.Float,
            y: Swift.Float
        ) {
            let __kt = com_cinpe_deponder_core_Vec2_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_cinpe_deponder_core_Vec2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float__(__kt, x, y); return () }()
        }
        public static func *(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2,
            scalar: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            this._times(scalar: scalar)
        }
        public static func +(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2,
            other: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            this._plus(other: other)
        }
        public static func -(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2,
            other: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            this._minus(other: other)
        }
        public static func ==(
            this: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func _minus(
            other: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_Vec2_minus__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _plus(
            other: ExportedKotlinPackages.com.cinpe.deponder.core.Vec2
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_Vec2_plus__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _times(
            scalar: Swift.Float
        ) -> ExportedKotlinPackages.com.cinpe.deponder.core.Vec2 {
            return ExportedKotlinPackages.com.cinpe.deponder.core.Vec2.__createClassWrapper(externalRCRef: com_cinpe_deponder_core_Vec2_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), scalar))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return com_cinpe_deponder_core_Vec2_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return com_cinpe_deponder_core_Vec2_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return com_cinpe_deponder_core_Vec2_toString(self.__externalRCRef())
        }
    }
}
