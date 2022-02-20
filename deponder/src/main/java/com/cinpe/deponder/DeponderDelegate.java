package com.cinpe.deponder;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewStructure;
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

        for (int i = rootOption.itemView().getChildCount() - 1; i >= 0; i--) {
            final View child = rootOption.itemView().getChildAt(i);
            if (child != null && sendToDelegate(child, event)) return true;
        }
        if(front!=null){
            front.setPressed(false);
        }
        return super.onTouchEvent(event);
    }

    private boolean sendToDelegate(@NonNull final View view, @NonNull MotionEvent event) {
        boolean b = view.dispatchTouchEvent(event);
        if (b) {
            if (front != view) {
                if (front != null) front.setPressed(false);
                view.bringToFront();
                front = view;
            }
        }
        return b;
    }
}
