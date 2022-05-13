package com.cinpe.deponder;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.NonNull;

import com.cinpe.deponder.option.RootOption;


/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/10
 * @Version: 0.01
 */
public class DeponderDelegate extends TouchDelegate {

    private static final String TAG = "DeponderDelegate";

    final private RootOption rootOption;
    private volatile View front;

    /**
     * Constructor
     */
    public DeponderDelegate(@NonNull RootOption rootOption) {
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

    private boolean sendToDelegate(@NonNull final View view, @NonNull MotionEvent event) {

        Animation animation = view.getAnimation();
        if (animation instanceof NAnimator) {
            NAnimator nAnimator = (NAnimator) animation;

            PointF point = new PointF(event.getX(0), event.getY(0));

            RectF rectF = DeponderHelper.hitRectF(view);

            Matrix temp = new Matrix();

            float[] floats = DeponderHelper.values(nAnimator.getMMatrix());

            temp.postScale(floats[Matrix.MSCALE_X], floats[Matrix.MSCALE_Y], rectF.width() * .5f, rectF.height() * .5f);
            temp.mapRect(rectF);
            rectF.offset(floats[Matrix.MTRANS_X], floats[Matrix.MTRANS_Y]);

            if (rectF.contains(point.x, point.y) && view.dispatchTouchEvent(event)) {
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
