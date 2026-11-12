package com.cinpe.deponder.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeponderComposeOperatorsTest {

    @Test
    fun deponderState_clampsScale_updatesPhysicsPreservingBodies_andDragsSmoothly() = runTest {
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

        val rAB = DeponderRubberNode("a", "b", RubberPhysics(naturalLengthDp = 80f))
        val pose = state.registerRubberNode(rAB)
        assertTrue(pose.isActive)
        assertEquals(200f, pose.length, 1e-3f)

        // Updating planet measured sizes automatically clears planet contours from RubberPose.length!
        state.updatePlanetMeasuredSize("a", widthPx = 40, heightPx = 40)
        state.updatePlanetMeasuredSize("b", widthPx = 40, heightPx = 40)
        // With density = 2.0f, endpointGapDp = 4f -> gapPx = 8f -> each clearance = 20 + 8 = 28px -> 200 - 56 = 144px!
        assertEquals(144f, pose.length, 1e-3f)
        assertEquals(200f, pose.centerDistance, 1e-3f)

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

        val forwardRubber = DeponderRubberNode("n1", "n2", RubberPhysics())
        val reverseRubber = DeponderRubberNode("n2", "n1", RubberPhysics())
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
}
