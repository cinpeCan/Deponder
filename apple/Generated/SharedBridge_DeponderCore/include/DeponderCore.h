#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void * com_cinpe_deponder_core_DeponderArrowMode_BOTH();

void * com_cinpe_deponder_core_DeponderArrowMode_END();

void * com_cinpe_deponder_core_DeponderArrowMode_NONE();

void * com_cinpe_deponder_core_DeponderArrowMode_START();

int32_t com_cinpe_deponder_core_DeponderArrowMode_ordinal(void * self);

float com_cinpe_deponder_core_DeponderDefaults_AUTO_LENGTH_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_INFINITE_RANGE_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Planet_DEGREE_RADIUS_INCREMENT_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Planet_ELASTICITY_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Planet_INFINITE_CHARGE_STRENGTH_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Planet_MASS_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Planet_REPULSION_RADIUS_DP_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Planet_get();

float com_cinpe_deponder_core_DeponderDefaults_Preset_airDamping_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Preset_alphaDecay_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Preset_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * self, float airDamping, float wallRepulsionDp, float planetRepulsionDp, float planetElasticity, float rubberNaturalLengthDp, float rubberElasticity, float alphaDecay);

_Bool com_cinpe_deponder_core_DeponderDefaults_Preset_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_DeponderDefaults_Preset_hashCode(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Preset_init_allocate();

_Bool com_cinpe_deponder_core_DeponderDefaults_Preset_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * __kt, float airDamping, float wallRepulsionDp, float planetRepulsionDp, float planetElasticity, float rubberNaturalLengthDp, float rubberElasticity, float alphaDecay);

_Bool com_cinpe_deponder_core_DeponderDefaults_Preset_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * __kt, float airDamping, float wallRepulsionDp, float planetRepulsionDp, float planetElasticity, float rubberNaturalLengthDp, float rubberElasticity);

float com_cinpe_deponder_core_DeponderDefaults_Preset_planetElasticity_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Preset_planetRepulsionDp_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Preset_rubberElasticity_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Preset_rubberNaturalLengthDp_get(void * self);

