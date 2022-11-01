package com.cinpe.deponder;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/18
 * @Version: 0.01
 */
public class DefSubscriber<T> extends DisposableSubscriber<T> {
    private static final String TAG = "DefSubscriber";

    @Override
    public void onNext(T transformationTimed) {

    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void onComplete() {

    }


}
