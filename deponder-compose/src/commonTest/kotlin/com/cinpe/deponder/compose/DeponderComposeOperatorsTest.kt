package com.cinpe.deponder.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.DeponderRubberInsets
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeponderComposeOperatorsTest {

    @Test
    fun deponderState_clampsScale_updatesPhysicsPreservingBodies_andResolvesRubberInsets() = runTest {
        val state = DeponderState(
            RootPhysics(
                initScale = 1.0f,
                minScale = 0.6f,
                maxScale = 1.8f,
            ),
        )
        state.updateViewportSize(800, 600)
        state.updateDensity(2.0f)

        val pA = DeponderPlanetNode("a", PlanetPhysics(repulsionRadiusDp = 0f))
        val pB = DeponderPlanetNode("b", PlanetPhysics(repulsionRadiusDp = 0f))
        state.registerPlanetNode(pA)
        state.registerPlanetNode(pB)
        state.syncPlanetPlacement(pA, 200f, 300f)
        state.syncPlanetPlacement(pB, 400f, 300f)

        val rAB = DeponderRubberNode("a", "b", RubberPhysics(naturalLengthDp = 80f), DeponderRubberInsets.Default)
        val pose = state.registerRubberNode(rAB)
        assertTrue(pose.isActive)
        assertEquals(200f, rAB.currentSpanLengthPx(pose), 1e-3f)

        // Updating planet measured sizes updates directional contour radii on RubberPose!
        state.updatePlanetMeasuredSize("a", widthPx = 40, heightPx = 40)
        state.updatePlanetMeasuredSize("b", widthPx = 40, heightPx = 40)
        // With density = 2.0f, default gap = 4dp -> 8px -> each clearance = 20 + 8 = 28px -> 200 - 56 = 144px!
        assertEquals(144f, rAB.currentSpanLengthPx(pose), 1e-3f)
        assertEquals(200f, pose.centerDistance, 1e-3f)

        // Updating insets to None spans full 200px center-to-center; endOnly(4f) insets only `b` (200 - 28 = 172px);
        // fixed(0f, 25f) at density = 2.0f insets `b` by 50px (200 - 50 = 150px).
        rAB.update("a", "b", RubberPhysics(naturalLengthDp = 80f), DeponderRubberInsets.None)
        assertEquals(200f, rAB.currentSpanLengthPx(pose), 1e-3f)
        rAB.update("a", "b", RubberPhysics(naturalLengthDp = 80f), DeponderRubberInsets.endOnly(4f))
        assertEquals(172f, rAB.currentSpanLengthPx(pose), 1e-3f)
        rAB.update("a", "b", RubberPhysics(naturalLengthDp = 80f), DeponderRubberInsets.fixed(0f, 25f))
        assertEquals(150f, rAB.currentSpanLengthPx(pose), 1e-3f)

        state.submitRubberElasticity(3.6f)
        assertEquals(3.6f, pose.spec.elasticity, 1e-4f)

        state.submitScale(5.0f)
        assertEquals(1.8f, state.scale, 1e-4f)
        state.submitScale(0.1f)
        assertEquals(0.6f, state.scale, 1e-4f)

        // Updating RootPhysics preserves existing body coordinates while updating bounds.
        state.moveTo("a", 250f, 320f)
        state.updatePhysics(
            RootPhysics.fromPreset(DeponderDefaults.Presets.GalaxyInfinite)
                .withScaleBounds(initScale = 1.0f, minScale = 0.8f, maxScale = 1.4f),
        )
        assertEquals(0.8f, state.scale, 1e-4f)
        assertEquals(250f, state.world.bodyOf("a")!!.x, 1e-4f)
        assertEquals(320f, state.world.bodyOf("a")!!.y, 1e-4f)

        state.dragBy("a", 30f, -20f)
        assertEquals(280f, state.world.bodyOf("a")!!.x, 1e-4f)
        assertEquals(300f, state.world.bodyOf("a")!!.y, 1e-4f)

        assertFailsWith<IllegalArgumentException> { state.submitScale(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { state.submitRubberElasticity(Float.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { state.dragBy("a", Float.NaN, 0f) }
        assertFailsWith<IllegalArgumentException> { state.moveTo("a", 0f, Float.NEGATIVE_INFINITY) }
    }

    @Test
    fun deponderState_rejectsDuplicatePlanetIds_andDuplicateUndirectedRubbers() {
        val state = DeponderState()
        val firstPlanet = DeponderPlanetNode("dup", PlanetPhysics())
        val secondPlanet = DeponderPlanetNode("dup", PlanetPhysics())
        state.registerPlanetNode(firstPlanet)

        assertFailsWith<IllegalStateException> {
            state.registerPlanetNode(secondPlanet)
        }

        val forwardRubber = DeponderRubberNode("n1", "n2", RubberPhysics(), DeponderRubberInsets.Default)
        val reverseRubber = DeponderRubberNode("n2", "n1", RubberPhysics(), DeponderRubberInsets.Default)
        state.registerRubberNode(forwardRubber)

        // Undirected EdgeKey("n1", "n2") == EdgeKey("n2", "n1") must fail fast!
        assertFailsWith<IllegalStateException> {
            state.registerRubberNode(reverseRubber)
        }

        // Unregistering frees the EdgeKey cleanly for reuse.
        state.unregisterRubberNode(forwardRubber)
        val reRegistered = state.registerRubberNode(reverseRubber)
        assertEquals(EdgeKey("n1", "n2"), reRegistered.spec.key)
    }

    @Test
    fun stretchMath_guardsZeroWidthScaleLength_andAlignsNonCenteredChildToRubberCenter() {
        assertEquals(0f, computeStretchScaleX(lengthPx = 180f, widthPx = 0, parentScale = 1f), 0f)
        assertEquals(0f, computeStretchScaleX(lengthPx = 180f, widthPx = 10, parentScale = 0f), 0f)
        assertEquals(0f, computeStretchScaleX(lengthPx = 0f, widthPx = 10, parentScale = 1f), 0f)
        assertEquals(15f, computeStretchScaleX(lengthPx = 180f, widthPx = 10, parentScale = 1.2f), 1e-4f)

        // A 20px-wide child placed at left = 0 inside a 100px-wide rubber shifts +40px so its center hits x = 50px.
        val alignTx = computeChildStretchCenterAlignTx(
            rubberWidthPx = 100,
            childLeftInRubberPx = 0f,
            childWidthPx = 20,
        )
        assertEquals(40f, alignTx, 1e-4f)
    }

    @Test
    fun rubberStyleDefaultsTransparent_andArrowStemInsetPreventsTipBleed() {
        assertEquals(Color.Transparent, DeponderRubberStyle.Default.labelBackgroundColor)
        assertEquals(Color.Transparent, DeponderRubberStyle.DefaultLabelBackgroundColor)

        val insetNormal = computeComposeArrowStemInset(effArrowW = 10f, effArrowH = 13.5f, lineThicknessPx = 2f)
        assertTrue(insetNormal > 7f && insetNormal < 10f)

        // Extra-thick line stops right at arrow flat base so corners never poke through arrow sides.
        val insetThick = computeComposeArrowStemInset(effArrowW = 10f, effArrowH = 13.5f, lineThicknessPx = 12f)
        assertEquals(10f, insetThick, 1e-4f)

        // Short span shrinks arrows proportionally so two arrows never cross past each other.
        assertEquals(
            0.5f,
            computeComposeArrowScale(localSpan = 10f, arrowLengthPx = 10f, hasStart = true, hasEnd = true),
            1e-4f,
        )

        // Center-anchored endpoints (`Mode.NONE`) automatically omit built-in directional arrowheads!
        assertFalse(isComposeStartArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.None))
        assertFalse(isComposeEndArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.None))
        assertFalse(isComposeStartArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.endOnly()))
        assertTrue(isComposeEndArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.endOnly()))
        assertFalse(isComposeStartArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.fixed(0f, 24f)))
        assertTrue(isComposeEndArrowEnabled(DeponderArrowMode.BOTH, DeponderRubberInsets.fixed(0f, 24f)))

        val s1 = DeponderRubberStyle.of(
            lineColor = Color(0xFF5C6BC0),
            lineThickness = 3.dp,
            labelTextSize = 14.sp,
        )
        val s2 = DeponderRubberStyle.of(
            lineColor = Color(0xFF5C6BC0),
            lineThickness = 3.dp,
            labelTextSize = 14.sp,
        )
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun controlPlaneCombine_preservesUnsettledEnergyAcrossRootDetachReattach_andStaysAsleepOnceSettled() = runTest {
        val state = DeponderState(RootPhysics(airDamping = 8f, wallRepulsionDp = 0f))
        val rootNode = DeponderRootNode(state)
        state.attachRoot(rootNode, density = 1f)
        state.updateViewportSize(800, 600)
        assertFalse(state.isAwake)
        assertFalse(state.loopGateFlow.first())

        val pA = DeponderPlanetNode("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
        val pB = DeponderPlanetNode("b", PlanetPhysics(pinned = false, repulsionRadiusDp = 0f))
        state.registerPlanetNode(pA)
        state.registerPlanetNode(pB)
        state.syncPlanetPlacement(pA, 100f, 300f)
        state.syncPlanetPlacement(pB, 220f, 300f)

        val rubber = DeponderRubberNode("a", "b", RubberPhysics.fixed(100f, elasticity = 12f), DeponderRubberInsets.Default)
        state.registerRubberNode(rubber)

        assertTrue(state.isAwake)
        assertTrue(state.loopGateFlow.first())

        // Detaching host gates loop off without clearing unsettled mechanical energy.
        state.detachRoot(rootNode)
        assertFalse(state.isAwake)
        assertFalse(state.loopGateFlow.first())

        // Re-attaching resumes cleanly without needing an external reheat!
        state.attachRoot(rootNode, density = 1f)
        assertTrue(state.isAwake)
        assertTrue(state.loopGateFlow.first())

        var active = true
        var guard = 0
        while (active && guard < 800) {
            active = state.stepFrame(0.016f)
            guard += 1
        }
        assertFalse(active, "Critically damped system must settle within 800 steps (guard=$guard)")
        assertFalse(state.isAwake)
        assertFalse(state.loopGateFlow.first())

        // Once settled, re-attaching host keeps loop asleep at 0% CPU.
        state.detachRoot(rootNode)
        assertFalse(state.isAwake)
        state.attachRoot(rootNode, density = 1f)
        assertFalse(state.isAwake)
        assertFalse(state.loopGateFlow.first())

        // Settled unchanged elasticity & planet contour size updates keep settled loop asleep!
        state.submitRubberElasticity(12f)
        assertFalse(state.isAwake)
        state.updatePlanetMeasuredSize("a", 80, 80)
        assertFalse(state.isAwake)

        // Mid-drag host detach clears `isPressed`, allowing subsequent re-attach to relax and sleep!
        state.setPressed("b", true)
        assertTrue(state.world.bodyOf("b")!!.isPressed)
        state.detachRoot(rootNode)
        assertFalse(state.world.bodyOf("b")!!.isPressed)
        assertFalse(state.isAwake)
        state.attachRoot(rootNode, density = 1f)
        assertTrue(state.isAwake)
        var resumedActive = true
        var resumeGuard = 0
        while (resumedActive && resumeGuard < 800) {
            resumedActive = state.stepFrame(0.016f)
            resumeGuard += 1
        }
        assertFalse(resumedActive, "Released body after detach/reattach must settle to sleep")
        assertFalse(state.isAwake)

        // Runtime setPinned state survives dynamic density changes!
        state.setPinned("b", true)
        assertTrue(state.world.bodyOf("b")!!.spec.pinned)
        state.updateDensity(3.0f)
        assertTrue(state.world.bodyOf("b")!!.spec.pinned)

        // Style guards on DeponderRubberStyle.of:
        assertFailsWith<IllegalArgumentException> {
            DeponderRubberStyle.of(lineThickness = Dp.Unspecified)
        }
        assertFailsWith<IllegalArgumentException> {
            DeponderRubberStyle.of(arrowSize = (-2).dp)
        }
        assertFailsWith<IllegalArgumentException> {
            DeponderRubberStyle.of(labelTextSize = TextUnit.Unspecified)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun deponderState_unfold_untanglesPreMountAndRuntimeCrossedCycle_whileWakeAndReheatPreserveLayout() = runTest {
        val state = DeponderState(RootPhysics())

        // Pre-mount unfold() call before viewport or nodes are attached remains armed until first frame!
        state.unfold()

        state.updateViewportSize(600, 600)
        state.updateDensity(1.0f)

        val nodes = listOf(
            DeponderPlanetNode("c0", PlanetPhysics()),
            DeponderPlanetNode("c1", PlanetPhysics()),
            DeponderPlanetNode("c2", PlanetPhysics()),
            DeponderPlanetNode("c3", PlanetPhysics()),
        )
        nodes.forEach { state.registerPlanetNode(it) }
        // Crossed bowtie placement: (0,1) crosses (2,3)
        state.syncPlanetPlacement(nodes[0], 200f, 200f)
        state.syncPlanetPlacement(nodes[1], 400f, 400f)
        state.syncPlanetPlacement(nodes[2], 200f, 400f)
        state.syncPlanetPlacement(nodes[3], 400f, 200f)

        for ((s, e) in listOf("c0" to "c1", "c1" to "c2", "c2" to "c3", "c3" to "c0")) {
            state.registerRubberNode(
                DeponderRubberNode(s, e, RubberPhysics.fixed(120f), DeponderRubberInsets.Default),
            )
        }
        assertTrue(state.world.bodies.isNotEmpty())

        var active = true
        var guard = 0
        while (active && guard < 1800) {
            active = state.stepFrame(0.016f)
            guard += 1
        }
        assertFalse(active, "Unfolded cycle via DeponderState should settle to 0% CPU sleep (guard=$guard)")

        val b0 = state.world.bodyOf("c0")!!
        val b1 = state.world.bodyOf("c1")!!
        val b2 = state.world.bodyOf("c2")!!
        val b3 = state.world.bodyOf("c3")!!
        fun orient(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float =
            (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        fun hasCrossing(): Boolean {
            val cross0123 =
                (orient(b0.x, b0.y, b1.x, b1.y, b2.x, b2.y) * orient(b0.x, b0.y, b1.x, b1.y, b3.x, b3.y) < 0f) &&
                    (orient(b2.x, b2.y, b3.x, b3.y, b0.x, b0.y) * orient(b2.x, b2.y, b3.x, b3.y, b1.x, b1.y) < 0f)
            val cross1230 =
                (orient(b1.x, b1.y, b2.x, b2.y, b3.x, b3.y) * orient(b1.x, b1.y, b2.x, b2.y, b0.x, b0.y) < 0f) &&
                    (orient(b3.x, b3.y, b0.x, b0.y, b1.x, b1.y) * orient(b3.x, b3.y, b0.x, b0.y, b2.x, b2.y) < 0f)
            return cross0123 || cross1230
        }
        assertFalse(hasCrossing(), "Pre-mount state.unfold() must untangle cycle before settling")

        // Verify both submitScale(1.25f) and reheat() preserve manual crossed arrangement,
        // while explicit state.unfold() untangles it cleanly.
        state.moveTo("c0", 200f, 200f)
        state.moveTo("c1", 400f, 400f)
        state.moveTo("c2", 200f, 400f)
        state.moveTo("c3", 400f, 200f)
        state.submitScale(1.25f)
        var scaleActive = true
        var scaleGuard = 0
        while (scaleActive && scaleGuard < 1800) {
            scaleActive = state.stepFrame(0.016f)
            scaleGuard += 1
        }
        assertFalse(scaleActive)
        assertTrue(hasCrossing(), "submitScale() must not erase manual layout via unfold")

        // Deprecated no-op state.reheat() must preserve manual layout when unfold() is not invoked.
        state.reheat()
        assertFalse(state.isAwake)
        assertTrue(hasCrossing(), "deprecated reheat() no-op must preserve manual layout")

        state.unfold()
        var unfoldActive = true
        var unfoldGuard = 0
        while (unfoldActive && unfoldGuard < 1800) {
            unfoldActive = state.stepFrame(0.016f)
            unfoldGuard += 1
        }
        assertFalse(unfoldActive)
        assertFalse(hasCrossing(), "state.unfold() must untangle crossed cycle")
    }

    @Test
    @Suppress("DEPRECATION")
    fun deponderState_deprecatedWithUnfoldToggle_preservesTransitionalBehavior() = runTest {
        val state = DeponderState(RootPhysics())
        assertFalse(state.physics.unfoldEnabled)
        assertFalse(state.world.environment.unfoldEnabled)

        state.updateViewportSize(600, 600)
        state.updateDensity(1.0f)

        val nodes = listOf(
            DeponderPlanetNode("c0", PlanetPhysics()),
            DeponderPlanetNode("c1", PlanetPhysics()),
            DeponderPlanetNode("c2", PlanetPhysics()),
            DeponderPlanetNode("c3", PlanetPhysics()),
        )
        nodes.forEach { state.registerPlanetNode(it) }
        state.syncPlanetPlacement(nodes[0], 200f, 200f)
        state.syncPlanetPlacement(nodes[1], 400f, 400f)
        state.syncPlanetPlacement(nodes[2], 200f, 400f)
        state.syncPlanetPlacement(nodes[3], 400f, 200f)
        for ((s, e) in listOf("c0" to "c1", "c1" to "c2", "c2" to "c3", "c3" to "c0")) {
            state.registerRubberNode(
                DeponderRubberNode(s, e, RubberPhysics.fixed(120f), DeponderRubberInsets.Default),
            )
        }

        state.updatePhysics(state.physics.withUnfold(true))
        assertTrue(state.physics.unfoldEnabled)
        assertTrue(state.world.environment.unfoldEnabled)

        var active = true
        var guard = 0
        while (active && guard < 1800) {
            active = state.stepFrame(0.016f)
            guard += 1
        }
        assertFalse(active)
        val b0 = state.world.bodyOf("c0")!!
        val b1 = state.world.bodyOf("c1")!!
        val b2 = state.world.bodyOf("c2")!!
        val b3 = state.world.bodyOf("c3")!!
        fun orient(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float =
            (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        val cross0123 =
            (orient(b0.x, b0.y, b1.x, b1.y, b2.x, b2.y) * orient(b0.x, b0.y, b1.x, b1.y, b3.x, b3.y) < 0f) &&
                (orient(b2.x, b2.y, b3.x, b3.y, b0.x, b0.y) * orient(b2.x, b2.y, b3.x, b3.y, b1.x, b1.y) < 0f)
        val cross1230 =
            (orient(b1.x, b1.y, b2.x, b2.y, b3.x, b3.y) * orient(b1.x, b1.y, b2.x, b2.y, b0.x, b0.y) < 0f) &&
                (orient(b3.x, b3.y, b0.x, b0.y, b1.x, b1.y) * orient(b3.x, b3.y, b0.x, b0.y, b2.x, b2.y) < 0f)
        assertFalse(cross0123 || cross1230, "Deprecated withUnfold(true) toggle must remain functional")
    }
}
