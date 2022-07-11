package com.cinpe.deponder;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.NonNull;

import com.cinpe.deponder.option.SimpleRootOption;


/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/10
 * @Version: 0.01
 */
public class DeponderDelegate extends TouchDelegate {

    private static final String TAG = "DeponderDelegate";

    final private SimpleRootOption rootOption;
    private volatile View front;

    /**
     * Constructor
     */
    public DeponderDelegate(@NonNull SimpleRootOption rootOption) {
        super(new Rect(1, 1, 1, 1), new View(rootOption.itemView().getContext()));
        this.rootOption = rootOption;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        if (front != null) {
            if (sendToDelegate(front, event)) return true;
        }
        for (int i = rootOption.itemView().getChildCount() - 1; i >= 0; i--) {
            final View child = rootOption.itemView().getChildAt(i);
            if (child != null && sendToDelegate(child, event)) return true;
        }
        if (front != null) {
            front.setPressed(false);
            front = null;
        }
        return false;
    }

    @Override
    public boolean onTouchExplorationHoverEvent(@NonNull MotionEvent event) {
        return super.onTouchExplorationHoverEvent(event);
    }

    private boolean sendToDelegate(@NonNull final View view, @NonNull MotionEvent event) {
        Animation animation = view.getAnimation();
        if (animation instanceof NAnimator) {
            if (((front == view && front.isPressed()) || DeponderHelper.planetCurrentHitRectF(view).contains(event.getX(0), event.getY(0))) && view.dispatchTouchEvent(event)) {
                if (front != view) {
                    if (front != null) front.setPressed(false);
                    view.bringToFront();
                    front = view;
                }
                return true;
            }
        }

        return false;
    }
}
