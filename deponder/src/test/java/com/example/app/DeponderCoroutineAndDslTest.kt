package com.example.app

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Pair
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.copy
import com.cinpe.deponder.deponder
import com.cinpe.deponder.simplePlanet
import com.cinpe.deponder.simpleRoot
import com.cinpe.deponder.simpleRubber
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class DeponderCoroutineAndDslTest {

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private data class Node(val id: String, val label: String)
    private data class Edge(val from: String, val to: String)

    private lateinit var context: Context
    private lateinit var root: FrameLayout
    private lateinit var owner: TestOwner

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        ShadowChoreographer.setPostFrameCallbackDelay(16)
        context = ApplicationProvider.getApplicationContext()
        root = FrameLayout(context).apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 800, 800)
        }
        owner = TestOwner().apply {
            registry.currentState = Lifecycle.State.RESUMED
        }
    }

    @After
    fun tearDown() {
        ShadowChoreographer.reset()
    }

    @Test
    fun kotlinDsl_immediateSubmit_customDiff_andLifecycleStopStartDestroy() {
        var rebindCount = 0
        // Immediate submit right after deponder(...) without any intervening idle!
        val control = deponder<Node, Edge>(owner, root) {
            planetId { it.id }
            rubberPairId { Pair(it.from, it.to) }
            createPlanet { parent, item ->
                val tv = TextView(parent.context).apply { text = item.label }
                simplePlanet {
                    itemView = tv
                    id = item.id
                }
            }
            bindPlanet { holder, item ->
                rebindCount++
                (holder.itemView() as TextView).text = item.label
                holder.copy { id = item.id }
            }
            createRubber { parent, edge ->
                simpleRubber {
                    itemView = View(parent.context)
                    sId = edge.from
                    eId = edge.to
                }
            }
            areContentsTheSameByP { a, b -> a.label == b.label }
        }

        control.submitPlanet(listOf(Node("1", "One"), Node("2", "Two")))
        control.submitRubber(listOf(Edge("1", "2")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, root.childCount)

        // Update label of Node("1") -> triggers custom areContentsTheSameByP -> bindPlanet in place.
        control.submitPlanet(listOf(Node("1", "One-Updated"), Node("2", "Two")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, rebindCount)
        assertEquals(3, root.childCount)

        // Lifecycle stop -> start replay must be idempotent (still 3 children, no extra rebind).
        owner.registry.currentState = Lifecycle.State.CREATED
        shadowOf(Looper.getMainLooper()).idle()
        owner.registry.currentState = Lifecycle.State.RESUMED
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, root.childCount)
        assertEquals(1, rebindCount)

        // Lifecycle ON_DESTROY automatically cancels pipelines without leaking.
        owner.registry.currentState = Lifecycle.State.DESTROYED
        shadowOf(Looper.getMainLooper()).idle()
        control.submitPlanet(listOf(Node("1", "AfterDestroy"), Node("2", "Two"), Node("3", "Three")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Submissions after ON_DESTROY are ignored", 3, root.childCount)
    }

    @Test
    fun debounce_minIntervalCoalescesRapidSubmissions() {
        var createCalls = 0
        val rootOpt = simpleRoot {
            itemView = root
            minInterval = 50L
        }
        val control = deponder<String, Pair<String, String>>(owner, rootOpt) {
            planetId { it }
            rubberPairId { it }
            createPlanet { parent, id ->
                createCalls++
                simplePlanet {
                    itemView = TextView(parent.context)
                    this.id = id
                }
            }
            createRubber { parent, r ->
                simpleRubber {
                    itemView = View(parent.context)
                    sId = r.first
                    eId = r.second
                }
            }
        }

        control.submitPlanet(listOf("a", "b", "c"))
        control.submitPlanet(listOf("a"))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Still within debounce window", 0, root.childCount)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80))
        assertEquals(1, root.childCount)
        assertEquals("Intermediate 3-item burst was coalesced away", 1, createCalls)

        // Also verify Choreographer frame ticking drives physics integration on DSL controller.
        val planetView = root.getChildAt(0)
        val beforeTx = planetView.translationX
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertNotEquals(beforeTx, planetView.translationX)
        control.cancel()
        val stoppedTx = planetView.translationX
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertEquals(stoppedTx, planetView.translationX, 0f)
    }

    @Test
    fun touchHelper_onScrollWithNullDownEvent_doesNotThrowAndTranslatesPlanet() {
        val tv = TextView(context)
        val planet = simplePlanet { itemView = tv }
        val helper = DeponderHelper.TouchHelper(planet)
        val now = SystemClock.uptimeMillis()
        val move = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 50f, 60f, 0)
        try {
            helper.onScroll(null, move, -15f, 25f)
            assertEquals(15f, tv.translationX, 0f)
            assertEquals(-25f, tv.translationY, 0f)
        } finally {
            move.recycle()
        }
    }

    @Test
    fun rubberReconciler_autoAlignsPooledEndpoints_ignoresSelfLoops_andScalesTaggedChildrenUniformly() {
        data class Link(val id: String, val from: String, val to: String, val label: String)

        val control = deponder<String, Link>(owner, root) {
            planetId { it }
            rubberPairId { Pair(it.from, it.to) }
            createPlanet { parent, id ->
                simplePlanet {
                    itemView = TextView(parent.context)
                    this.id = id
                }
            }
            createRubber { parent, link ->
                val container = FrameLayout(parent.context).apply {
                    val labelView = TextView(parent.context).apply {
                        tag = DeponderHelper.TAG_OF_UN_RUBBER
                        text = link.label
                    }
                    addView(labelView)
                }
                simpleRubber {
                    itemView = container
                    sId = link.from
                    eId = link.to
                }
            }
            // Intentionally return `holder` WITHOUT updating `sId`/`eId` to verify automatic endpoint alignment!
            bindRubber { holder, link ->
                val group = holder.itemView() as FrameLayout
                (group.getChildAt(0) as TextView).text = link.label
                holder
            }
            areContentsTheSameByR { a, b -> a.label == b.label }
        }

        control.submitPlanet(listOf("a", "b", "c"))
        // Include a self-loop Link("loop", "a", "a", "Loop") — must be safely ignored rather than crashing.
        control.submitRubber(listOf(Link("e1", "a", "b", "AB"), Link("loop", "a", "a", "Loop")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(4, root.childCount) // 3 planets + 1 rubber

        // Remove e1 (a-b) so its SimpleRubber goes into the reuse pool, then add e2 (b-c) which reuses it via bindRubber.
        control.submitRubber(emptyList())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, root.childCount)

        control.submitRubber(listOf(Link("e2", "b", "c", "BC")))
        control.submitScale(1.3f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertEquals(4, root.childCount)

        val rubberGroup = (0 until root.childCount)
            .map { root.getChildAt(it) }
            .filterIsInstance<FrameLayout>()
            .single()
        val taggedChild = rubberGroup.getChildAt(0) as TextView
        assertEquals("BC", taggedChild.text.toString())
        assertEquals(1.3f, rubberGroup.scaleY, 0f)
        assertEquals(1f, taggedChild.scaleY, 0f)

        // Removing e2 (b-c) must cleanly detach the reused rubber view without leaking!
        control.submitRubber(emptyList())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, root.childCount)
    }
}
