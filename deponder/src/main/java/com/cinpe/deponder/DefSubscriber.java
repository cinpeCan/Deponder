package com.cinpe.deponder;

import android.util.Log;
import android.view.animation.Transformation;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.schedulers.Timed;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/18
 * @Version: 0.01
 */
public class DefSubscriber implements FlowableSubscriber<Timed<Transformation>> {
    private static final String TAG = "DefSubscriber";

    @Override
    public void onNext(Timed<Transformation> transformationTimed) {
    }

    @Override
    public void onError(Throwable t) {
        Log.d(TAG, "doOnError() called with: throwable = [" + t + "]");
    }

    @Override
    public void onComplete() {
        Log.d(TAG, "onComplete() called");
    }

    @Override
    public void onSubscribe(@NonNull Subscription s) {
        s.request(Long.MAX_VALUE);
    }
}
