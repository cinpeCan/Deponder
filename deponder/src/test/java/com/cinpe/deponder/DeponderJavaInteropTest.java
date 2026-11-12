package com.cinpe.deponder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import com.cinpe.deponder.core.DeponderArrowMode;
import com.cinpe.deponder.core.DeponderDefaults;
import com.cinpe.deponder.core.PlanetPhysics;
import com.cinpe.deponder.core.RootPhysics;
import com.cinpe.deponder.core.RubberPhysics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class DeponderJavaInteropTest {

    @Test
    public void javaStaticDeponderViewsApi_andDeponderRubberViewFluentBuilders_workCleanly() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = activityController.get();

        FrameLayout root = new FrameLayout(activity);
        activity.setContentView(root, new FrameLayout.LayoutParams(600, 600));
        root.layout(0, 0, 600, 600);

        RootPhysics rootPhysics = RootPhysics.fromPreset(DeponderDefaults.Presets.INSTANCE.getBalanced())
                .withScaleBounds(1.0f, 0.5f, 1.8f);
        DeponderController controller = DeponderViews.deponderRoot(root, null, rootPhysics);
        assertNotNull(controller);

        View nodeA = new View(activity);
        View nodeB = new View(activity);
        root.addView(nodeA, new FrameLayout.LayoutParams(48, 48));
        root.addView(nodeB, new FrameLayout.LayoutParams(48, 48));
        nodeA.layout(100, 200, 148, 248);
        nodeB.layout(300, 200, 348, 248);

        DeponderViews.deponderPlanet(nodeA, "java_a", new PlanetPhysics().withPinned(true));
        DeponderViews.deponderPlanet(nodeB, "java_b");
        DeponderViews.deponderDrag(nodeB);

        DeponderRubberView rubberView = new DeponderRubberView(activity)
                .withLabel("depends on")
                .withArrowMode(DeponderArrowMode.END)
                .withLineStyle(Color.DKGRAY, 2.5f)
                .withLabelStyle(Color.BLACK, 11f, Color.TRANSPARENT);
        root.addView(rubberView, new FrameLayout.LayoutParams(120, 28));
        rubberView.layout(0, 0, 120, 28);

        DeponderViews.deponderRubber(
                rubberView,
                "java_a",
                "java_b",
                new RubberPhysics().withNaturalLengthDp(110f)
        );

        controller.submitScale(1.4f);
        controller.submitRubberElasticity(3.0f);
        assertEquals(1.4f, controller.getScale(), 1e-4f);
        assertEquals("depends on", rubberView.getLabel());
        assertEquals(DeponderArrowMode.END, rubberView.getArrowMode());
        assertEquals(Color.TRANSPARENT, rubberView.getLabelBackgroundColor());

        controller.cancel();
        activityController.destroy();
    }
}
