package com.cinpe.deponder

import android.app.Activity
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import java.time.Duration
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class DeponderOperatorsTest {

    @Before
    fun setUp() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
    }

    private inline fun mountAttachedRoot(
        width: Int = 800,
        height: Int = 600,
        block: (Activity, FrameLayout) -> Unit,
    ) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        val root = FrameLayout(activity)
        activity.setContentView(root, FrameLayout.LayoutParams(width, height))
        ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)
        block(activity, root)
        controller.destroy()
    }

    @Test
    fun nonDirectChildPlanetAndRubber_andOrphanXmlStretch_failFastWithClearDiagnostic() {
        mountAttachedRoot { activity, root ->
            root.deponderRoot()
            val nestedWrapper = LinearLayout(activity)
            root.addView(nestedWrapper, FrameLayout.LayoutParams(400, 200))

            val nestedPlanet = TextView(activity)
            nestedWrapper.addView(nestedPlanet, LinearLayout.LayoutParams(80, 40))

            try {
                nestedPlanet.deponderPlanet("nested")
                fail("Expected IllegalStateException when planet is not a direct child of deponderRoot")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("DIRECT child"))
            }

            // Orphan XML @string/deponder_stretch outside a deponderRubber must fail fast!
            val orphanStretchChild = View(activity).apply {
                tag = activity.getString(R.string.deponder_stretch)
            }
            val nonRubberPlanet = FrameLayout(activity).apply {
                addView(orphanStretchChild, FrameLayout.LayoutParams(20, 4))
            }
            root.addView(nonRubberPlanet, FrameLayout.LayoutParams(60, 40))
            try {
                nonRubberPlanet.deponderPlanet("bad_stretch_host")
                fail("Expected orphan @string/deponder_stretch inside a non-rubber subtree to fail fast")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("deponderRubber"))
            }
            root.removeView(nonRubberPlanet)

            // Pre-attaching deponderDrag() before root.addView(...) + deponderPlanet(...) must mount cleanly!
            val preDragHandle = View(activity).apply { deponderDrag() }
            val validCardPlanet = FrameLayout(activity).apply {
                addView(preDragHandle, FrameLayout.LayoutParams(20, 20))
            }
            root.addView(validCardPlanet, FrameLayout.LayoutParams(60, 40))
            validCardPlanet.deponderPlanet("pre_drag_ok")

            // Orphan deponderDrag() on an unbound direct child must fail fast on layout sweep!
            val orphanDragView = View(activity).apply { deponderDrag() }
            root.addView(orphanDragView, FrameLayout.LayoutParams(40, 40))
            try {
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, 800, 600)
                fail("Expected unbound orphan deponderDrag() direct child to fail fast on layout")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("deponderPlanet"))
            } finally {
                root.removeView(orphanDragView)
            }
        }
    }

    @Test
    fun duplicatePlanetId_andDuplicateUndirectedRubber_failFast() {
        mountAttachedRoot { activity, root ->
            root.deponderRoot()

            val p1 = View(activity).also { root.addView(it, FrameLayout.LayoutParams(40, 40)) }
            val p2 = View(activity).also { root.addView(it, FrameLayout.LayoutParams(40, 40)) }
            p1.deponderPlanet("same")

            try {
                p2.deponderPlanet("same")
                fail("Expected duplicate planet id to throw IllegalStateException")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("Duplicate deponderPlanet"))
            }

            p2.deponderPlanet("other")

            val r1 = DeponderRubberView(activity).also { root.addView(it) }
            val r2 = DeponderRubberView(activity).also { root.addView(it) }
            r1.deponderRubber("same", "other")

            try {
                r2.deponderRubber("other", "same")
                fail("Expected duplicate undirected rubber EdgeKey to throw IllegalStateException")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("Duplicate deponderRubber"))
            }
        }
    }

    @Test
    fun outerSpanningCompoundRubber_preventsHostClipChildrenTruncation_andPrunesNestedStretch() {
        mountAttachedRoot { activity, root ->
            // Host `root` retains Android's default `clipChildren == true` untouched!
            assertTrue(root.clipChildren)
            val ctrl = root.deponderRoot(physics = RootPhysics(initScale = 1.2f, minScale = 0.5f, maxScale = 2.0f))

            val pA = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(80, 280, 120, 320) // static center (100, 300), halfW = 20
                it.deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            val pB = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(380, 280, 420, 320) // static center (400, 300), halfW = 20 -> centerDist = 300px
                it.deponderPlanet("b", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            assertEquals(0f, pA.translationX, 1e-4f)
            assertEquals(0f, pB.translationX, 1e-4f)

            // Custom FrameLayout rubber (width 100px) with left-aligned 20px stretched line, nested 10px grandchild,
            // and centered 50px non-stretched badge label.
            val customRubber = FrameLayout(activity)
            val outerStretched = FrameLayout(activity).apply {
                tag = activity.getString(R.string.deponder_stretch)
            }
            val innerGrandChild = View(activity).apply {
                deponderStretch(true)
            }
            val nonStretchedBadge = TextView(activity)
            outerStretched.addView(innerGrandChild, FrameLayout.LayoutParams(10, 4))
            customRubber.addView(outerStretched, FrameLayout.LayoutParams(20, 4))
            customRubber.addView(nonStretchedBadge, FrameLayout.LayoutParams(50, 16))
            root.addView(customRubber, FrameLayout.LayoutParams(100, 20))
            customRubber.layout(0, 0, 100, 20)
            outerStretched.layout(0, 8, 20, 12)
            innerGrandChild.layout(0, 0, 10, 4)
            nonStretchedBadge.layout(25, 2, 75, 18)

            customRubber.deponderRubber(
                "a",
                "b",
                RubberPhysics(naturalLengthDp = 100f, endpointGapDp = 0f),
            )
            ctrl.applyCurrentPosesToViews()

            // Contour-cleared visible span: 300 - (20 * 1.2) - (20 * 1.2) = 252px!
            val pose = ctrl.world.rubberOf(EdgeKey("a", "b"))!!
            assertEquals(300f, pose.centerDistance, 1e-3f)
            assertEquals(252f, pose.length, 1e-3f)

            // Outer RenderNode spans full `252px` (`scaleX = 252 / 100 = 2.52`) so `root.clipChildren = true` never clips!
            assertEquals(-1f, customRubber.translationZ, 1e-4f)
            assertEquals(2.52f, customRubber.scaleX, 1e-4f)
            assertEquals(1.2f, customRubber.scaleY, 1e-4f)
            assertEquals(200f, customRubber.translationX, 1e-4f) // 250 - (0 + 50)

            // Stretched child fills `[0, 100]` locally (`scaleX = 100 / 20 = 5.0`), mapping to `20 * 5.0 * 2.52 = 252px` on screen!
            assertEquals(5.0f, outerStretched.scaleX, 1e-4f)
            assertEquals(1.0f, outerStretched.scaleY, 1e-4f)
            assertEquals(40f, outerStretched.translationX, 1e-4f)

            // Inner grandchild is pruned because ancestor `outerStretched` already stretches -> stays 1f!
            assertEquals(1.0f, innerGrandChild.scaleX, 1e-4f)
            assertEquals(0f, innerGrandChild.translationX, 1e-4f)

            // Non-stretched badge counter-scales by `1.2 / 2.52` so screen scale is undistorted `(1.2, 1.2)`!
            assertEquals(1.2f, nonStretchedBadge.scaleX * customRubber.scaleX, 1e-4f)
            assertEquals(1.2f, nonStretchedBadge.scaleY * customRubber.scaleY, 1e-4f)

            bDetachCheck(root, pB, customRubber)
        }
    }

    @Test
    fun customCompoundRubber_autoShrinksBadge_andSleepingLabelMutationRefreshesImmediately() {
        mountAttachedRoot { activity, root ->
            val ctrl = root.deponderRoot()

            val pA = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(80, 280, 120, 320) // center (100, 300), halfW = 20
                it.deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            val pB = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(160, 280, 200, 320) // center (180, 300), halfW = 20 -> centerDist = 80px, visible = 40px
                it.deponderPlanet("b", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            assertTrue(pA.width == 40 && pB.width == 40)

            val customCompound = FrameLayout(activity)
            val stretchedLine = View(activity).apply {
                tag = activity.getString(R.string.deponder_stretch)
            }
            val badgeLabel = TextView(activity)
            customCompound.addView(stretchedLine, FrameLayout.LayoutParams(200, 2))
            customCompound.addView(badgeLabel, FrameLayout.LayoutParams(80, 18))
            root.addView(customCompound, FrameLayout.LayoutParams(200, 20))
            customCompound.layout(0, 0, 200, 20)
            stretchedLine.layout(0, 9, 200, 11)
            badgeLabel.layout(60, 1, 140, 19)

            customCompound.deponderRubber("a", "b", RubberPhysics(endpointGapDp = 0f))
            ctrl.applyCurrentPosesToViews()

            assertEquals(0.2f, customCompound.scaleX, 1e-4f)
            assertEquals(0.5f, customCompound.scaleY, 1e-4f)
            assertEquals(40f, 200f * stretchedLine.scaleX * customCompound.scaleX, 1e-3f)
            assertEquals(0.5f, badgeLabel.scaleX * customCompound.scaleX, 1e-3f)

            // Built-in DeponderRubberView with fixed LayoutParams updates scaleY immediately when label grows while sleeping!
            val pC = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(220, 280, 260, 320) // center (240, 300) -> visible gap from B(180) is 60 - 40 = 20px
                it.deponderPlanet("c", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            assertTrue(pC.width == 40)
            val defaultRubber = DeponderRubberView(activity)
                .withLabel("X")
                .withArrowMode(DeponderArrowMode.BOTH)
            root.addView(defaultRubber, FrameLayout.LayoutParams(80, 40))
            defaultRubber.measure(
                View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(40, View.MeasureSpec.EXACTLY),
            )
            defaultRubber.layout(0, 0, 80, 40)
            defaultRubber.deponderRubber("b", "c", RubberPhysics(endpointGapDp = 0f))
            ctrl.applyCurrentPosesToViews()

            assertEquals(Color.TRANSPARENT, defaultRubber.labelBackgroundColor)
            assertEquals(20f, defaultRubber.width * defaultRubber.scaleX, 1e-3f)
            val initialScaleY = defaultRubber.scaleY

            // Mutate label to a very wide string without running another physics frame:
            defaultRubber.label = "VERY_WIDE_SLEEPING_LABEL_TEXT_12345"
            assertTrue(
                "Updating label while sleeping with fixed bounds must shrink effectiveScale (${defaultRubber.scaleY} < $initialScaleY)",
                defaultRubber.scaleY < initialScaleY,
            )

            ctrl.submitRubberElasticity(4.2f)
            assertEquals(4.2f, ctrl.world.rubberOf(EdgeKey("a", "b"))!!.spec.elasticity, 1e-4f)
        }
    }

    @Test
    fun elevationAwareZOrdering_elevatesDraggedPlanetAboveCardElevation_andKeepsElevatedRubberUnderneath() {
        mountAttachedRoot { activity, root ->
            val ctrl = root.deponderRoot()

            val lowPlanet = View(activity).apply { elevation = 0f }
            val highCardPlanet = View(activity).apply { elevation = 24f }
            val elevatedRubber = View(activity).apply { elevation = 12f }

            root.addView(lowPlanet, FrameLayout.LayoutParams(40, 40))
            root.addView(highCardPlanet, FrameLayout.LayoutParams(40, 40))
            root.addView(elevatedRubber, FrameLayout.LayoutParams(40, 10))
            lowPlanet.layout(80, 280, 120, 320)
            highCardPlanet.layout(280, 280, 320, 320)
            elevatedRubber.layout(0, 0, 40, 10)

            lowPlanet.deponderPlanet("low", PlanetPhysics(pinned = true))
            highCardPlanet.deponderPlanet("card", PlanetPhysics(pinned = true))
            elevatedRubber.deponderRubber("low", "card")
            ctrl.applyCurrentPosesToViews()

            // Even with elevation=12f on rubber, total rubber Z (elevation + translationZ) must sit strictly below lowPlanet (0f)!
            assertTrue(elevatedRubber.elevation + elevatedRubber.translationZ < lowPlanet.elevation + lowPlanet.translationZ)

            // Bringing lowPlanet (elevation=0f) to front must exceed highCardPlanet (elevation=24f)!
            ctrl.bringToFront("low")
            assertTrue(lowPlanet.elevation + lowPlanet.translationZ > highCardPlanet.elevation + highCardPlanet.translationZ)
        }
    }

    private fun bDetachCheck(root: FrameLayout, pB: View, customRubber: FrameLayout) {
        root.removeView(pB)
        assertEquals(0f, customRubber.alpha, 1e-4f)
        assertEquals(0f, pB.translationX, 1e-4f)
        assertEquals(1f, pB.scaleX, 1e-4f)

        customRubber.clearDeponderRubber()
        assertEquals(1f, customRubber.alpha, 1e-4f)
        assertEquals(0f, customRubber.translationZ, 1e-4f)
    }

    @Test
    fun dragProbe_disallowsParentInterceptOnDown_preservesClick_elevatesZ_andDragsInRootSpace() = runTest {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        var disallowInterceptRequestedOnDown = false
        val scrollWrapper = object : ScrollView(activity) {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                if (disallowIntercept) disallowInterceptRequestedOnDown = true
                super.requestDisallowInterceptTouchEvent(disallowIntercept)
            }
        }
        val root = FrameLayout(activity)
        scrollWrapper.addView(root, FrameLayout.LayoutParams(800, 600))
        activity.setContentView(scrollWrapper, FrameLayout.LayoutParams(800, 600))
        ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS)
        scrollWrapper.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        scrollWrapper.layout(0, 0, 800, 600)

        val ctrl = root.deponderRoot()
        var clickedCount = 0

        val planetA = TextView(activity).apply {
            setOnClickListener { clickedCount += 1 }
        }
        val planetB = View(activity)
        root.addView(planetA, FrameLayout.LayoutParams(60, 60))
        root.addView(planetB, FrameLayout.LayoutParams(60, 60))
        planetA.layout(170, 270, 230, 330) // center (200, 300)
        planetB.layout(370, 270, 430, 330) // center (400, 300)

        planetA.deponderPlanet("a", PlanetPhysics(repulsionRadiusDp = 0f))
        planetB.deponderPlanet("b", PlanetPhysics(repulsionRadiusDp = 0f))
        planetA.deponderDrag(enabled = true, bringToFrontOnPress = true)

        val rubber = DeponderRubberView(activity)
            .withLabel("links")
            .withArrowMode(DeponderArrowMode.BOTH)
        root.addView(rubber, FrameLayout.LayoutParams(120, 30))
        rubber.layout(0, 0, 120, 30)
        rubber.deponderRubber("a", "b", RubberPhysics(naturalLengthDp = 100f))

        // 1. Tap without exceeding touchSlop -> disallows parent intercept immediately on ACTION_DOWN,
        // preserves native OnClickListener, AND elevates translationZ!
        val now = SystemClock.uptimeMillis()
        val downTap = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 30f, 30f, 0)
        planetA.dispatchTouchEvent(downTap)
        assertTrue("ACTION_DOWN must immediately disallow outer scroll interception", disallowInterceptRequestedOnDown)

        val upTap = MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, 31f, 30f, 0)
        planetA.dispatchTouchEvent(upTap)
        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
        downTap.recycle()
        upTap.recycle()

        assertEquals(1, clickedCount)
        assertTrue(planetA.translationZ > planetB.translationZ)

        // 2. Fast drag (<100ms TapTimeout) inside ScrollView -> cancels pending CheckForTap / pre-pressed ripple
        // so `planetA.isPressed` never sticks on even after advancing looper past TapTimeout!
        val dragStart = SystemClock.uptimeMillis()
        val dragDown = MotionEvent.obtain(dragStart, dragStart, MotionEvent.ACTION_DOWN, 30f, 30f, 0)
        val dragMove = MotionEvent.obtain(dragStart, dragStart + 20, MotionEvent.ACTION_MOVE, 90f, 30f, 0)
        planetA.dispatchTouchEvent(dragDown)
        planetA.dispatchTouchEvent(dragMove)
        ShadowLooper.idleMainLooper(150, TimeUnit.MILLISECONDS)

        assertFalse("Dragging past touchSlop must cancel pending CheckForTap/isPressed", planetA.isPressed)
        assertEquals(1, clickedCount) // Drag must NOT re-trigger click
        assertTrue(ctrl.world.bodyOf("a")!!.x > 230f)

        repeat(15) {
            ctrl.stepSingleFrame(1f / 120f)
        }

        val dragEnd = SystemClock.uptimeMillis()
        val dragUp = MotionEvent.obtain(dragStart, dragEnd, MotionEvent.ACTION_UP, 90f, 30f, 0)
        planetA.dispatchTouchEvent(dragUp)
        assertFalse("Planet must not remain pressed after drag ACTION_UP", planetA.isPressed)
        dragDown.recycle()
        dragMove.recycle()
        dragUp.recycle()
        controller.destroy()
    }

    @Test
    fun matrixProjection_mapsTransformedAndNestedViewsToStaticRootCoordinates() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = FrameLayout(ctx)
        val planet = FrameLayout(ctx)
        val handle = View(ctx)
        planet.addView(handle)
        root.addView(planet)

        root.layout(0, 0, 1000, 1000)
        planet.layout(20, 30, 120, 90) // 100x60 at (20, 30)
        handle.layout(10, 5, 30, 25)

        planet.pivotX = 50f
        planet.pivotY = 30f
        planet.scaleX = 2.0f
        planet.scaleY = 2.0f
        planet.translationX = 200f
        planet.translationY = 100f

        val out = FloatArray(2)
        // Local (45, 25) inside handle -> (55, 30) inside planet -> scaled 2x around (50, 30) = (60, 30)
        // + translation (200, 100) + planet.left/top (20, 30) = (280, 160) in root!
        mapViewPointToRootStaticSpace(handle, root, 45f, 25f, out)
        assertEquals(280f, out[0], 1e-3f)
        assertEquals(160f, out[1], 1e-3f)
    }
}
