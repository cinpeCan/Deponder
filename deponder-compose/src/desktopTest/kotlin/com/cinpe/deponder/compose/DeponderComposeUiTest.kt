package com.cinpe.deponder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
                        physics = RubberPhysics(naturalLengthDp = 120f, endpointGapDp = 0f),
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
            assertTrue(poseBefore.isActive)
            assertTrue(poseBefore.length > 0f && poseBefore.length < poseBefore.centerDistance)

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
                    }

                    // Upstream direct stretch: .deponderStretch().deponderRubber(...)
                    Box(
                        modifier = Modifier
                            .size(12.dp, 2.dp)
                            .deponderStretch()
                            .deponderRubber("a", "b", RubberPhysics(endpointGapDp = 0f)),
                    )

                    // Compound Box rubber with fillMaxWidth() stretched stem + 80px non-stretched badge + nested grandchild
                    Box(
                        modifier = Modifier.deponderRubber("b", "c", RubberPhysics(endpointGapDp = 0f)),
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

                    // Fixed-width compound Box rubber (80px outer width > 20px stretched stem)
                    Layout(
                        content = {
                            Layout(modifier = Modifier.deponderStretch()) { _, _ ->
                                layout(20, 2) {}
                            }
                        },
                        modifier = Modifier.deponderRubber("a", "c", RubberPhysics(endpointGapDp = 0f)),
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
            // Even with fillMaxWidth() on the stretched line inside a 400dp root, rubberBc wraps the 80px badge
            // and shrinks cleanly to 40 / 80 = 0.5f!
            assertEquals(80, rubberBc.measuredWidthPx)
            assertEquals(0.5f, rubberBc.computeEffectiveContainerScale(poseBc), 0.05f)

            state.moveTo("a", 200f, 220f)
            state.moveTo("c", 260f, 220f)
            waitForIdle()
            val rubberAc = state.activeRubbersByKey[EdgeKey("a", "c")]!!
            val poseAc = state.world.rubberOf(EdgeKey("a", "c"))!!
            assertEquals(0.5f, rubberAc.computeEffectiveContainerScale(poseAc), 0.05f)
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
}
