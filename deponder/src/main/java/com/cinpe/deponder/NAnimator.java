package com.cinpe.deponder;

import android.graphics.Matrix;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;


/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/24
 * @Version: 0.01
 */
public class NAnimator extends Animation {

    private static final String TAG = "NAnimator";

    @NonNull
    public Matrix getmMatrix() {
        return mMatrix;
    }

    @NonNull
    private final Matrix mMatrix;

    private ApplyTransformationListener transformationListener;

    /**
     * 回调.
     */
    public interface ApplyTransformationListener {
        void onApplyTransformation(Transformation t);
    }

    public NAnimator(@NonNull final Matrix matrix) {
        Log.i("Deponder", "创建了nAnimal:" + matrix.hashCode()+","+matrix);
        mMatrix = matrix;
        this.setDuration(3600000L);
        this.setZAdjustment(Animation.ZORDER_TOP);
        this.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        if (transformationListener != null)
            transformationListener.onApplyTransformation(t);

        Log.i(TAG, "matrix动画中的hashCode:" + mMatrix.hashCode());

        final Matrix fromMatrix = t.getMatrix();
        fromMatrix.set(mMatrix);
        super.applyTransformation(interpolatedTime, t);
    }


    @Override
    public boolean willChangeBounds() {
        return super.willChangeBounds();
    }

    @Override
    public long computeDurationHint() {
        return Long.MAX_VALUE;
    }

    public void setApplyTransformationListener(ApplyTransformationListener transformationListener) {
        this.transformationListener = transformationListener;
    }

}
