package com.example.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import com.cinpe.deponder.BindAdapter;
import com.cinpe.deponder.Deponder;
import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.itf.FrontalAreaProperty;
import com.cinpe.deponder.itf.QualityProperty;
import com.cinpe.deponder.model.SimpleOption;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRoot;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;
import com.cinpe.deponder.option.SimpleRootOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowChoreographer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Java source-compatibility guard.
 * <p>
 * This test lives OUTSIDE the SDK package on purpose: it sees the SDK exactly like the (Java) demo app does.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class JavaApiCompatTest {

    private Context context;
    private FrameLayout root;
    private TestOwner owner;

    /** A plain Java LifecycleOwner, like an Activity/Fragment would be. */
    static final class TestOwner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    /** A Java implementation of a public SDK interface: method names must stay {@code itemView()}, {@code id()}. */
    static final class JavaOption implements BaseOption {
        private final View view;

        JavaOption(View view) {
            this.view = view;
        }

        @NonNull
        @Override
        public View itemView() {
            return view;
        }

        @NonNull
        @Override
        public String id() {
            return "java";
        }
    }

    @Before
    public void setUp() {
        ShadowChoreographer.setPostFrameCallbackDelay(16);
        context = ApplicationProvider.getApplicationContext();
        root = new FrameLayout(context);
        owner = new TestOwner();
        owner.registry.setCurrentState(Lifecycle.State.RESUMED);
    }

    @After
    public void tearDown() {
        ShadowChoreographer.reset();
    }

    @Test
    public void constants_areAccessibleAsStaticFields() {
        float f = DeponderHelper.DEFAULT_INIT_SCALE + DeponderHelper.DEFAULT_MAX_SCALE + DeponderHelper.DEFAULT_MIN_SCALE
                + DeponderHelper.DEFAULT_ROOT_DENSITY + DeponderHelper.DEFAULT_QUALITY_PROPERTY
                + DeponderHelper.DEFAULT_Internal_Pressure_START + DeponderHelper.DEFAULT_Internal_Pressure_TOP
                + DeponderHelper.DEFAULT_Internal_Pressure_END + DeponderHelper.DEFAULT_Internal_Pressure_BOTTOM
                + DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT + DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT
                + DeponderHelper.MIN_ACCELERATION + DeponderHelper.INCREMENTAL_SCALE;
        int i = DeponderHelper.DEFAULT_Internal_Pressure + DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE
                + DeponderHelper.DEFAULT_RUBBER_NATURAL_LENGTH;
        String s = DeponderHelper.TAG_OF_UN_RUBBER + DeponderHelper.TAG;
        assertTrue(f > 0);
        assertEquals(820, i);
        assertEquals("UN_RUBBER_RUBBERDeponderHelper", s);
    }

    @Test
    public void concatId_staticOverloads() {
        assertEquals("Ab", DeponderHelper.concatId("b", "A"));
        assertEquals("ab", DeponderHelper.concatId(Pair.create("a", "b")));
    }

    @Test
    public void simplePlanet_builderCreateAndAccessors() {
        TextView v = new TextView(context);
        SimplePlanet.Builder builder = SimplePlanet.builder();
        builder = builder.itemView(v).id("p1").quality(3f).frontalArea(2f).mInternalPressure(100f).elasticityCoefficient(1.5f);
        SimplePlanet p = builder.build();
        PlanetOption asOption = p;
        QualityProperty quality = p;
        FrontalAreaProperty frontal = p;

        assertSame(v, p.itemView());
        assertEquals("p1", p.id());
        assertEquals(3f, quality.quality(), 0f);
        assertEquals(2f, frontal.frontalArea(), 0f);
        assertEquals(100f, asOption.mInternalPressure(), 0f);
        assertEquals(1.5f, asOption.elasticityCoefficient(), 0f);

        SimplePlanet copy = p.toBuilder().quality(4f).build();
        assertEquals(4f, copy.quality(), 0f);
        assertEquals("p1", copy.id());

        SimplePlanet created = SimplePlanet.create(v, "p2", 1f, 1f, 50f, 1f);
        assertEquals("p2", created.id());
    }

    @Test
    public void simpleRubber_builderCreateAndAccessors() {
        View line = new View(context);
        SimpleRubber.Builder builder = SimpleRubber.builder();
        SimpleRubber r = builder.itemView(line).sId("a").eId("b").elasticityCoefficient(2f).naturalLength(200).build();
        RubberOption asOption = r;

        assertSame(line, r.itemView());
        assertEquals("a", asOption.sId());
        assertEquals("b", asOption.eId());
        assertEquals("ab", r.id());
        assertEquals(2f, r.elasticityCoefficient(), 0f);
        assertEquals(200, r.naturalLength());
        List<BaseOption> vArr = r.vArr();
        assertTrue(vArr.isEmpty());

        SimpleRubber copy = r.toBuilder().naturalLength(10).build();
        assertEquals(10, copy.naturalLength());

        SimpleRubber created = SimpleRubber.create(line, "x", "y", 1f, 30);
        assertEquals("xy", created.id());
    }

    @Test
    public void simpleRoot_builderCreateAndAccessors() {
        SimpleRoot.Builder builder = SimpleRoot.builder();
        SimpleRoot r = builder.itemView(root).initScale(1f).maxScale(2f).minScale(0.2f).mRootDensity(0.001f)
                .mInternalPressure(200f).elasticityCoefficientStart(1f).elasticityCoefficientTop(1.1f)
                .elasticityCoefficientEnd(1.2f).elasticityCoefficientBot(1.3f).minInterval(16L).build();
        SimpleRootOption asOption = r;
        ViewGroup group = asOption.itemView();

        assertSame(root, group);
        assertEquals(1f, r.initScale(), 0f);
        assertEquals(2f, r.maxScale(), 0f);
        assertEquals(0.2f, r.minScale(), 0f);
        assertEquals(0.001f, r.mRootDensity(), 0f);
        assertEquals(200f, r.mInternalPressure(), 0f);
        assertEquals(1f, r.elasticityCoefficientStart(), 0f);
        assertEquals(1.1f, r.elasticityCoefficientTop(), 0f);
        assertEquals(1.2f, r.elasticityCoefficientEnd(), 0f);
        assertEquals(1.3f, r.elasticityCoefficientBot(), 0f);
        assertEquals(16L, r.minInterval());
        assertNotNull(r.id());

        SimpleRoot created = SimpleRoot.create(root, 1f, 1.5f, 0.5f, 0.0006f, 300f, 1.44f, 1.44f, 1.44f, 1.44f, 0L);
        assertEquals(1.5f, created.maxScale(), 0f);
    }

    @Test
    public void simpleOption_builderAndCreate() {
        View v = new View(context);
        SimpleOption o = SimpleOption.builder().itemView(v).build();
        SimpleOption c = SimpleOption.create(v);
        assertSame(v, o.itemView());
        assertEquals(String.valueOf(v.hashCode()), c.id());
    }

    @Test
    public void javaImplementationOfInterface_isUsable() {
        JavaOption option = new JavaOption(new View(context));
        BaseOption base = option;
        assertEquals("java", base.id());
        assertNotNull(base.itemView());
    }

    @Test
    public void helpers_touchDragAndClick() {
        TextView v = new TextView(context);
        root.addView(v);
        SimplePlanet p = SimplePlanet.builder().itemView(v).build();
        DeponderHelper.bindDefTouchPlanet(p);
        DeponderHelper.TouchHelper helper = new DeponderHelper.TouchHelper(p);
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0);
        assertTrue(helper.onTouch(v, down));
        assertTrue(v.isPressed());

        MotionEvent move = MotionEvent.obtain(now, now + 16, MotionEvent.ACTION_MOVE, 60f, 80f, 0);
        helper.onTouch(v, move);
        assertTrue(v.getTranslationX() > 0f);
        assertTrue(v.getTranslationY() > 0f);

        MotionEvent up = MotionEvent.obtain(now, now + 32, MotionEvent.ACTION_UP, 60f, 80f, 0);
        helper.onTouch(v, up);
        assertFalse(v.isPressed());
        down.recycle();
        move.recycle();
        up.recycle();
    }

    @Test
    public void deponder_subclassedFromJava_withBothConstructors() {
        final List<String> created = new ArrayList<>();
        Deponder<String, Pair<String, String>> d = new Deponder<String, Pair<String, String>>(owner, root) {
            @Override
            public String planetId(String s) {
                return s;
            }

            @Override
            public Pair<String, String> rubberPairId(Pair<String, String> r) {
                return r;
            }

            @Override
            public SimplePlanet createPlanet(@NonNull ViewGroup parent, String s) {
                created.add(s);
                return SimplePlanet.builder().itemView(new TextView(parent.getContext())).id(s).build();
            }

            @Override
            public SimpleRubber createRubber(@NonNull ViewGroup parent, Pair<String, String> r) {
                return SimpleRubber.builder().itemView(new View(parent.getContext())).sId(r.first).eId(r.second).build();
            }

            // Overriding a default method of BindAdapter from Java must still work.
            @Override
            public boolean areContentsTheSameByP(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }
        };

        DeponderControl<String, Pair<String, String>> control = d;
        BindAdapter<String, Pair<String, String>> adapter = d;

        // BindAdapter default methods are callable from Java.
        assertEquals("ab", adapter.rubberId(Pair.create("b", "a")));
        assertEquals(0, adapter.typePlanet("a"));
        assertEquals(0, adapter.typeRubber(Pair.create("a", "b")));
        assertTrue(adapter.areContentsTheSameByR(Pair.create("a", "b"), Pair.create("a", "b")));
        SimplePlanet bound = adapter.bindPlanet(SimplePlanet.builder().itemView(new View(context)).id("old").build(), "new");
        assertEquals("new", bound.id());
        SimpleRubber boundR = adapter.bindRubber(
                SimpleRubber.builder().itemView(new View(context)).sId("1").eId("2").build(), Pair.create("x", "y"));
        assertEquals("xy", boundR.id());

        Collection<String> planets = Arrays.asList("a", "b");
        control.submitPlanet(planets);
        control.submitRubber(new ArrayList<Pair<String, String>>());
        control.submitScale(1.2f);
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();
        control.cancel();

        // Second constructor: custom root option.
        SimpleRootOption rootOption = SimpleRoot.builder().itemView(new FrameLayout(context)).minInterval(0L).build();
        Deponder<String, Pair<String, String>> d2 = new Deponder<String, Pair<String, String>>(owner, rootOption) {
            @Override
            public String planetId(String s) {
                return s;
            }

            @Override
            public Pair<String, String> rubberPairId(Pair<String, String> r) {
                return r;
            }

            @Override
            public SimplePlanet createPlanet(@NonNull ViewGroup parent, String s) {
                return SimplePlanet.builder().itemView(new TextView(parent.getContext())).id(s).build();
            }

            @Override
            public SimpleRubber createRubber(@NonNull ViewGroup parent, Pair<String, String> r) {
                return SimpleRubber.builder().itemView(new View(parent.getContext())).sId(r.first).eId(r.second).build();
            }
        };
        d2.cancel();
    }
}
