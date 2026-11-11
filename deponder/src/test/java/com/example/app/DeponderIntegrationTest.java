package com.example.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import com.cinpe.deponder.Deponder;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRoot;
import com.cinpe.deponder.model.SimpleRubber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowChoreographer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * End-to-end behaviour of {@link Deponder} driven via Choreographer and RenderNode properties.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LooperMode(LooperMode.Mode.PAUSED)
public class DeponderIntegrationTest {

    static final class TestOwner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    final class TestDeponder extends Deponder<String, Pair<String, String>> {
        final Map<String, View> createdPlanets = new HashMap<>();
        final List<Pair<String, String>> createdRubbers = new ArrayList<>();

        TestDeponder(LifecycleOwner owner, SimpleRoot root) {
            super(owner, root);
        }

        @Override
        public String planetId(String s) {
            return s;
        }

        @Override
        public Pair<String, String> rubberPairId(Pair<String, String> r) {
            return r;
        }

        @NonNull
        @Override
        public SimplePlanet createPlanet(@NonNull ViewGroup parent, String s) {
            TextView v = new TextView(parent.getContext());
            v.setText(s);
            createdPlanets.put(s, v);
            return SimplePlanet.builder().itemView(v).id(s).build();
        }

        @NonNull
        @Override
        public SimpleRubber createRubber(@NonNull ViewGroup parent, Pair<String, String> r) {
            createdRubbers.add(r);
            return SimpleRubber.builder().itemView(new View(parent.getContext())).sId(r.first).eId(r.second).build();
        }
    }

    private Context context;
    private FrameLayout root;
    private TestOwner owner;
    private TestDeponder deponder;

    @Before
    public void setUp() {
        ShadowChoreographer.setPostFrameCallbackDelay(16);
        context = ApplicationProvider.getApplicationContext();
        root = new FrameLayout(context);
        root.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1000, 1000);
        owner = new TestOwner();
        owner.registry.setCurrentState(Lifecycle.State.RESUMED);
        deponder = new TestDeponder(owner, SimpleRoot.builder().itemView(root).build());
        shadowOf(Looper.getMainLooper()).idle();
    }

    @After
    public void tearDown() {
        ShadowChoreographer.reset();
    }

    // ------------------------------------------------------------------ helpers

    /** Pumps the main looper until the condition holds. */
    private void await(String what, Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            if (condition.call()) return;
            Thread.sleep(5);
        }
        fail("Timed out waiting for: " + what + " (children=" + root.getChildCount() + ")");
    }

    private int planetCount() {
        int n = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (root.getChildAt(i) instanceof TextView) n++;
        }
        return n;
    }

    private int rubberCount() {
        return root.getChildCount() - planetCount();
    }

    private void frame() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void planetsAndRubbers_areReconciled_withPoolReuse() throws Exception {
        deponder.submitPlanet(Arrays.asList("a", "b", "c"));
        deponder.submitRubber(Arrays.asList(Pair.create("a", "b"), Pair.create("b", "c"), Pair.create("a", "zz")));

        await("3 planets + 2 rubbers (dangling a-zz filtered out)", () -> planetCount() == 3 && rubberCount() == 2);
        assertEquals(3, deponder.createdPlanets.size());
        View viewOfC = deponder.createdPlanets.get("c");

        // Remove c -> its rubber b-c disappears as well.
        deponder.submitPlanet(Arrays.asList("a", "b"));
        await("c and b-c removed", () -> planetCount() == 2 && rubberCount() == 1);
        assertEquals(null, viewOfC.getParent());

        // Add d -> c's view is reused from the pool instead of creating a new one.
        deponder.submitPlanet(Arrays.asList("a", "b", "d"));
        await("d added", () -> planetCount() == 3);
        assertEquals("no new planet view created", 3, deponder.createdPlanets.size());
        assertSame(root, viewOfC.getParent());

        // Bring c back -> rubber b-c comes back too (rubber view reused from rubber pool).
        deponder.submitPlanet(Arrays.asList("a", "b", "c", "d"));
        await("c and b-c back", () -> planetCount() == 4 && rubberCount() == 2);
    }

    @Test
    public void resubmittingMutatedSameListInstance_isApplied() throws Exception {
        List<String> list = new ArrayList<>(Collections.singletonList("a"));
        deponder.submitRubber(Collections.singletonList(Pair.create("a", "b")));
        deponder.submitPlanet(list);
        await("a", () -> planetCount() == 1);
        list.add("b");
        deponder.submitPlanet(list);
        await("a, b and rubber a-b", () -> planetCount() == 2 && rubberCount() == 1);
    }

    @Test
    public void frames_moveThePlanets() throws Exception {
        deponder.submitPlanet(Arrays.asList("a", "b", "c"));
        deponder.submitRubber(Arrays.asList(Pair.create("a", "b"), Pair.create("b", "c")));
        await("graph", () -> planetCount() == 3 && rubberCount() == 2);

        final View a = deponder.createdPlanets.get("a");
        final float beforeTx = a.getTranslationX();
        final float beforeTy = a.getTranslationY();
        await("planet a moved by the Choreographer physics loop", () -> {
            frame();
            return Math.abs(a.getTranslationX() - beforeTx) > 1f
                    || Math.abs(a.getTranslationY() - beforeTy) > 1f;
        });
    }

    @Test
    public void submitScale_outsideBounds_isIgnored_andCancelStopsPhysics() throws Exception {
        deponder.submitPlanet(Collections.singletonList("a"));
        await("planet a added", () -> planetCount() == 1);
        final View a = deponder.createdPlanets.get("a");

        deponder.submitScale(100f);
        deponder.submitScale(0.0001f);
        deponder.submitScale(1.2f);
        frame();
        assertEquals(1.2f, a.getScaleX(), 0f);
        assertEquals(1.2f, a.getScaleY(), 0f);

        deponder.cancel();
        float txAfterCancel = a.getTranslationX();
        float tyAfterCancel = a.getTranslationY();
        frame();
        assertEquals(txAfterCancel, a.getTranslationX(), 0f);
        assertEquals(tyAfterCancel, a.getTranslationY(), 0f);
    }
}
