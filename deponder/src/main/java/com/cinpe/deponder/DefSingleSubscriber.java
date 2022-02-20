package com.cinpe.deponder;

import android.util.Log;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/18
 * @Version: 0.01
 */
public class DefSingleSubscriber<T> extends DisposableSingleObserver<T> {
    private static final String TAG = "DefSingleSubscriber";
    final Consumer<T> consumer;
    public DefSingleSubscriber(Consumer<T> consumer)  {
        this.consumer=consumer;
    }

    @Override
    public void onSuccess(@NonNull T t) {
        try {
            consumer.accept(t);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void onError(@NonNull Throwable e) {
        Log.i(TAG, e.getMessage());
    }
}