NSString * com_cinpe_deponder_core_DeponderDefaults_Preset_toString(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Preset_wallRepulsionDp_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Presets_Balanced_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Presets_Crisp_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Presets_GalaxyInfinite_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Presets_get();

float com_cinpe_deponder_core_DeponderDefaults_Root_AIR_DAMPING_get(void * self);

_Bool com_cinpe_deponder_core_DeponderDefaults_Root_COOLING_ENABLED_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_INIT_SCALE_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_MAX_SCALE_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_MIN_SCALE_get(void * self);

_Bool com_cinpe_deponder_core_DeponderDefaults_Root_UNFOLD_ENABLED_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_BOTTOM_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_END_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_START_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_WALL_ELASTICITY_TOP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Root_WALL_REPULSION_DP_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Root_get();

float com_cinpe_deponder_core_DeponderDefaults_Rubber_CONTENT_PADDING_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_ARROW_SIZE_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_ENDPOINT_GAP_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_LABEL_OFFSET_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_LABEL_TEXT_SIZE_SP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_DEFAULT_THICKNESS_DP_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_ELASTICITY_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Rubber_NATURAL_LENGTH_DP_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Rubber_get();

float com_cinpe_deponder_core_DeponderDefaults_Simulation_ALPHA_DECAY_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_ALPHA_MIN_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_BARNES_HUT_THETA_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_COINCIDENT_EPSILON_SQ_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_COINCIDENT_PERTURBATION_RADIUS_PX_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_COULOMB_SOFTENING_SQ_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_INITIAL_ALPHA_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_INTERACTION_ALPHA_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_DT_SECONDS_get(void * self);

int32_t com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_SUBSTEPS_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_MAX_SUBSTEP_DT_SECONDS_get(void * self);

int32_t com_cinpe_deponder_core_DeponderDefaults_Simulation_PARALLEL_DISPATCH_THRESHOLD_get(void * self);

int32_t com_cinpe_deponder_core_DeponderDefaults_Simulation_PARALLEL_WORKER_CHUNKS_get(void * self);

int32_t com_cinpe_deponder_core_DeponderDefaults_Simulation_QUADTREE_NODE_THRESHOLD_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_REFERENCE_DT_SECONDS_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_REST_ACCEL_PX_PER_SEC_SQ_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_REST_SPEED_PX_PER_SEC_get(void * self);

float com_cinpe_deponder_core_DeponderDefaults_Simulation_SETTLE_DURATION_SECONDS_get(void * self);

void * com_cinpe_deponder_core_DeponderDefaults_Simulation_get();

void * com_cinpe_deponder_core_DeponderDefaults_get();

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_Default_get(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_None_get(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_contour__TypesOfArguments__Swift_Float__(void * self, float gapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_contour__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float startGapDp, float endGapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_endOnly__TypesOfArguments__Swift_Float__(void * self, float gapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_fixed__TypesOfArguments__Swift_Float__(void * self, float insetDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_fixed__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float startDp, float endDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_get();

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_none(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_Companion_startOnly__TypesOfArguments__Swift_Float__(void * self, float gapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_Mode_CONTOUR();

void * com_cinpe_deponder_core_DeponderRubberInsets_Mode_FIXED();

void * com_cinpe_deponder_core_DeponderRubberInsets_Mode_NONE();

int32_t com_cinpe_deponder_core_DeponderRubberInsets_Mode_ordinal(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_endMode_get(void * self);

float com_cinpe_deponder_core_DeponderRubberInsets_endValueDp_get(void * self);

_Bool com_cinpe_deponder_core_DeponderRubberInsets_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

_Bool com_cinpe_deponder_core_DeponderRubberInsets_hasEndSafeArea_get(void * self);

_Bool com_cinpe_deponder_core_DeponderRubberInsets_hasStartSafeArea_get(void * self);

int32_t com_cinpe_deponder_core_DeponderRubberInsets_hashCode(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_startMode_get(void * self);

float com_cinpe_deponder_core_DeponderRubberInsets_startValueDp_get(void * self);

NSString * com_cinpe_deponder_core_DeponderRubberInsets_toString(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_withEndContour__TypesOfArguments__Swift_Float__(void * self, float gapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_withEndFixed__TypesOfArguments__Swift_Float__(void * self, float insetDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_withEndNone(void * self);

void * com_cinpe_deponder_core_DeponderRubberInsets_withStartContour__TypesOfArguments__Swift_Float__(void * self, float gapDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_withStartFixed__TypesOfArguments__Swift_Float__(void * self, float insetDp);

void * com_cinpe_deponder_core_DeponderRubberInsets_withStartNone(void * self);

NSString * com_cinpe_deponder_core_EdgeKey_Companion_encode__TypesOfArguments__Swift_String_Swift_String__(void * self, NSString * sId, NSString * eId);

void * com_cinpe_deponder_core_EdgeKey_Companion_get();

NSString * com_cinpe_deponder_core_EdgeKey_encoded_get(void * self);

_Bool com_cinpe_deponder_core_EdgeKey_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

NSString * com_cinpe_deponder_core_EdgeKey_first_get(void * self);

int32_t com_cinpe_deponder_core_EdgeKey_hashCode(void * self);

void * com_cinpe_deponder_core_EdgeKey_init_allocate();

_Bool com_cinpe_deponder_core_EdgeKey_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(void * __kt, NSString * a, NSString * b);

NSString * com_cinpe_deponder_core_EdgeKey_second_get(void * self);

NSString * com_cinpe_deponder_core_EdgeKey_toString(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_airDamping_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_alphaDecay_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_alphaMin_get(void * self);

_Bool com_cinpe_deponder_core_EnvironmentSpec_coolingEnabled_get(void * self);

void * com_cinpe_deponder_core_EnvironmentSpec_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * self, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionPx, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, float degreeRadiusIncrementPx, float infiniteChargeStrength, _Bool coolingEnabled, float alphaDecay, float alphaMin, float restSpeedPxPerSec, float restAccelPxPerSecSq, float settleDurationSeconds);

float com_cinpe_deponder_core_EnvironmentSpec_degreeRadiusIncrementPx_get(void * self);

_Bool com_cinpe_deponder_core_EnvironmentSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_EnvironmentSpec_hashCode(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_infiniteChargeStrength_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_initScale_get(void * self);

void * com_cinpe_deponder_core_EnvironmentSpec_init_allocate();

_Bool com_cinpe_deponder_core_EnvironmentSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

_Bool com_cinpe_deponder_core_EnvironmentSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * __kt, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionPx, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, float degreeRadiusIncrementPx, float infiniteChargeStrength, _Bool coolingEnabled, float alphaDecay, float alphaMin, float restSpeedPxPerSec, float restAccelPxPerSecSq, float settleDurationSeconds);

float com_cinpe_deponder_core_EnvironmentSpec_maxScale_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_minScale_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_restAccelPxPerSecSq_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_restSpeedPxPerSec_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_settleDurationSeconds_get(void * self);

NSString * com_cinpe_deponder_core_EnvironmentSpec_toString(void * self);

_Bool com_cinpe_deponder_core_EnvironmentSpec_unfoldEnabled_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_wallElasticityBottom_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_wallElasticityEnd_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_wallElasticityStart_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_wallElasticityTop_get(void * self);

float com_cinpe_deponder_core_EnvironmentSpec_wallRepulsionPx_get(void * self);

void * com_cinpe_deponder_core_EnvironmentSpec_withUnfold__TypesOfArguments__Swift_Bool__(void * self, _Bool unfoldEnabled);

void * com_cinpe_deponder_core_NodePair_copy__TypesOfArguments__Swift_String_Swift_String__(void * self, NSString * startId, NSString * endId);

NSString * com_cinpe_deponder_core_NodePair_endId_get(void * self);

_Bool com_cinpe_deponder_core_NodePair_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_NodePair_hashCode(void * self);

void * com_cinpe_deponder_core_NodePair_init_allocate();

_Bool com_cinpe_deponder_core_NodePair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(void * __kt, NSString * startId, NSString * endId);

NSString * com_cinpe_deponder_core_NodePair_startId_get(void * self);

NSString * com_cinpe_deponder_core_NodePair_toString(void * self);

int32_t com_cinpe_deponder_core_PlanetBody_degree_get(void * self);

float com_cinpe_deponder_core_PlanetBody_dx_get(void * self);

float com_cinpe_deponder_core_PlanetBody_dy_get(void * self);

float com_cinpe_deponder_core_PlanetBody_halfHeightPx_get(void * self);

float com_cinpe_deponder_core_PlanetBody_halfWidthPx_get(void * self);

_Bool com_cinpe_deponder_core_PlanetBody_isPressed_get(void * self);

void * com_cinpe_deponder_core_PlanetBody_spec_get(void * self);

_Bool com_cinpe_deponder_core_PlanetBody_spec_set__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__(void * self, void * newValue);

float com_cinpe_deponder_core_PlanetBody_vx_get(void * self);

float com_cinpe_deponder_core_PlanetBody_vy_get(void * self);

float com_cinpe_deponder_core_PlanetBody_x_get(void * self);

float com_cinpe_deponder_core_PlanetBody_y_get(void * self);

void * com_cinpe_deponder_core_PlanetPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(void * self, void * preset);

void * com_cinpe_deponder_core_PlanetPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset_Swift_Bool__(void * self, void * preset, _Bool pinned);

void * com_cinpe_deponder_core_PlanetPhysics_Companion_get();

void * com_cinpe_deponder_core_PlanetPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(void * self, float mass, float repulsionRadiusDp, float elasticity, _Bool pinned);

float com_cinpe_deponder_core_PlanetPhysics_elasticity_get(void * self);

_Bool com_cinpe_deponder_core_PlanetPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_PlanetPhysics_hashCode(void * self);

void * com_cinpe_deponder_core_PlanetPhysics_init_allocate();

_Bool com_cinpe_deponder_core_PlanetPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(void * __kt, float mass, float repulsionRadiusDp, float elasticity, _Bool pinned);

float com_cinpe_deponder_core_PlanetPhysics_mass_get(void * self);

_Bool com_cinpe_deponder_core_PlanetPhysics_pinned_get(void * self);

float com_cinpe_deponder_core_PlanetPhysics_repulsionRadiusDp_get(void * self);

void * com_cinpe_deponder_core_PlanetPhysics_toPlanetSpec__TypesOfArguments__Swift_String_Swift_Float__(void * self, NSString * id_, float density);

NSString * com_cinpe_deponder_core_PlanetPhysics_toString(void * self);

void * com_cinpe_deponder_core_PlanetPhysics_withElasticity__TypesOfArguments__Swift_Float__(void * self, float elasticity);

void * com_cinpe_deponder_core_PlanetPhysics_withMass__TypesOfArguments__Swift_Float__(void * self, float mass);

void * com_cinpe_deponder_core_PlanetPhysics_withPinned__TypesOfArguments__Swift_Bool__(void * self, _Bool pinned);

void * com_cinpe_deponder_core_PlanetPhysics_withRepulsionRadiusDp__TypesOfArguments__Swift_Float__(void * self, float repulsionRadiusDp);

void * com_cinpe_deponder_core_PlanetSpec_copy__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(void * self, NSString * id_, float mass, float repulsionRadiusPx, float elasticity, _Bool pinned);

float com_cinpe_deponder_core_PlanetSpec_elasticity_get(void * self);

_Bool com_cinpe_deponder_core_PlanetSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_PlanetSpec_hashCode(void * self);

NSString * com_cinpe_deponder_core_PlanetSpec_id_get(void * self);

void * com_cinpe_deponder_core_PlanetSpec_init_allocate();

_Bool com_cinpe_deponder_core_PlanetSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Float_Swift_Float_Swift_Float_Swift_Bool__(void * __kt, NSString * id_, float mass, float repulsionRadiusPx, float elasticity, _Bool pinned);

float com_cinpe_deponder_core_PlanetSpec_mass_get(void * self);

_Bool com_cinpe_deponder_core_PlanetSpec_pinned_get(void * self);

float com_cinpe_deponder_core_PlanetSpec_repulsionRadiusPx_get(void * self);

NSString * com_cinpe_deponder_core_PlanetSpec_toString(void * self);

void * com_cinpe_deponder_core_RootPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(void * self, void * preset);

void * com_cinpe_deponder_core_RootPhysics_Companion_get();

float com_cinpe_deponder_core_RootPhysics_airDamping_get(void * self);

float com_cinpe_deponder_core_RootPhysics_alphaDecay_get(void * self);

_Bool com_cinpe_deponder_core_RootPhysics_coolingEnabled_get(void * self);

void * com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * self, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom);

void * com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float__(void * self, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, _Bool coolingEnabled, float alphaDecay);

void * com_cinpe_deponder_core_RootPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * self, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, float alphaDecay);

_Bool com_cinpe_deponder_core_RootPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_RootPhysics_hashCode(void * self);

float com_cinpe_deponder_core_RootPhysics_initScale_get(void * self);

void * com_cinpe_deponder_core_RootPhysics_init_allocate();

_Bool com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

_Bool com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * __kt, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom);

_Bool com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Bool_Swift_Float__(void * __kt, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, _Bool coolingEnabled, float alphaDecay);

_Bool com_cinpe_deponder_core_RootPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * __kt, float initScale, float maxScale, float minScale, float airDamping, float wallRepulsionDp, float wallElasticityStart, float wallElasticityTop, float wallElasticityEnd, float wallElasticityBottom, float alphaDecay);

float com_cinpe_deponder_core_RootPhysics_maxScale_get(void * self);

float com_cinpe_deponder_core_RootPhysics_minScale_get(void * self);

void * com_cinpe_deponder_core_RootPhysics_toEnvironmentSpec__TypesOfArguments__Swift_Float__(void * self, float density);

NSString * com_cinpe_deponder_core_RootPhysics_toString(void * self);

_Bool com_cinpe_deponder_core_RootPhysics_unfoldEnabled_get(void * self);

float com_cinpe_deponder_core_RootPhysics_wallElasticityBottom_get(void * self);

float com_cinpe_deponder_core_RootPhysics_wallElasticityEnd_get(void * self);

float com_cinpe_deponder_core_RootPhysics_wallElasticityStart_get(void * self);

float com_cinpe_deponder_core_RootPhysics_wallElasticityTop_get(void * self);

float com_cinpe_deponder_core_RootPhysics_wallRepulsionDp_get(void * self);

void * com_cinpe_deponder_core_RootPhysics_withAirDamping__TypesOfArguments__Swift_Float__(void * self, float airDamping);

void * com_cinpe_deponder_core_RootPhysics_withCooling__TypesOfArguments__Swift_Bool_Swift_Float__(void * self, _Bool coolingEnabled, float alphaDecay);

void * com_cinpe_deponder_core_RootPhysics_withScaleBounds__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float__(void * self, float initScale, float minScale, float maxScale);

void * com_cinpe_deponder_core_RootPhysics_withUnfold__TypesOfArguments__Swift_Bool__(void * self, _Bool unfoldEnabled);

void * com_cinpe_deponder_core_RootPhysics_withWallRepulsionDp__TypesOfArguments__Swift_Float__(void * self, float wallRepulsionDp);

void * com_cinpe_deponder_core_RubberPhysics_Companion_Default_get(void * self);

void * com_cinpe_deponder_core_RubberPhysics_Companion_auto__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float__(void * self, float contentPaddingDp, float minLengthDp, float maxLengthDp, float elasticity);

void * com_cinpe_deponder_core_RubberPhysics_Companion_fixed__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float naturalLengthDp, float elasticity);

void * com_cinpe_deponder_core_RubberPhysics_Companion_fromPreset__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderDefaults_Preset__(void * self, void * preset);

void * com_cinpe_deponder_core_RubberPhysics_Companion_get();

float com_cinpe_deponder_core_RubberPhysics_contentPaddingDp_get(void * self);

void * com_cinpe_deponder_core_RubberPhysics_copy__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float elasticity, float naturalLengthDp);

float com_cinpe_deponder_core_RubberPhysics_elasticity_get(void * self);

_Bool com_cinpe_deponder_core_RubberPhysics_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_RubberPhysics_hashCode(void * self);

void * com_cinpe_deponder_core_RubberPhysics_init_allocate();

_Bool com_cinpe_deponder_core_RubberPhysics_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float__(void * __kt, float elasticity, float naturalLengthDp);

_Bool com_cinpe_deponder_core_RubberPhysics_isAutoLength_get(void * self);

float com_cinpe_deponder_core_RubberPhysics_maxLengthDp_get(void * self);

float com_cinpe_deponder_core_RubberPhysics_minLengthDp_get(void * self);

float com_cinpe_deponder_core_RubberPhysics_naturalLengthDp_get(void * self);

float com_cinpe_deponder_core_RubberPhysics_resolveNaturalLengthDp__TypesOfArguments__Swift_Float__(void * self, float contentWidthDp);

float com_cinpe_deponder_core_RubberPhysics_resolveNaturalLengthPx__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float density, float contentWidthPx);

void * com_cinpe_deponder_core_RubberPhysics_toRubberSpec__TypesOfArguments__Swift_String_Swift_String_Swift_Float__(void * self, NSString * startId, NSString * endId, float density);

void * com_cinpe_deponder_core_RubberPhysics_toRubberSpec__TypesOfArguments__Swift_String_Swift_String_Swift_Float_Swift_Float__(void * self, NSString * startId, NSString * endId, float density, float contentWidthPx);

NSString * com_cinpe_deponder_core_RubberPhysics_toString(void * self);

void * com_cinpe_deponder_core_RubberPhysics_withAutoLength__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float__(void * self, float contentPaddingDp, float minLengthDp, float maxLengthDp);

void * com_cinpe_deponder_core_RubberPhysics_withElasticity__TypesOfArguments__Swift_Float__(void * self, float elasticity);

void * com_cinpe_deponder_core_RubberPhysics_withNaturalLengthDp__TypesOfArguments__Swift_Float__(void * self, float naturalLengthDp);

float com_cinpe_deponder_core_RubberPose_centerDistance_get(void * self);

float com_cinpe_deponder_core_RubberPose_centerX_get(void * self);

float com_cinpe_deponder_core_RubberPose_centerY_get(void * self);

float com_cinpe_deponder_core_RubberPose_directionX_get(void * self);

float com_cinpe_deponder_core_RubberPose_directionY_get(void * self);

_Bool com_cinpe_deponder_core_RubberPose_dirty_get(void * self);

float com_cinpe_deponder_core_RubberPose_endContourPx_get(void * self);

_Bool com_cinpe_deponder_core_RubberPose_isActive_get(void * self);

float com_cinpe_deponder_core_RubberPose_localSpanOffsetX__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_resolvedCenterX__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_resolvedCenterY__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_resolvedEndInsetPx__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_resolvedLength__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_resolvedStartInsetPx__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_DeponderRubberInsets_Swift_Float__(void * self, void * insets, float density);

float com_cinpe_deponder_core_RubberPose_rotationDeg_get(void * self);

float com_cinpe_deponder_core_RubberPose_scale_get(void * self);

void * com_cinpe_deponder_core_RubberPose_spec_get(void * self);

_Bool com_cinpe_deponder_core_RubberPose_spec_set__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__(void * self, void * newValue);

float com_cinpe_deponder_core_RubberPose_startContourPx_get(void * self);

void * com_cinpe_deponder_core_RubberSpec_copy__TypesOfArguments__Swift_String_Swift_String_Swift_Float_Swift_Float__(void * self, NSString * startId, NSString * endId, float elasticity, float naturalLengthPx);

float com_cinpe_deponder_core_RubberSpec_elasticity_get(void * self);

NSString * com_cinpe_deponder_core_RubberSpec_endId_get(void * self);

_Bool com_cinpe_deponder_core_RubberSpec_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_RubberSpec_hashCode(void * self);

void * com_cinpe_deponder_core_RubberSpec_init_allocate();

_Bool com_cinpe_deponder_core_RubberSpec_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String_Swift_Float_Swift_Float__(void * __kt, NSString * startId, NSString * endId, float elasticity, float naturalLengthPx);

void * com_cinpe_deponder_core_RubberSpec_key_get(void * self);

float com_cinpe_deponder_core_RubberSpec_naturalLengthPx_get(void * self);

NSString * com_cinpe_deponder_core_RubberSpec_startId_get(void * self);

NSString * com_cinpe_deponder_core_RubberSpec_toString(void * self);

void * com_cinpe_deponder_core_SimulationWorld_Companion_computeWallForce__TypesOfArguments__Swift_Float_Swift_Float_Swift_Float_Swift_Float_Swift_Float_ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec__(void * self, float px, float py, float scale, float width, float height, void * env);

void * com_cinpe_deponder_core_SimulationWorld_Companion_get();

float com_cinpe_deponder_core_SimulationWorld_alpha_get(void * self);

NSArray<id> * com_cinpe_deponder_core_SimulationWorld_bodies_get(void * self);

void * _Nullable com_cinpe_deponder_core_SimulationWorld_bodyOf__TypesOfArguments__Swift_String__(void * self, NSString * id_);

void * com_cinpe_deponder_core_SimulationWorld_environment_get(void * self);

void * com_cinpe_deponder_core_SimulationWorld_init_allocate();

_Bool com_cinpe_deponder_core_SimulationWorld_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec__(void * __kt, void * environment);

_Bool com_cinpe_deponder_core_SimulationWorld_offsetBodyPosition__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(void * self, NSString * id_, float dx, float dy, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_refreshRubberPoses__TypesOfArguments__Swift_Float__(void * self, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_reheat__TypesOfArguments__Swift_Float__(void * self, float minAlpha);

_Bool com_cinpe_deponder_core_SimulationWorld_removePlanet__TypesOfArguments__Swift_String_Swift_Float__(void * self, NSString * id_, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_removeRubber__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EdgeKey_Swift_Float__(void * self, void * key, float scale);

void * _Nullable com_cinpe_deponder_core_SimulationWorld_rubberOf__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EdgeKey__(void * self, void * key);

NSArray<id> * com_cinpe_deponder_core_SimulationWorld_rubbers_get(void * self);

_Bool com_cinpe_deponder_core_SimulationWorld_setBodyPinned__TypesOfArguments__Swift_String_Swift_Bool__(void * self, NSString * id_, _Bool pinned);

_Bool com_cinpe_deponder_core_SimulationWorld_setBodyPosition__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(void * self, NSString * id_, float x, float y, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_setBodyPressed__TypesOfArguments__Swift_String_Swift_Bool__(void * self, NSString * id_, _Bool pressed);

_Bool com_cinpe_deponder_core_SimulationWorld_setBodySize__TypesOfArguments__Swift_String_Swift_Float_Swift_Float_Swift_Float__(void * self, NSString * id_, float widthPx, float heightPx, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_step__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float dtSeconds, float scale, _Bool (^continuation)(_Bool), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool com_cinpe_deponder_core_SimulationWorld_stepSync__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float dtSeconds, float scale);

_Bool com_cinpe_deponder_core_SimulationWorld_syncTopology__TypesOfArguments__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__U28ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpecU29202D_U20ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(void * self, NSArray<id> * planets, NSArray<id> * edges, void * (^initialPositionProvider)(void *));

_Bool com_cinpe_deponder_core_SimulationWorld_syncTopology__TypesOfArguments__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec__Swift_Array_ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec__Swift_Float_Swift_Float__(void * self, NSArray<id> * planets, NSArray<id> * edges, float defaultInitialX, float defaultInitialY);

_Bool com_cinpe_deponder_core_SimulationWorld_unfold(void * self);

_Bool com_cinpe_deponder_core_SimulationWorld_updateAllRubbersElasticity__TypesOfArguments__Swift_Float__(void * self, float elasticity);

_Bool com_cinpe_deponder_core_SimulationWorld_updateEnvironment__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_EnvironmentSpec_Swift_Bool__(void * self, void * spec, _Bool reheatSimulation);

_Bool com_cinpe_deponder_core_SimulationWorld_updateViewport__TypesOfArguments__Swift_Float_Swift_Float__(void * self, float widthPx, float heightPx);

_Bool com_cinpe_deponder_core_SimulationWorld_upsertPlanet__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_PlanetSpec_Swift_Float_Swift_Float_Swift_Float__(void * self, void * spec, float initialX, float initialY, float scale);

void * com_cinpe_deponder_core_SimulationWorld_upsertRubber__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_RubberSpec_Swift_Float__(void * self, void * spec, float scale);

float com_cinpe_deponder_core_SimulationWorld_viewportHeightPx_get(void * self);

float com_cinpe_deponder_core_SimulationWorld_viewportWidthPx_get(void * self);

_Bool com_cinpe_deponder_core_SimulationWorld_wake__TypesOfArguments__Swift_Float__(void * self, float minAlpha);

void * com_cinpe_deponder_core_Vec2_Companion_Zero_get(void * self);

void * com_cinpe_deponder_core_Vec2_Companion_get();

_Bool com_cinpe_deponder_core_Vec2_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable other);

int32_t com_cinpe_deponder_core_Vec2_hashCode(void * self);

void * com_cinpe_deponder_core_Vec2_init_allocate();

_Bool com_cinpe_deponder_core_Vec2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64__(void * __kt, int64_t packed);

_Bool com_cinpe_deponder_core_Vec2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float_Swift_Float__(void * __kt, float x, float y);

void * com_cinpe_deponder_core_Vec2_minus__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(void * self, void * other);

int64_t com_cinpe_deponder_core_Vec2_packed_get(void * self);

void * com_cinpe_deponder_core_Vec2_plus__TypesOfArguments__ExportedKotlinPackages_com_cinpe_deponder_core_Vec2__(void * self, void * other);

void * com_cinpe_deponder_core_Vec2_times__TypesOfArguments__Swift_Float__(void * self, float scalar);

NSString * com_cinpe_deponder_core_Vec2_toString(void * self);

float com_cinpe_deponder_core_Vec2_x_get(void * self);

float com_cinpe_deponder_core_Vec2_y_get(void * self);

NS_ASSUME_NONNULL_END
