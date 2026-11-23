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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.DeponderRubberInsets
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import kotlinx.coroutines.flow.first
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
                RubberPhysics(naturalLengthDp = 100f),
                DeponderRubberInsets.contour(0f),
            )
            ctrl.applyCurrentPosesToViews()

            // Contour-cleared visible span: 300 - (20 * 1.2) - (20 * 1.2) = 252px!
            val pose = ctrl.world.rubberOf(EdgeKey("a", "b"))!!
            assertEquals(300f, pose.centerDistance, 1e-3f)
            assertEquals(252f, pose.resolvedLength(DeponderRubberInsets.contour(0f), ctrl.density), 1e-3f)

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
    fun customCompoundRubber_autoShrinksBadge_andSleepingLabelOrInsetsMutationRefreshesImmediately() {
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

            customCompound.deponderRubber("a", "b", RubberPhysics(), DeponderRubberInsets.contour(0f))
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
                .withInsets(DeponderRubberInsets.contour(0f))
            root.addView(defaultRubber, FrameLayout.LayoutParams(80, 40))
            defaultRubber.measure(
                View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(40, View.MeasureSpec.EXACTLY),
            )
            defaultRubber.layout(0, 0, 80, 40)
            defaultRubber.deponderRubber("b", "c")
            ctrl.applyCurrentPosesToViews()

            assertEquals(Color.TRANSPARENT, defaultRubber.labelBackgroundColor)
            assertEquals(20f, defaultRubber.width * defaultRubber.scaleX, 1e-3f)
            assertTrue(defaultRubber.isStartArrowRendered && defaultRubber.isEndArrowRendered)
            val initialScaleY = defaultRubber.scaleY

            // Mutate label to a very wide string without running another physics frame:
            defaultRubber.label = "VERY_WIDE_SLEEPING_LABEL_TEXT_12345"
            assertTrue(
                "Updating label while sleeping with fixed bounds must shrink effectiveScale (${defaultRubber.scaleY} < $initialScaleY)",
                defaultRubber.scaleY < initialScaleY,
            )

            // Switching to `fixed(0f, 15f)` suppresses start arrow on center-anchored end while rendering end arrow!
            defaultRubber.insets = DeponderRubberInsets.fixed(startDp = 0f, endDp = 15f)
            assertFalse(defaultRubber.isStartArrowRendered)
            assertTrue(defaultRubber.isEndArrowRendered)

            // Mutating `insets` to DeponderRubberInsets.None while sleeping spans full 60px center-to-center and omits both arrows!
            defaultRubber.insets = DeponderRubberInsets.None
            assertEquals(60f, defaultRubber.width * defaultRubber.scaleX, 1e-3f)
            assertFalse(defaultRubber.isStartArrowRendered)
            assertFalse(defaultRubber.isEndArrowRendered)

            // XML `app:deponderInsets="24dp"` resolves directly to FIXED(24f)!
            @Suppress("DEPRECATION")
            val xmlAttrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.deponderInsets, "24dp")
                .build()
            val xmlFixedRubber = DeponderRubberView(activity, xmlAttrs)
            assertEquals(DeponderRubberInsets.fixed(24f), xmlFixedRubber.insets)

            // Adaptive natural length: mutating short label -> long label expands `pose.spec.naturalLengthPx`
            // while preserving runtime `submitRubberElasticity`, and XML supports both `wrap_content` and `145dp`!
            ctrl.submitRubberElasticity(4.2f)
            assertEquals(4.2f, ctrl.world.rubberOf(EdgeKey("a", "b"))!!.spec.elasticity, 1e-4f)

            defaultRubber.label = "Hi"
            val shortNatPx = ctrl.world.rubberOf(EdgeKey("b", "c"))!!.spec.naturalLengthPx
            defaultRubber.label = "LONG_ADAPTIVE_ANDROID_VIEW_LABEL_TEXT_1234567890"
            val longNatPx = ctrl.world.rubberOf(EdgeKey("b", "c"))!!.spec.naturalLengthPx
            assertTrue("Long label ($longNatPx) must exceed short label ($shortNatPx)", longNatPx > shortNatPx + 80f)
            assertEquals(4.2f, ctrl.world.rubberOf(EdgeKey("b", "c"))!!.spec.elasticity, 1e-4f)

            val pD = View(activity).also {
                root.addView(it, FrameLayout.LayoutParams(40, 40))
                it.layout(320, 280, 360, 320)
                it.deponderPlanet("d", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
            }
            assertTrue(pD.width == 40)
            @Suppress("DEPRECATION")
            val xmlFixedLenAttrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.deponderStartId, "c")
                .addAttribute(R.attr.deponderEndId, "d")
                .addAttribute(R.attr.deponderNaturalLength, "145dp")
                .addAttribute(R.attr.deponderLabel, "VERY_LONG_LABEL_LOCKED_BY_FIXED_XML_LENGTH")
                .build()
            val xmlFixedLenRubber = DeponderRubberView(activity, xmlFixedLenAttrs)
            root.addView(xmlFixedLenRubber, FrameLayout.LayoutParams(120, 40))
            assertEquals(145f * ctrl.density, ctrl.world.rubberOf(EdgeKey("c", "d"))!!.spec.naturalLengthPx, 1e-3f)

            // XML `deponderNaturalLength` + `deponderElasticity` WITHOUT XML start/end IDs must be preserved
            // when `deponderRubber(startId, endId)` is subsequently invoked without explicit `physics`!
            @Suppress("DEPRECATION")
            val xmlTemplateAttrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.deponderNaturalLength, "155dp")
                .addAttribute(R.attr.deponderElasticity, "2.75")
                .addAttribute(R.attr.deponderLabel, "VERY_LONG_TEMPLATE_LABEL_THAT_MUST_STAY_FIXED")
                .build()
            val xmlTemplateRubber = DeponderRubberView(activity, xmlTemplateAttrs)
            root.addView(xmlTemplateRubber, FrameLayout.LayoutParams(120, 40))
            xmlTemplateRubber.deponderRubber("a", "d")
            val templatePose = ctrl.world.rubberOf(EdgeKey("a", "d"))!!
            assertEquals(155f * ctrl.density, templatePose.spec.naturalLengthPx, 1e-3f)
            assertEquals(2.75f, templatePose.spec.elasticity, 1e-4f)

            // Mutating `.physics` at runtime updates the active RubberSpec immediately:
            xmlTemplateRubber.physics = RubberPhysics.fixed(175f, elasticity = 3.25f)
            assertEquals(175f * ctrl.density, templatePose.spec.naturalLengthPx, 1e-3f)
            assertEquals(3.25f, templatePose.spec.elasticity, 1e-4f)
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
        val dragStart = SystemClock.uptimeMillis() + 20
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

    @Test
    fun clearPlanetAndRubber_cleanSubtreeProbesWithoutBreakingSubsequentLayout_andPreserveBusinessTouchAndTags() {
        mountAttachedRoot { activity, root ->
            val ctrl = root.deponderRoot()

            val customTouchPlanet = View(activity)
            var customTouchCalls = 0
            customTouchPlanet.setOnTouchListener { _, _ ->
                customTouchCalls += 1
                false
            }
            // Disabling drag upfront must NOT overwrite an existing custom OnTouchListener.
            customTouchPlanet.deponderDrag(enabled = false)
            root.addView(customTouchPlanet, FrameLayout.LayoutParams(40, 40))
            customTouchPlanet.layout(60, 280, 100, 320)
            customTouchPlanet.deponderPlanet("p1", PlanetPhysics(pinned = true))

            val downProbe = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
            customTouchPlanet.dispatchTouchEvent(downProbe)
            downProbe.recycle()
            assertEquals(1, customTouchCalls)

            // Planet with nested drag handle -> clearDeponderPlanet() keeps view in root without orphan drag crash!
            val handle = View(activity)
            val cardPlanet = FrameLayout(activity).apply {
                addView(handle, FrameLayout.LayoutParams(20, 20))
            }
            root.addView(cardPlanet, FrameLayout.LayoutParams(48, 48))
            cardPlanet.layout(200, 280, 248, 328)
            cardPlanet.deponderPlanet("p2", PlanetPhysics(pinned = true))
            handle.deponderDrag(enabled = true)

            // Custom compound rubber with both XML tag and programmatic stretch -> clearDeponderRubber() keeps view in root!
            val customRubber = FrameLayout(activity)
            val xmlStem = View(activity).apply { tag = activity.getString(R.string.deponder_stretch) }
            val progStem = View(activity).apply { deponderStretch(true) }
            customRubber.addView(xmlStem, FrameLayout.LayoutParams(20, 2))
            customRubber.addView(progStem, FrameLayout.LayoutParams(20, 2))
            root.addView(customRubber, FrameLayout.LayoutParams(80, 20))
            customRubber.layout(0, 0, 80, 20)
            xmlStem.layout(0, 8, 20, 10)
            progStem.layout(0, 12, 20, 14)
            customRubber.deponderRubber("p1", "p2")
            ctrl.applyCurrentPosesToViews()

            cardPlanet.clearDeponderPlanet()
            customRubber.clearDeponderRubber()

            // Re-measuring & laying out root while cleared views remain attached must NOT throw,
            // and both rubber stems must reset cleanly to 1.0x!
            root.measure(
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, 800, 600)
            assertEquals(1f, xmlStem.scaleX, 1e-4f)
            assertEquals(1f, progStem.scaleX, 1e-4f)

            // XML @string/deponder_stretch tag remains preserved and re-activates cleanly on re-bind,
            // while programmatic stretch was cleared (counter-scaled as non-stretched content so screen width stays 1.0x).
            assertEquals(activity.getString(R.string.deponder_stretch), xmlStem.tag)
            cardPlanet.deponderPlanet("p2", PlanetPhysics(pinned = true))
            customRubber.deponderRubber("p1", "p2", RubberPhysics.fixed(100f), DeponderRubberInsets.None)
            ctrl.applyCurrentPosesToViews()
            assertTrue(xmlStem.scaleX > 1f)
            assertEquals(1f, progStem.scaleX * customRubber.scaleX, 1e-4f)

            // Non-finite guards on DeponderController:
            try {
                ctrl.submitScale(Float.NaN)
                fail("Expected IllegalArgumentException for NaN scale")
            } catch (_: IllegalArgumentException) {
            }
            try {
                ctrl.submitRubberElasticity(Float.POSITIVE_INFINITY)
                fail("Expected IllegalArgumentException for non-finite elasticity")
            } catch (_: IllegalArgumentException) {
            }
            try {
                ctrl.dragBy("p1", Float.NaN, 0f)
                fail("Expected IllegalArgumentException for NaN dragBy")
            } catch (_: IllegalArgumentException) {
            }
            try {
                ctrl.moveTo("p1", 0f, Float.NEGATIVE_INFINITY)
                fail("Expected IllegalArgumentException for non-finite moveTo")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun controlPlaneCombine_pausesOnLifecycleStop_resumesUnsettledOnStart_andStaysAsleepOnceSettled() = runTest {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        val root = FrameLayout(activity)
        activity.setContentView(root, FrameLayout.LayoutParams(600, 600))
        ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS)
        root.layout(0, 0, 600, 600)

        lateinit var registry: LifecycleRegistry
        val owner = object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = registry
        }
        registry = LifecycleRegistry.createUnsafe(owner)
        registry.currentState = Lifecycle.State.STARTED

        val ctrl = root.deponderRoot(
            lifecycleOwner = owner,
            physics = RootPhysics(airDamping = 8f, wallRepulsionDp = 0f),
        )
        assertFalse(ctrl.isAwake)
        assertFalse(ctrl.loopGateFlow.first())

        val pA = View(activity).also {
            root.addView(it, FrameLayout.LayoutParams(40, 40))
            it.layout(100, 280, 140, 320)
            it.deponderPlanet("a", PlanetPhysics(pinned = true, repulsionRadiusDp = 0f))
        }
        val pB = View(activity).also {
            root.addView(it, FrameLayout.LayoutParams(40, 40))
            it.layout(220, 280, 260, 320)
            it.deponderPlanet("b", PlanetPhysics(pinned = false, repulsionRadiusDp = 0f))
        }
        assertTrue(pA.width == 40 && pB.width == 40)
        val rubber = DeponderRubberView(activity).also {
            root.addView(it, FrameLayout.LayoutParams(80, 20))
            it.layout(0, 0, 80, 20)
            it.deponderRubber("a", "b", RubberPhysics.fixed(100f, elasticity = 12f))
        }
        assertTrue(rubber.isAttachedToWindow)

        assertTrue(ctrl.isAwake)
        assertTrue(ctrl.loopGateFlow.first())

        // ON_STOP pauses the frame loop while preserving unsettled kinetic energy.
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFalse(ctrl.isAwake)
        assertFalse(ctrl.loopGateFlow.first())

        // ON_START automatically resumes because mechanical energy was never wiped!
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertTrue(ctrl.isAwake)
        assertTrue(ctrl.loopGateFlow.first())

        // Step deterministically until kinetic-force sleep is reached.
        var active = true
        var guard = 0
        while (active && guard < 800) {
            active = ctrl.stepSingleFrame(0.016f)
            guard += 1
        }
        assertFalse("Critically damped system must settle within 800 steps (guard=$guard)", active)
        assertFalse(ctrl.isAwake)
        assertFalse(ctrl.loopGateFlow.first())

        // Once settled, cycling ON_STOP -> ON_START stays asleep at 0% CPU.
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFalse(ctrl.isAwake)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertFalse(ctrl.isAwake)
        assertFalse(ctrl.loopGateFlow.first())

        // Idempotent unchanged elasticity & planet contour size updates keep settled loop asleep!
        ctrl.submitRubberElasticity(12f)
        assertFalse(ctrl.isAwake)
        ctrl.updatePlanetMeasuredSize("a", 64, 64)
        assertFalse(ctrl.isAwake)

        // Mid-drag ON_STOP releases pressed state so ON_START settles cleanly instead of spinning forever!
        pB.layout(100, 280, 140, 320)
        ctrl.setPressed("b", true)
        assertTrue(ctrl.world.bodyOf("b")!!.isPressed)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFalse(ctrl.world.bodyOf("b")!!.isPressed)
        assertFalse(ctrl.isAwake)

        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertTrue(ctrl.isAwake)
        var resumedActive = true
        var resumeGuard = 0
        while (resumedActive && resumeGuard < 800) {
            resumedActive = ctrl.stepSingleFrame(0.016f)
            resumeGuard += 1
        }
        assertFalse("Released body after ON_STOP/ON_START must settle to sleep", resumedActive)
        assertFalse(ctrl.isAwake)

        // Style finite guards on DeponderRubberView:
        try {
            rubber.lineThicknessDp = Float.NaN
            fail("Expected IllegalArgumentException for NaN lineThicknessDp")
        } catch (_: IllegalArgumentException) {
        }
        try {
            rubber.arrowSizeDp = -1f
            fail("Expected IllegalArgumentException for negative arrowSizeDp")
        } catch (_: IllegalArgumentException) {
        }
        try {
            rubber.labelTextSizeSp = 0f
            fail("Expected IllegalArgumentException for non-positive labelTextSizeSp")
        } catch (_: IllegalArgumentException) {
        }

        ctrl.cancel()
        activityController.destroy()
    }

    @Test
    fun controllerUnfold_worksPreLayoutAndAtRuntime_whileSubmitScaleAndReheatPreserveManualLayout() = runTest {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        val root = FrameLayout(activity)
        activity.setContentView(root, FrameLayout.LayoutParams(600, 600))

        // Bind root + views + call unfold() BEFORE the first Android layout pass (onCreate style).
        val ctrl = root.deponderRoot()
        val views = (0 until 4).map { idx ->
            View(activity).also { v ->
                root.addView(v, FrameLayout.LayoutParams(40, 40))
                v.deponderPlanet("n$idx", PlanetPhysics())
            }
        }
        val links = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0).map { (u, v) ->
            DeponderRubberView(activity).also { rv ->
                root.addView(rv, FrameLayout.LayoutParams(80, 20))
                rv.deponderRubber("n$u", "n$v", RubberPhysics.fixed(120f))
            }
        }
        assertTrue(views.size == 4 && links.size == 4)
        ctrl.unfold()

        // Now deliver initial layout bounds with a crossed bowtie placement.
        ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS)
        root.layout(0, 0, 600, 600)
        val initialCenters = listOf(200 to 200, 400 to 400, 200 to 400, 400 to 200)
        views.forEachIndexed { idx, v ->
            val (cx, cy) = initialCenters[idx]
            v.layout(cx - 20, cy - 20, cx + 20, cy + 20)
        }
        links.forEach { it.layout(0, 0, 80, 20) }

        var active = true
        var guard = 0
        while (active && guard < 1800) {
            active = ctrl.stepSingleFrame(0.016f)
            guard += 1
        }
        assertFalse(active)

        val b0 = ctrl.world.bodyOf("n0")!!
        val b1 = ctrl.world.bodyOf("n1")!!
        val b2 = ctrl.world.bodyOf("n2")!!
        val b3 = ctrl.world.bodyOf("n3")!!
        fun orient(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float =
            (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        fun hasCrossing(): Boolean {
            val c0123 =
                (orient(b0.x, b0.y, b1.x, b1.y, b2.x, b2.y) * orient(b0.x, b0.y, b1.x, b1.y, b3.x, b3.y) < 0f) &&
                    (orient(b2.x, b2.y, b3.x, b3.y, b0.x, b0.y) * orient(b2.x, b2.y, b3.x, b3.y, b1.x, b1.y) < 0f)
            val c1230 =
                (orient(b1.x, b1.y, b2.x, b2.y, b3.x, b3.y) * orient(b1.x, b1.y, b2.x, b2.y, b0.x, b0.y) < 0f) &&
                    (orient(b3.x, b3.y, b0.x, b0.y, b1.x, b1.y) * orient(b3.x, b3.y, b0.x, b0.y, b2.x, b2.y) < 0f)
            return c0123 || c1230
        }
        assertFalse("Pre-layout controller.unfold() must untangle crossed cycle", hasCrossing())

        // Manually arrange into a crossed bowtie and verify submitScale() & deprecated reheat() preserve manual arrangement.
        ctrl.moveTo("n0", 200f, 200f)
        ctrl.moveTo("n1", 400f, 400f)
        ctrl.moveTo("n2", 200f, 400f)
        ctrl.moveTo("n3", 400f, 200f)
        ctrl.submitScale(1.25f)
        var scaleActive = true
        var scaleGuard = 0
        while (scaleActive && scaleGuard < 1800) {
            scaleActive = ctrl.stepSingleFrame(0.016f)
            scaleGuard += 1
        }
        assertFalse(scaleActive)
        assertTrue("submitScale() must preserve manual layout without triggering unfold", hasCrossing())

        @Suppress("DEPRECATION")
        ctrl.reheat()
        assertFalse(ctrl.isAwake)
        assertTrue("deprecated reheat() no-op must preserve manual layout", hasCrossing())

        ctrl.unfold()
        var unfoldActive = true
        var unfoldGuard = 0
        while (unfoldActive && unfoldGuard < 1800) {
            unfoldActive = ctrl.stepSingleFrame(0.016f)
            unfoldGuard += 1
        }
        assertFalse(unfoldActive)
        assertFalse("controller.unfold() must untangle crossed cycle", hasCrossing())

        ctrl.cancel()
        activityController.destroy()
    }

    @Test
    @Suppress("DEPRECATION")
    fun controllerDeprecatedWithUnfoldFlag_preservesTransitionalBehavior() = runTest {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        val root = FrameLayout(activity)
        activity.setContentView(root, FrameLayout.LayoutParams(600, 600))
        ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS)
        root.layout(0, 0, 600, 600)

        val ctrl = root.deponderRoot(physics = RootPhysics().withUnfold(true))
        val initialCenters = listOf(200 to 200, 400 to 400, 200 to 400, 400 to 200)
        (0 until 4).forEach { idx ->
            View(activity).also { v ->
                val (cx, cy) = initialCenters[idx]
                root.addView(v, FrameLayout.LayoutParams(40, 40))
                v.layout(cx - 20, cy - 20, cx + 20, cy + 20)
                v.deponderPlanet("n$idx", PlanetPhysics())
            }
        }
        listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0).forEach { (u, v) ->
            DeponderRubberView(activity).also { rv ->
                root.addView(rv, FrameLayout.LayoutParams(80, 20))
                rv.layout(0, 0, 80, 20)
                rv.deponderRubber("n$u", "n$v", RubberPhysics.fixed(120f))
            }
        }

        var active = true
        var guard = 0
        while (active && guard < 1800) {
            active = ctrl.stepSingleFrame(0.016f)
            guard += 1
        }
        assertFalse(active)

        val b0 = ctrl.world.bodyOf("n0")!!
        val b1 = ctrl.world.bodyOf("n1")!!
        val b2 = ctrl.world.bodyOf("n2")!!
        val b3 = ctrl.world.bodyOf("n3")!!
        fun orient(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float =
            (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        val c0123 =
            (orient(b0.x, b0.y, b1.x, b1.y, b2.x, b2.y) * orient(b0.x, b0.y, b1.x, b1.y, b3.x, b3.y) < 0f) &&
                (orient(b2.x, b2.y, b3.x, b3.y, b0.x, b0.y) * orient(b2.x, b2.y, b3.x, b3.y, b1.x, b1.y) < 0f)
        val c1230 =
            (orient(b1.x, b1.y, b2.x, b2.y, b3.x, b3.y) * orient(b1.x, b1.y, b2.x, b2.y, b0.x, b0.y) < 0f) &&
                (orient(b3.x, b3.y, b0.x, b0.y, b1.x, b1.y) * orient(b3.x, b3.y, b0.x, b0.y, b2.x, b2.y) < 0f)
        assertFalse("Legacy RootPhysics().withUnfold(true) must remain functional", c0123 || c1230)

        ctrl.cancel()
        activityController.destroy()
    }
}
