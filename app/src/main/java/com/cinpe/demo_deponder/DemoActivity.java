package com.cinpe.demo_deponder;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cinpe.demo_deponder.databinding.ActivityDemoBinding;
import com.cinpe.deponder.Deponder;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity";

    private ActivityDemoBinding mBinding;

    Deponder<PlanetOption, RubberOption> deponderProxy;

//    PlanetOption item0;
//    PlanetOption item1;
//    PlanetOption item2;

    List<PlanetOption> pList = new ArrayList<>();
    List<RubberOption> rList = new ArrayList<>();

//    RubberOption ro0to1;


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null & fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

//        item0 = functionP(mBinding.item0);
//        item1 = functionP(mBinding.item1);
//        item2 = functionP(mBinding.item2);
//        ro0to1 = functionR(item0, item1);

        deponderProxy = new Deponder<PlanetOption, RubberOption>(this, mBinding.layoutRoot);

        incubateDate2();

        Log.i(TAG, "[submit]childCount:" + mBinding.layoutRoot);
    }

    @Override
    public void onClickAddPO(View view) {

        PlanetOption po = buildPo();
        mBinding.layoutRoot.addView(po.itemView());
        pList.add(po);

        //提交view集合.
        deponderProxy.submitPlanet(pList);
    }

    @Override
    public void onClickAddRO(View view) {

        int sIndex = new Random().nextInt(pList.size());
        int eIndex = new Random().nextInt(pList.size());
        while (sIndex == eIndex) {
            eIndex = new Random().nextInt(pList.size());
        }

        rList.add(buildRo(pList.get(sIndex), pList.get(eIndex)));
        deponderProxy.submitRubber(rList);
    }


    /**
     * 随便生成些数据2.
     */
    private void incubateDate2() {

        PlanetOption p0 = buildPo();
        PlanetOption p1 = buildPo();

        mBinding.layoutRoot.addView(p0.itemView());
        mBinding.layoutRoot.addView(p1.itemView());

        pList.add(p0);
        pList.add(p1);

        //提交view集合.
        deponderProxy.submit(pList, rList, 1f);

    }

    private PlanetOption buildPo() {
        return SimplePlanet
                .builder()
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.planet_demo, mBinding.layoutRoot, false).getRoot())
                .id(UUID.randomUUID().toString())
                .build();
    }

    private RubberOption buildRo(@NonNull PlanetOption s, @NonNull PlanetOption e) {
        return SimpleRubber.builder()
                .sId(s.id())
                .eId(e.id())
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.rubber_demo, mBinding.layoutRoot, false).getRoot())
                .build();
    }
