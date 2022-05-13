package com.cinpe.deponder;

import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.disposables.Disposable;


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
    private final Matrix mMatrix;
    private ApplyTransformationListener transformationListener;

    /**
     * 回调.
     */
    public interface ApplyTransformationListener extends Disposable {
        void onApplyTransformation(float interpolatedTime);

        @Override
        void dispose();
    }

    public NAnimator(@NonNull final Matrix matrix) {
        mMatrix = matrix;
        this.setDuration(3600000L);
        this.setZAdjustment(Animation.ZORDER_TOP);
        this.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        if (transformationListener != null)
            transformationListener.onApplyTransformation(interpolatedTime);
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

    @NonNull
    public Matrix getMMatrix() {
        return mMatrix;
    }

    public void setApplyTransformationListener(ApplyTransformationListener transformationListener) {
        if(this.transformationListener!=null){
            this.transformationListener.dispose();
        }
        this.transformationListener = transformationListener;
    }

    @Override
    public void cancel() {
        super.cancel();
        if(this.transformationListener!=null){
            this.transformationListener.dispose();
        }
    }


}
