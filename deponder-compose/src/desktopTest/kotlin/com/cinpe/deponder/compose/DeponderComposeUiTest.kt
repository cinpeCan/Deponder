package com.cinpe.deponder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.DeponderRubberInsets
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DeponderComposeUiTest {

    @Test
    fun bothModifierOrders_mountCleanly_preserveClick_dragInRootSpace_andZeroRemeasureOnBringToFront() =
        runComposeUiTest {
            val state = DeponderState()
            var p1Clicks = 0
            var p2Clicks = 0
            var p1MeasureCount = 0

            setContent {
                Box(
                    modifier = Modifier
                        .size(500.dp, 400.dp)
                        .deponderRoot(state),
                ) {
                    DeponderDefaultRubber(
                        startId = "p1",
                        endId = "p2",
                        label = "p1 -> p2",
                        arrowMode = DeponderArrowMode.BOTH,
                        physics = RubberPhysics(naturalLengthDp = 120f),
                        insets = DeponderRubberInsets.contour(0f),
                    )

                    // Order 1: .deponderPlanet("p1").deponderDrag()
                    Layout(
                        modifier = Modifier
                            .offset(x = 60.dp, y = 160.dp)
                            .deponderPlanet("p1", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
                            .deponderDrag()
                            .clickable { p1Clicks += 1 }
                            .testTag("planet_p1"),
                    ) { _, _ ->
                        p1MeasureCount += 1
                        layout(40, 40) {}
                    }

                    // Order 2: .deponderDrag().deponderPlanet("p2") (upstream drag before planet on same LayoutNode!)
                    Box(
                        modifier = Modifier
                            .offset(x = 260.dp, y = 160.dp)
                            .size(40.dp)
                            .deponderDrag()
                            .deponderPlanet("p2", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
                            .clickable { p2Clicks += 1 }
                            .testTag("planet_p2"),
                    )
                }
            }

            waitForIdle()

            val body1Before = state.world.bodyOf("p1")!!
            val body2Before = state.world.bodyOf("p2")!!
            val poseBefore = state.world.rubberOf(EdgeKey("p1", "p2"))!!
            val resolvedBefore = poseBefore.resolvedLength(
                DeponderRubberInsets.contour(0f),
                state.currentDensity,
            )
            assertTrue(poseBefore.isActive)
            assertTrue(resolvedBefore > 0f && resolvedBefore < poseBefore.centerDistance)

            // 1. Tap below touchSlop preserves downstream .clickable {} on BOTH modifier orderings!
            onNodeWithTag("planet_p1").performTouchInput { click() }
            onNodeWithTag("planet_p2").performTouchInput { click() }
            waitForIdle()
            assertEquals(1, p1Clicks)
            assertEquals(1, p2Clicks)

            // 2. bringToFront updates placement zOrder with ZERO re-measurements of p1!
            val measureBeforeZ = p1MeasureCount
            state.bringToFront("p1")
            waitForIdle()
            assertEquals(measureBeforeZ, p1MeasureCount, "bringToFront must not re-measure planet content")

            // 2b. Physics drag / scale updates cause ZERO extra Compose re-measurements!
            val measureBeforeMove = p1MeasureCount
            state.moveTo("p1", 110f, 180f)
            state.submitScale(1.15f)
            waitForIdle()
            assertEquals(measureBeforeMove, p1MeasureCount, "Physics updates must not trigger planet re-measurement")
            state.submitScale(1.0f)
            waitForIdle()

            // 3. Dragging BOTH p1 (.deponderPlanet().deponderDrag().clickable) and
            // p2 (.deponderDrag().deponderPlanet().clickable) moves bodies in root space without firing .clickable {}!
            val startX1 = body1Before.x
            onNodeWithTag("planet_p1").performTouchInput {
                down(center)
                moveBy(Offset(60f, 0f))
                up()
            }
            waitForIdle()
            assertEquals(1, p1Clicks, "Dragging p1 past touchSlop must cancel downstream .clickable")
            assertTrue(state.world.bodyOf("p1")!!.x > startX1 + 25f)

            val startX2 = body2Before.x
            onNodeWithTag("planet_p2").performTouchInput {
                down(center)
                moveBy(Offset(75f, 0f))
                up()
            }
            waitForIdle()
            assertEquals(1, p2Clicks, "Dragging p2 past touchSlop must cancel downstream .clickable")
            assertTrue(state.world.bodyOf("p2")!!.x > startX2 + 40f)
            assertTrue(state.world.bodyOf("p1")!!.x < state.world.bodyOf("p2")!!.x)
        }

    @Test
    fun compoundRubberWithFillMaxWidthStretch_preservesExactBadgeShrinkWidth_andSupportsBothStretchOrders() =
        runComposeUiTest {
            val state = DeponderState()
            var adInsets by mutableStateOf(DeponderRubberInsets.None)

            setContent {
                Box(
                    modifier = Modifier
                        .size(400.dp, 300.dp)
                        .deponderRoot(state),
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .deponderPlanet("b", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .deponderPlanet("c", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .deponderPlanet("d", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                        )
                    }

                    // Upstream direct stretch: .deponderStretch().deponderRubber(...)
                    Box(
                        modifier = Modifier
                            .size(12.dp, 2.dp)
                            .deponderStretch()
                            .deponderRubber("a", "b", RubberPhysics(), DeponderRubberInsets.contour(0f)),
                    )

                    // Compound Box rubber with fillMaxWidth() stretched stem + 80px non-stretched badge + nested grandchild
                    Box(
                        modifier = Modifier.deponderRubber("b", "c", RubberPhysics(), DeponderRubberInsets.contour(0f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .deponderStretch(),
                        ) {
                            // Nested stretched grandchild inside already-stretched branch (pruned cleanly)
                            Box(modifier = Modifier.size(4.dp, 2.dp).deponderStretch())
                        }
                        Layout(modifier = Modifier) { _, _ ->
                            layout(80, 18) {}
                        }
                    }

                    // Default rubber on ("a", "d") with BOTH arrows: omits arrows on Mode.NONE endpoints!
                    DeponderDefaultRubber(
                        startId = "a",
                        endId = "d",
                        arrowMode = DeponderArrowMode.BOTH,
                        insets = adInsets,
                    )

                    // Fixed-width compound Box rubber (80px outer width > 20px stretched stem)
                    Layout(
                        content = {
                            Layout(modifier = Modifier.deponderStretch()) { _, _ ->
                                layout(20, 2) {}
                            }
                        },
                        modifier = Modifier.deponderRubber("a", "c", RubberPhysics(), DeponderRubberInsets.contour(0f)),
                    ) { measurables, _ ->
                        val p = measurables.first().measure(androidx.compose.ui.unit.Constraints.fixed(20, 2))
                        layout(80, 18) { p.place(0, 8) }
                    }
                }
            }

            waitForIdle()

            // Move b & c to 60px apart -> visible contour-cleared length is 60 - 10 - 10 = 40px (< 80px badge!).
            state.moveTo("a", 50f, 150f)
            state.moveTo("b", 200f, 150f)
            state.moveTo("c", 260f, 150f)
            waitForIdle()

            val rubberAb = state.activeRubbersByKey[EdgeKey("a", "b")]!!
            assertTrue(rubberAb.isDirectStretchEnabled(), "Upstream .deponderStretch().deponderRubber() must bind as direct stretch")

            val rubberBc = state.activeRubbersByKey[EdgeKey("b", "c")]!!
            val poseBc = state.world.rubberOf(EdgeKey("b", "c"))!!
            // Even with fillMaxWidth() on the stretched line inside a 400dp root,
            // rubberBc wraps the 80px badge and shrinks cleanly to 40 / 80 = 0.5f!
            assertEquals(80, rubberBc.measuredWidthPx)
            assertEquals(0.5f, rubberBc.computeEffectiveContainerScale(poseBc), 0.05f)

            state.moveTo("a", 200f, 220f)
            state.moveTo("c", 260f, 220f)
            state.moveTo("d", 340f, 220f)
            waitForIdle()
            val rubberAc = state.activeRubbersByKey[EdgeKey("a", "c")]!!
            val poseAc = state.world.rubberOf(EdgeKey("a", "c"))!!
            assertEquals(0.5f, rubberAc.computeEffectiveContainerScale(poseAc), 0.05f)

            val rubberAd = state.activeRubbersByKey[EdgeKey("a", "d")]!!
            val poseAd = state.world.rubberOf(EdgeKey("a", "d"))!!
            val noneWidth = rubberAd.measuredWidthPx
            assertEquals(140f, rubberAd.currentSpanLengthPx(poseAd), 1e-3f)

            // Switching to `fixed(0f, 15f)` enables only the end arrow and trims 15dp from `d`!
            adInsets = DeponderRubberInsets.fixed(startDp = 0f, endDp = 15f)
            waitForIdle()
            val oneArrowWidth = rubberAd.measuredWidthPx
            assertTrue(oneArrowWidth > noneWidth, "Single safe-area arrow ($oneArrowWidth) must exceed None stem ($noneWidth)")
            assertEquals(140f - 15f * state.currentDensity, rubberAd.currentSpanLengthPx(poseAd), 1e-3f)

            // Switching to `Default` enables both arrows!
            adInsets = DeponderRubberInsets.Default
            waitForIdle()
            assertTrue(rubberAd.measuredWidthPx > oneArrowWidth)
        }

    @Test
    fun adaptiveRubberLength_scalesWithDefaultLabelAndCustomBadge_andPreservesRuntimeElasticity() =
        runComposeUiTest {
            val state = DeponderState()
            var dynamicLabel by mutableStateOf("Hi")
            var customBadgeWidthPx by mutableStateOf(20)
            var rubberPhysics by mutableStateOf(RubberPhysics())

            setContent {
                Box(
                    modifier = Modifier
                        .size(600.dp, 400.dp)
                        .deponderRoot(state),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .deponderPlanet("b", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .deponderPlanet("c", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )

                    DeponderDefaultRubber(
                        startId = "a",
                        endId = "b",
                        label = dynamicLabel,
                        arrowMode = DeponderArrowMode.END,
                        physics = rubberPhysics,
                    )

                    Box(
                        modifier = Modifier.deponderRubber("b", "c", RubberPhysics.auto(contentPaddingDp = 50f, minLengthDp = 80f)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).deponderStretch())
                        Layout(modifier = Modifier) { _, _ ->
                            layout(customBadgeWidthPx, 16) {}
                        }
                    }
                }
            }

            waitForIdle()
            val poseAb = state.world.rubberOf(EdgeKey("a", "b"))!!
            val poseBc = state.world.rubberOf(EdgeKey("b", "c"))!!
            val density = state.currentDensity

            val shortNatPx = poseAb.spec.naturalLengthPx
            assertEquals(110f * density, shortNatPx, 1f)

            // Override runtime elasticity, then expand label text -> naturalLengthPx grows while preserving elasticity!
            state.submitRubberElasticity(3.75f)
            dynamicLabel = "LONG_RELATIONSHIP_DESCRIPTION_TEXT_FOR_ADAPTIVE_SPRING"
            waitForIdle()

            val longNatPx = poseAb.spec.naturalLengthPx
            assertTrue(longNatPx > shortNatPx + 80f * density, "Long label ($longNatPx) must expand beyond short ($shortNatPx)")
            assertEquals(3.75f, poseAb.spec.elasticity, 1e-4f)

            // Custom compound rubber badge width growth also expands its spring rest length automatically!
            val smallBadgeNatPx = poseBc.spec.naturalLengthPx
            customBadgeWidthPx = (160f * density).toInt()
            waitForIdle()
            assertEquals(210f * density, poseBc.spec.naturalLengthPx, 2f)
            assertTrue(poseBc.spec.naturalLengthPx > smallBadgeNatPx)

            // Switching explicitly to fixed length locks exact 130dp regardless of the long label!
            rubberPhysics = RubberPhysics.fixed(130f)
            waitForIdle()
            assertEquals(130f * density, poseAb.spec.naturalLengthPx, 1e-3f)
        }

    @Test
    fun invalidModifierPlacements_failFastInRealComposeHierarchy() {
        // 1. deponderPlanet outside deponderRoot
        assertFailsWith<IllegalStateException> {
            runComposeUiTest {
                setContent {
                    Box(Modifier.size(20.dp).deponderPlanet("orphan"))
                }
            }
        }

        // 2. Nested deponderPlanet inside another deponderPlanet
        assertFailsWith<IllegalStateException> {
            runComposeUiTest {
                val state = DeponderState()
                setContent {
                    Box(Modifier.size(200.dp).deponderRoot(state)) {
                        Box(Modifier.size(40.dp).deponderPlanet("outer")) {
                            Box(Modifier.size(20.dp).deponderPlanet("inner"))
                        }
                    }
                }
            }
        }

        // 3. deponderDrag on outer parent Box outside deponderPlanet
        assertFailsWith<IllegalStateException> {
            runComposeUiTest {
                val state = DeponderState()
                setContent {
                    Box(Modifier.size(200.dp).deponderRoot(state)) {
                        Box(Modifier.size(60.dp).deponderDrag()) {
                            Box(Modifier.size(30.dp).deponderPlanet("p"))
                        }
                    }
                }
            }
        }

        // 4. deponderStretch on outer parent Box outside deponderRubber
        assertFailsWith<IllegalStateException> {
            runComposeUiTest {
                val state = DeponderState()
                setContent {
                    Box(Modifier.size(200.dp).deponderRoot(state)) {
                        Box(Modifier.size(60.dp).deponderStretch()) {
                            Box(Modifier.size(30.dp).deponderRubber("a", "b"))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun dynamicDeponderStateSwapFromSleep_migratesMountedNodesWithoutLeaks_andWakesNewSession() =
        runComposeUiTest {
            val state1 = DeponderState(RootPhysics(airDamping = 25f))
            val state2 = DeponderState(RootPhysics(airDamping = 1.5f))
            var activeState by mutableStateOf(state1)

            setContent {
                Box(
                    modifier = Modifier
                        .size(400.dp, 300.dp)
                        .deponderRoot(activeState),
                ) {
                    DeponderDefaultRubber(
                        startId = "a",
                        endId = "b",
                        physics = RubberPhysics.fixed(180f),
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 120.dp, y = 140.dp)
                            .size(30.dp)
                            .deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 170.dp, y = 140.dp)
                            .size(30.dp)
                            .deponderPlanet("b", PlanetPhysics(pinned = false, repulsionRadiusDp = 0f)),
                    )
                }
            }

            waitForIdle()
            assertFalse(state1.isAwake, "First state should settle cleanly to sleep before swapping")
            assertEquals(2, state1.activePlanetsById.size)
            assertEquals(1, state1.activeRubbersByKey.size)

            // Swap to state2 while state1 is sleeping!
            activeState = state2
            waitForIdle()

            assertTrue(state1.activePlanetsById.isEmpty(), "Old state must not leak planet nodes after root state swap")
            assertTrue(state1.activeRubbersByKey.isEmpty(), "Old state must not leak rubber nodes after root state swap")
            assertEquals(2, state2.activePlanetsById.size)
            assertEquals(1, state2.activeRubbersByKey.size)
            assertTrue(state2.world.viewportWidthPx > 0f && state2.world.viewportHeightPx > 0f)
            assertTrue(state2.world.rubberOf(EdgeKey("a", "b"))!!.isActive)

            val bInState2 = state2.world.bodyOf("b")!!
            assertTrue(
                bInState2.x > 260f * state2.currentDensity,
                "New state2 frame loop must wake and pull 'b' toward 180dp rest distance from 'a' (x=${bInState2.x})",
            )
        }

    @Test
    fun innerSizedStretchChild_collapsesLayoutWidthForBadgeAndAdaptiveLength_andRefreshesOnDetach() =
        runComposeUiTest {
            val state = DeponderState()
            var showStretch by mutableStateOf(true)
            var planetMeasureCount = 0
            var planetPlacementCount = 0

            setContent {
                Box(
                    modifier = Modifier
                        .size(500.dp, 400.dp)
                        .deponderRoot(state),
                ) {
                    Layout(
                        modifier = Modifier
                            .offset(x = 100.dp, y = 150.dp)
                            .deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    ) { _, _ ->
                        planetMeasureCount += 1
                        layout(20, 20) {
                            planetPlacementCount += 1
                        }
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 220.dp, y = 150.dp)
                            .size(20.dp)
                            .deponderPlanet("b", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )

                    Box(
                        modifier = Modifier.deponderRubber(
                            startId = "a",
                            endId = "b",
                            physics = RubberPhysics.auto(contentPaddingDp = 40f, minLengthDp = 60f),
                            insets = DeponderRubberInsets.None,
                        ),
                    ) {
                        if (showStretch) {
                            Box(
                                modifier = Modifier
                                    .deponderStretch()
                                    .size(120.dp, 2.dp),
                            )
                        }
                        Layout(modifier = Modifier) { _, _ ->
                            layout(40, 16) {}
                        }
                    }
                }
            }

            waitForIdle()
            val rubber = state.activeRubbersByKey[EdgeKey("a", "b")]!!
            val pose = state.world.rubberOf(EdgeKey("a", "b"))!!
            val density = state.currentDensity

            assertEquals(40, rubber.measuredWidthPx)
            assertEquals((40f / density + 40f).coerceAtLeast(60f) * density, pose.spec.naturalLengthPx, 1e-2f)

            val measureBefore = planetMeasureCount
            val placeBefore = planetPlacementCount
            state.moveTo("a", 200f, 160f)
            state.moveTo("b", 220f, 160f)
            waitForIdle()
            assertEquals(measureBefore, planetMeasureCount)
            assertEquals(placeBefore, planetPlacementCount)
            assertEquals(0.5f, rubber.computeEffectiveContainerScale(pose), 1e-3f)

            // Detaching the inner stretch node triggers clean membership removal and pose refresh.
            showStretch = false
            waitForIdle()
            assertTrue(rubber.childStretchNodes.isEmpty())
            assertEquals(40, rubber.measuredWidthPx)
        }

    @Test
    fun unpinnedMovingPlanet_undergoesMultiFrameSpringMotion_withZeroRelayouts() =
        runComposeUiTest {
            val state = DeponderState(RootPhysics(airDamping = 1.5f))
            var movingMeasureCount = 0
            var movingPlacementCount = 0

            setContent {
                Box(
                    modifier = Modifier
                        .size(400.dp, 300.dp)
                        .deponderRoot(state),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = 100.dp, y = 140.dp)
                            .size(20.dp)
                            .deponderPlanet("anchor", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f)),
                    )
                    Layout(
                        modifier = Modifier
                            .offset(x = 150.dp, y = 140.dp)
                            .deponderPlanet("mover", PlanetPhysics(pinned = false, repulsionRadiusDp = 0f))
                            .deponderDrag(),
                    ) { _, _ ->
                        movingMeasureCount += 1
                        layout(20, 20) {
                            movingPlacementCount += 1
                        }
                    }
                    Box(
                        modifier = Modifier
                            .deponderRubber("anchor", "mover", RubberPhysics.fixed(180f), DeponderRubberInsets.None)
                            .deponderStretch(),
                    ) {
                        Box(modifier = Modifier.size(20.dp, 2.dp))
                    }
                }
            }

            waitForIdle()
            val measureBaseline = movingMeasureCount
            val placeBaseline = movingPlacementCount
            val settledBeforeX = state.world.bodyOf("mover")!!.x
            val displacedX = settledBeforeX - 100f

            // Displace mover toward anchor so multi-frame spring integration runs continuously until sleep.
            state.moveTo("mover", displacedX, state.world.bodyOf("mover")!!.y)
            waitForIdle()

            val settledAfterX = state.world.bodyOf("mover")!!.x
            assertTrue(
                settledAfterX > displacedX + 40f,
                "Spring must actively integrate over multiple frames (displacedX=$displacedX, settledAfterX=$settledAfterX)",
            )
            assertEquals(measureBaseline, movingMeasureCount, "Multi-frame physics motion must trigger 0 remeasurements")
            assertEquals(placeBaseline, movingPlacementCount, "Multi-frame physics motion must trigger 0 relayouts")
        }
}
