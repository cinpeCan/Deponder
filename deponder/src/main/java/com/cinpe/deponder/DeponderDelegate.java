package com.cinpe.deponder;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/10
 * @Version: 0.01
 */
public class DeponderDelegate extends TouchDelegate {

    private static final String TAG = "DeponderDelegate";

    private final Collection<? extends PlanetOption> optionList;

    /**
     * 当前选中的planet
     */
    private PlanetOption option;

    /**
     * Constructor
     */
    public DeponderDelegate(@NonNull Collection<? extends PlanetOption> planetOptions) {
        super(new Rect(1, 1, 1, 1), planetOptions.stream().findFirst().get().itemView());
        optionList = planetOptions;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        int action = event.getAction();
        final PlanetOption mOption = option;
        Log.i(TAG, "进入PlanetOption的touch");

        if (mOption != null) {
            boolean b = sendToDelegate(mOption, event);
            if (b) {
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_OUTSIDE) {
                    mOption.itemView().setPressed(false);
                } else {
                    mOption.itemView().setPressed(true);
                    mOption.speed().set(0, 0);
                }
                return true;
            } else {
                mOption.itemView().setPressed(false);
                option = null;
            }
        }


        if (action == MotionEvent.ACTION_DOWN) {

            final PointF point = new PointF(event.getX(), event.getY());
            boolean intersect = false;

            final Iterator<? extends PlanetOption> iterator = optionList.iterator();

            while (iterator.hasNext() && !intersect) {
                final PlanetOption option = iterator.next();
                Rect hitRect = new Rect();
                option.itemView().getHitRect(hitRect);
                RectF rectF = new RectF(hitRect);
                Matrix matrix = option.matrix();

                Matrix temp = new Matrix();
                float[] floats = new float[9];
                matrix.getValues(floats);
                rectF.offset(floats[Matrix.MTRANS_X], floats[Matrix.MTRANS_Y]);
                temp.postScale(floats[Matrix.MSCALE_X], floats[Matrix.MSCALE_Y], rectF.left, rectF.top);
                temp.mapRect(rectF);

                intersect = rectF.contains(point.x, point.y);

                if (intersect) {
                    this.option = option;

                    intersect = sendToDelegate(option, event);

                    option.itemView().setPressed(true);
                    option.speed().set(0, 0);
                }


            }

        }

        return true;
    }

    private boolean sendToDelegate(final PlanetOption mOption, @NonNull MotionEvent event) {
        Log.i(TAG, "发射事件到planet上");
        return mOption.itemView().dispatchTouchEvent(event);
    }
}