//
//    2022-02-22 16:29:25.645 9721-14403/? E/AndroidRuntime: FATAL EXCEPTION: RxCachedThreadScheduler-5
//    Process: com.cinpe.demo_deponder, PID: 9721
//    io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException: The exception was not handled due to missing onError handler in the subscribe() method call. Further reading: https://github.com/ReactiveX/RxJava/wiki/Error-Handling | io.reactivex.rxjava3.exceptions.MissingBackpressureException: Could not deliver value due to lack of requests
//    at io.reactivex.rxjava3.internal.functions.Functions$OnErrorMissingConsumer.accept(Functions.java:717)
//    at io.reactivex.rxjava3.internal.functions.Functions$OnErrorMissingConsumer.accept(Functions.java:714)
//    at io.reactivex.rxjava3.internal.subscribers.LambdaSubscriber.onError(LambdaSubscriber.java:79)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableSubscribeOn$SubscribeOnSubscriber.onError(FlowableSubscribeOn.java:102)
//    at io.reactivex.rxjava3.internal.util.AtomicThrowable.tryTerminateConsumer(AtomicThrowable.java:94)
//    at io.reactivex.rxjava3.internal.util.HalfSerializer.onError(HalfSerializer.java:67)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableWithLatestFromMany$WithLatestFromSubscriber.onError(FlowableWithLatestFromMany.java:197)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableObserveOn$BaseObserveOnSubscriber.checkTerminated(FlowableObserveOn.java:209)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableObserveOn$ObserveOnConditionalSubscriber.runAsync(FlowableObserveOn.java:631)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableObserveOn$BaseObserveOnSubscriber.run(FlowableObserveOn.java:176)
//    at io.reactivex.rxjava3.internal.schedulers.ScheduledRunnable.run(ScheduledRunnable.java:65)
//    at io.reactivex.rxjava3.internal.schedulers.ScheduledRunnable.call(ScheduledRunnable.java:56)
//    at java.util.concurrent.FutureTask.run(FutureTask.java:266)
//    at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:301)
//    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
//    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
//    at java.lang.Thread.run(Thread.java:923)
//    Caused by: io.reactivex.rxjava3.exceptions.MissingBackpressureException: Could not deliver value due to lack of requests
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableThrottleFirstTimed$DebounceTimedSubscriber.onNext(FlowableThrottleFirstTimed.java:99)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableMap$MapSubscriber.onNext(FlowableMap.java:69)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableMap$MapSubscriber.onNext(FlowableMap.java:69)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableTimeInterval$TimeIntervalSubscriber.onNext(FlowableTimeInterval.java:69)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableDoOnEach$DoOnEachSubscriber.onNext(FlowableDoOnEach.java:92)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableCreate$LatestAsyncEmitter.drain(FlowableCreate.java:687)
//    at io.reactivex.rxjava3.internal.operators.flowable.FlowableCreate$LatestAsyncEmitter.onNext(FlowableCreate.java:616)
//    at com.cinpe.deponder.Deponder$Listener.onApplyTransformation(Deponder.java:279)
//    at com.cinpe.deponder.NAnimator.applyTransformation(NAnimator.java:58)
//    at android.view.animation.Animation.getTransformation(Animation.java:946)
//    at android.view.animation.Animation.getTransformation(Animation.java:1038)
//    at android.view.View.applyLegacyAnimation(View.java:23453)
//    at android.view.View.draw(View.java:23569)
//    at android.view.ViewGroup.drawChild(ViewGroup.java:5336)
//    at android.view.ViewGroup.dispatchDraw(ViewGroup.java:5093)
//    at androidx.constraintlayout.widget.ConstraintLayout.dispatchDraw(ConstraintLayout.java:1882)
//    at android.view.View.draw(View.java:23904)
//    at android.view.View.updateDisplayListIfDirty(View.java:22776)
//    at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:5320)
//            2022-02-22 16:29:25.646 9721-14403/? E/AndroidRuntime:     at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:5292)
//    at android.view.View.updateDisplayListIfDirty(View.java:22731)
//    at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:5320)
//    at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:5292)
//    at android.view.View.updateDisplayListIfDirty(View.java:22731)
//    at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:5320)
//    at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:5292)
//    at android.view.View.updateDisplayListIfDirty(View.java:22731)
//    at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:5320)
//    at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:5292)
//    at android.view.View.updateDisplayListIfDirty(View.java:22731)
//    at android.view.ViewGroup.recreateChildDisplayList(ViewGroup.java:5320)
//    at android.view.ViewGroup.dispatchGetDisplayList(ViewGroup.java:5292)
//    at android.view.View.updateDisplayListIfDirty(View.java:22731)
//    at android.view.ThreadedRenderer.updateViewTreeDisplayList(ThreadedRenderer.java:579)
//    at android.view.ThreadedRenderer.updateRootDisplayList(ThreadedRenderer.java:585)
//    at android.view.ThreadedRenderer.draw(ThreadedRenderer.java:662)
//    at android.view.ViewRootImpl.draw(ViewRootImpl.java:5042)
//    at android.view.ViewRootImpl.performDraw(ViewRootImpl.java:4749)
//    at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:3866)
//    at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:2618)
//    at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:9971)
//    at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1010)
//    at android.view.Choreographer.doCallbacks(Choreographer.java:809)
//    at android.view.Choreographer.doFrame(Choreographer.java:744)
//    at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:995)
//    at android.os.Handler.handleCallback(Handler.java:938)
//    at android.os.Handler.dispatchMessage(Handler.java:99)
//    at android.os.Looper.loop(Looper.java:246)
//    at android.app.ActivityThread.main(ActivityThread.java:8633)
//    at java.lang.reflect.Method.invoke(Native Method)
//    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:602)
//    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1130)

}