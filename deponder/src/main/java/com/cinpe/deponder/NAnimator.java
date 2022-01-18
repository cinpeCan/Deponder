package com.cinpe.deponder;

import android.graphics.Matrix;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

import com.cinpe.deponder.option.PlanetOption;
import com.orhanobut.logger.Logger;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
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
    public interface ApplyTransformationListener {
        void onApplyTransformation(Transformation t);
    }

    public NAnimator(@NonNull Matrix matrix) {
        mMatrix = matrix;
        this.setDuration(3600000L);
        this.setZAdjustment(Animation.ZORDER_TOP);
        this.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        if (transformationListener != null)
            transformationListener.onApplyTransformation(t);

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
