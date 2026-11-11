package com.example.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRoot;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.BaseOption;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Locks down the observable behaviour of the (formerly AutoValue) model classes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ModelBehaviorTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    // ---------------------------------------------------------------- SimplePlanet

    @Test
    public void planet_defaults() {
        View v = new View(context);
        SimplePlanet p = SimplePlanet.builder().itemView(v).id("a").build();
        assertEquals(DeponderHelper.DEFAULT_QUALITY_PROPERTY, p.quality(), 0f);
        assertEquals(DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE, p.mInternalPressure(), 0f);
        assertEquals(DeponderHelper.DEFAULT_INIT_SCALE, p.frontalArea(), 0f);
        assertEquals(DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT, p.elasticityCoefficient(), 0f);
    }

    @Test
    public void planet_missingId_fallsBackToViewHashCode() {
        View v = new View(context);
        assertEquals(String.valueOf(v.hashCode()), SimplePlanet.builder().itemView(v).build().id());
    }

    @Test
    public void planet_emptyId_fallsBackToViewHashCode() {
        View v = new View(context);
        assertEquals(String.valueOf(v.hashCode()), SimplePlanet.builder().itemView(v).id("").build().id());
    }

    @Test
    public void planet_missingItemView_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> SimplePlanet.builder().build());
        assertThrows(IllegalStateException.class, () -> SimplePlanet.builder().id("a").build());
    }

    @Test
    public void planet_nullArgument_throwsNpe() {
        assertThrows(NullPointerException.class, () -> SimplePlanet.builder().itemView(null));
        assertThrows(NullPointerException.class, () -> SimplePlanet.builder().id(null));
    }

    @Test
    public void planet_valueEquality() {
        View v = new View(context);
        SimplePlanet a = SimplePlanet.builder().itemView(v).id("a").quality(2f).build();
        SimplePlanet b = SimplePlanet.builder().itemView(v).id("a").quality(2f).build();
        SimplePlanet c = SimplePlanet.builder().itemView(v).id("a").quality(3f).build();
        SimplePlanet d = SimplePlanet.builder().itemView(new View(context)).id("a").quality(2f).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertEquals(a, a.toBuilder().build());
        assertTrue(a.toString().startsWith("SimplePlanet{"));
    }

    // ---------------------------------------------------------------- SimpleRubber

    @Test
    public void rubber_defaults_andId() {
        SimpleRubber r = SimpleRubber.builder().itemView(new View(context)).sId("b").eId("a").build();
        assertEquals(DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT, r.elasticityCoefficient(), 0f);
        assertEquals(DeponderHelper.DEFAULT_RUBBER_NATURAL_LENGTH, r.naturalLength());
        assertEquals("ab", r.id());
    }

    @Test
    public void rubber_sameEndpoints_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> SimpleRubber.builder().itemView(new View(context)).sId("a").eId("a").build());
    }

    @Test
    public void rubber_missingRequired_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> SimpleRubber.builder().sId("a").eId("b").build());
        assertThrows(IllegalStateException.class, () -> SimpleRubber.builder().itemView(new View(context)).eId("b").build());
        assertThrows(IllegalStateException.class, () -> SimpleRubber.builder().itemView(new View(context)).sId("a").build());
    }

    @Test
    public void rubber_vArr_collectsTaggedChildren() {
        FrameLayout group = new FrameLayout(context);
        View tagged = new TextView(context);
        tagged.setTag(DeponderHelper.TAG_OF_UN_RUBBER);
        View plain = new View(context);
        View otherTag = new View(context);
        otherTag.setTag("other");
        group.addView(plain);
        group.addView(tagged);
        group.addView(otherTag);

        SimpleRubber r = SimpleRubber.builder().itemView(group).sId("a").eId("b").build();
        List<BaseOption> arr = r.vArr();
        assertEquals(1, arr.size());
        assertSame(tagged, arr.get(0).itemView());
        assertSame(arr, r.vArr());
        assertThrows(UnsupportedOperationException.class, () -> arr.add(arr.get(0)));

        SimpleRubber leaf = SimpleRubber.builder().itemView(new View(context)).sId("a").eId("b").build();
        assertTrue(leaf.vArr().isEmpty());
    }

    @Test
    public void rubber_valueEquality() {
        View v = new View(context);
        SimpleRubber a = SimpleRubber.builder().itemView(v).sId("a").eId("b").build();
        SimpleRubber b = SimpleRubber.builder().itemView(v).sId("a").eId("b").build();
        SimpleRubber c = SimpleRubber.builder().itemView(v).sId("b").eId("a").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().startsWith("SimpleRubber{"));
    }

    // ---------------------------------------------------------------- SimpleRoot

    @Test
    public void root_defaults() {
        FrameLayout group = new FrameLayout(context);
        SimpleRoot r = SimpleRoot.builder().itemView(group).build();
        assertEquals(DeponderHelper.DEFAULT_INIT_SCALE, r.initScale(), 0f);
        assertEquals(DeponderHelper.DEFAULT_MAX_SCALE, r.maxScale(), 0f);
        assertEquals(DeponderHelper.DEFAULT_MIN_SCALE, r.minScale(), 0f);
        assertEquals(DeponderHelper.DEFAULT_ROOT_DENSITY, r.mRootDensity(), 0f);
        assertEquals(DeponderHelper.DEFAULT_Internal_Pressure, r.mInternalPressure(), 0f);
        assertEquals(DeponderHelper.DEFAULT_Internal_Pressure_START, r.elasticityCoefficientStart(), 0f);
        assertEquals(DeponderHelper.DEFAULT_Internal_Pressure_TOP, r.elasticityCoefficientTop(), 0f);
        assertEquals(DeponderHelper.DEFAULT_Internal_Pressure_END, r.elasticityCoefficientEnd(), 0f);
        assertEquals(DeponderHelper.DEFAULT_Internal_Pressure_BOTTOM, r.elasticityCoefficientBot(), 0f);
        assertEquals(0L, r.minInterval());
        assertEquals(String.valueOf(group.hashCode()), r.id());
    }

    @Test
    public void root_negativeMinInterval_isClampedToZero() {
        SimpleRoot r = SimpleRoot.builder().itemView(new FrameLayout(context)).minInterval(-5L).build();
        assertEquals(0L, r.minInterval());
    }

    @Test
    public void root_missingItemView_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> SimpleRoot.builder().build());
    }

    @Test
    public void root_valueEquality() {
        FrameLayout g = new FrameLayout(context);
        assertEquals(SimpleRoot.builder().itemView(g).build(), SimpleRoot.builder().itemView(g).build());
        assertNotEquals(SimpleRoot.builder().itemView(g).build(), SimpleRoot.builder().itemView(g).maxScale(3f).build());
    }
}
