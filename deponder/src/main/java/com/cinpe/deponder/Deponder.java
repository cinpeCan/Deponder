package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.SimpleRootOption;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function4;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.Timed;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/*
 * Copyright (c) 2022-present, Cinpecan and Deponder Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Deponder baseUrl:https://github.com/cinpeCan/DemoDeponder
 *
 * Deponder Subcomponents:
 *
 * The following components are provided under the Apache License. See project link for details.
 * The text of each license is the standard Apache 2.0 license.
 *
 * io.reactivex.rxjava3:rxandroid from https://github.com/ReactiveX/RxJava Apache-2.0 License
 * com.google.guava from https://github.com/google/guava Apache-2.0 License
 * com.google.auto.value from https://github.com/google/auto Apache-2.0 License
 */

/**
 * @Description:
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public final class Deponder<PO extends PlanetOption, RO extends RubberOption> implements DeponderControl<PO, RO> {

    public static final String TAG = "Deponder";

    @NonNull
    final LifecycleOwner owner;
    @NonNull
    final private RootOption rootOption;
    @NonNull
    final private MutableLiveData<Collection<PO>> poClt;
    @NonNull
    final private MutableLiveData<Collection<RO>> roClt;
    @NonNull
    final private MutableLiveData<Float> scaleLD;

    public Deponder(@NonNull LifecycleOwner lifecycleOwner, @NonNull ViewGroup rootView) {
        this(lifecycleOwner, SimpleRootOption.builder()
                .initScale(DeponderHelper.DEFAULT_INIT_SCALE)
                .maxScale(DeponderHelper.DEFAULT_MAX_SCALE)
                .minScale(DeponderHelper.DEFAULT_MIN_SCALE)
                .mRootDensity(DeponderHelper.DEFAULT_ROOT_DENSITY)
                .id(UUID.randomUUID().toString())
                .itemView(rootView)
                .build());
    }

    public Deponder(@NonNull LifecycleOwner lifecycleOwner, @NonNull RootOption rootOption) {
        this.owner = lifecycleOwner;
        this.rootOption = rootOption;
        poClt = new MutableLiveData<>(Collections.emptyList());
        roClt = new MutableLiveData<>(Collections.emptyList());
        scaleLD = new MutableLiveData<>(rootOption.initScale());
        init();
    }


    private void init() {
        bind(this.rootOption, poClt, roClt, scaleLD);
        this.rootOption.itemView().startAnimation(this.rootOption.animator());
        DeponderHelper.bindDelegateRootTouch(this.rootOption);
    }


    /**
     * 提交P
     */
    @Override
    public void submitPlanet(@NonNull Collection<PO> pList) {

        Observable.fromIterable(pList)
                .distinct(BaseOption::id)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(l -> {
                    Collection<PO> clt = poClt.getValue();
                    if (clt != null) {
                        Map<String, PO> mapOld = clt.stream().collect(Collectors.toMap(BaseOption::id, po -> po));
                        Map<String, PO> mapNew = l.stream().collect(Collectors.toMap(BaseOption::id, po -> po));
                        MapDifference<String, PO> dif = Maps.difference(mapOld, mapNew);
                        dif.entriesOnlyOnLeft().values().stream().map(BaseOption::itemView).forEach(View::clearAnimation);
                    }
                })
                .doOnSuccess(l -> l.stream().filter(po -> !Objects.equals(po.itemView().getAnimation(), po.animator())).forEach(po -> {
                    po.itemView().startAnimation(po.animator());
                    DeponderHelper.bindPlanet(po);
                }))
                .subscribe(new DefSingleSubscriber<>(poClt::postValue));
    }

    /**
     * 提交R
     */
    @Override
    public void submitRubber(@NonNull Collection<RO> rList) {
        Observable.fromIterable(rList)
                .distinct(BaseOption::id)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(l -> {
                    Collection<RO> clt = roClt.getValue();
                    if (clt != null) {
                        Map<String, RO> mapOld = clt.stream().collect(Collectors.toMap(BaseOption::id, ro -> ro));
                        Map<String, RO> mapNew = l.stream().collect(Collectors.toMap(BaseOption::id, ro -> ro));
                        Maps.difference(mapOld, mapNew).entriesOnlyOnLeft().values().stream().map(BaseOption::itemView).forEach(v -> {
                            v.clearAnimation();
                            rootOption.itemView().removeView(v);
                        });
                    }
                })
                .doOnSuccess(l -> l.stream().filter(ro -> !Objects.equals(ro.itemView().getAnimation(), ro.animator())).forEach(ro -> {
                    rootOption.itemView().addView(ro.itemView());
                    ro.itemView().startAnimation(ro.animator());
                }))
                .subscribe(new DefSingleSubscriber<>(roClt::postValue));
    }

    /**
     * 提交scale
     */
    @Override
    public void submitScale(@FloatRange(from = 0, fromInclusive = false) float scale) {
        if (scale <= rootOption.maxScale() && scale >= rootOption.minScale())
            scaleLD.postValue(scale);
    }

    @Override
    public void cancel() {
        rootOption.itemView().clearAnimation();
    }


    static class Listener extends MainThreadDisposable implements NAnimator.ApplyTransformationListener {

        final NAnimator nAnimator;
        final FlowableEmitter<Float> emitter;

        public Listener(NAnimator nAnimator, FlowableEmitter<Float> emitter) {
            this.nAnimator = nAnimator;
            this.emitter = emitter;

        }

        @Override
        public void onApplyTransformation(float t) {
            emitter.onNext(t);
        }

        @Override
        protected void onDispose() {
            emitter.onComplete();
        }
    }

    private void bind(@NonNull RootOption option, @NonNull LiveData<Collection<PO>> poClt, @NonNull LiveData<Collection<RO>> roClt, @NonNull LiveData<Float> scaleLD) {

        Flowable<? extends Collection<NT<PO>>> ntCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, poClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().map(NT::new).collect(Collectors.toList())).subscribeOn(Schedulers.io());

        Flowable.<Float>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
                .timeInterval().map(Timed::time).map(t -> Math.min(16L, t))
                .withLatestFrom(Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, scaleLD)).subscribeOn(Schedulers.io()),
                        Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, roClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().collect(Collectors.<RO, String, RO>toMap(BaseOption::id, ro -> ro))).subscribeOn(Schedulers.io()),
                        Environment::new)
//                .onBackpressureBuffer()
                .withLatestFrom(ntCltFlowable, (roEnvironment, nts) -> {
                    Flowable.fromIterable(nts).compose(DeponderHelper.flowableCombinations(upstream -> upstream.doOnNext(s -> s.newton.postConcat(evaluate(s.pointF, roEnvironment.scale))).doAfterNext(s -> {
                        drawPo(s.p, s.newton, roEnvironment.time, roEnvironment.scale);
                        //更新
                        s.update();
                    }), (s, e) -> {
                        if (TextUtils.equals(s.p.id(), e.p.id()))
                            return new Matrix();
                        Matrix m;
                        RO ro = roEnvironment.map.get(DeponderHelper.concatId(s.p.id(), e.p.id()));
                        if (ro != null) {
                            if (ro.id().startsWith(s.p.id())) {
                                drawRo(ro, s.pointF, e.pointF, roEnvironment.scale);
                            } else {
                                drawRo(ro, e.pointF, s.pointF, roEnvironment.scale);
                            }
                            s.count.incrementAndGet();
                            e.count.incrementAndGet();
                            m = evaluate(s.pointF, e.pointF, roEnvironment.scale, ro.elasticityCoefficient(), ro.naturalLength(), s.p.elasticityCoefficient(), s.p.mInternalPressure() + s.count.get() * DeponderHelper.INCREMENTAL_SCALE);
                        } else {
                            m = evaluate(s.pointF, e.pointF, roEnvironment.scale, 0, 0, s.p.elasticityCoefficient(), s.p.mInternalPressure() + s.count.get() * DeponderHelper.INCREMENTAL_SCALE);
                        }
                        Matrix v = new Matrix();
                        m.invert(v);
                        s.newton.postConcat(v);
                        e.newton.postConcat(m);
                        return m;
                    }))
                            .subscribeOn(Schedulers.io())
                            .blockingSubscribe();

                    return roEnvironment.time;

                })
                .subscribeOn(Schedulers.io())
                .subscribe(new FlowableSubscriber<Object>() {
                    Subscription s;

                    @Override
                    public void onSubscribe(@NonNull Subscription s) {
                        this.s = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(Object o) {
                        s.request(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.i(TAG, "" + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        s.cancel();
                    }
                });

    }


    /**
     * 绘制po.
     */
    @WorkerThread
    protected void drawPo(@NonNull PO po, @NonNull Matrix newton, long interval, final float scale) {

        Log.i(TAG, "[evaluate]绘制PO" + po.id());


        final float[] n = DeponderHelper.values(newton);
        final float[] s = DeponderHelper.values(po.speed());

        PointF acceleration = new PointF(n[Matrix.MTRANS_X] / po.quality(), n[Matrix.MTRANS_Y] / po.quality());
        PointF speed = new PointF(s[Matrix.MTRANS_X], s[Matrix.MTRANS_Y]);


        //loss
        acceleration.offset(-speed.x * rootOption.mRootDensity(), -speed.y * rootOption.mRootDensity());


        //displacement
        PointF p = DeponderHelper.calculate(speed, acceleration, interval);

        //Low-speed anti-shake
        final float minAcceleration = DeponderHelper.MIN_ACCELERATION * scale;
        if (p.length() < minAcceleration || po.itemView().isPressed()) {
            po.speed().setTranslate(0, 0);
            p.set(0, 0);
        }

        float[] values = DeponderHelper.values(po.matrix());
        Rect rect = DeponderHelper.hitRect(po.itemView());

        float tempX = scale / values[Matrix.MSCALE_X];
        float tempY = scale / values[Matrix.MSCALE_Y];
        Matrix matrix = new Matrix();
        matrix.postScale(tempX, tempY, (rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
        matrix.postTranslate(p.x, p.y);
//
        po.matrix().postConcat(matrix);

        //Record the final speed.
        po.speed().postTranslate(acceleration.x * interval, acceleration.y * interval);

    }

    @NonNull
    private Matrix evaluate(@NonNull final PointF point, final float scale) {

        final RectF rootRect = DeponderHelper.hitRectF(rootOption.itemView());

        //displacement vector
        PointF distance = new PointF();

        final Matrix matrix = new Matrix();

        //rectangle with four sides
        float l = point.x;
        float t = point.y;
        float r = rootRect.width() - point.x;
        float b = rootRect.height() - point.y;

        //四边产生的力.
        final float internalPressure = rootOption.mInternalPressure() * scale;
        l = l < internalPressure ? internalPressure - l : 0;
        t = t < internalPressure ? internalPressure - t : 0;
        r = r < internalPressure ? internalPressure - r : 0;
        b = b < internalPressure ? internalPressure - b : 0;

        distance.x = l - r;//向右
        distance.y = t - b;//向下

        //step#1
        distance.set(DeponderHelper.calculate(distance, rootOption.elasticityCoefficient()));

        matrix.postTranslate(distance.x, distance.y);

        return matrix;
    }


    @NonNull
    private Matrix evaluate(@NonNull final PointF s, @NonNull final PointF e, final float scale, final float rubber_elasticity_coefficient, final int rubber_natural_length, float elasticity_coefficient, float planet_internal_pressure) {

        //newton
        final Matrix matrix = new Matrix();
        if (s.equals(e)) return matrix;

        final PointF diff = new PointF(e.x - s.x, e.y - s.y);

        double angle = Math.atan2(diff.x, diff.y);

        //hypotenuse
        final float length = diff.length();

        if (rubber_elasticity_coefficient != 0) {

            //natural length
            final float natural = rubber_natural_length * scale;

            //Deformation variable
            float rubberDiff = DeponderHelper.dDistance(length, natural);

            PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));

            //newton
            PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, rubber_elasticity_coefficient);

            //save
            matrix.postTranslate(rubberNewton.x, rubberNewton.y);


        }

        final float mInternalPressure = planet_internal_pressure * scale;
        if (length < mInternalPressure) {

            float planetDiff = DeponderHelper.dDistance(length, mInternalPressure);

            //displacement->Deformation variable
            PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));

            //Deformation variable->Newton
            PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, elasticity_coefficient);

            matrix.postTranslate(planetDiffNewton.x, planetDiffNewton.y);

        }

        return matrix;
    }


    /**
     * 绘制ro.
     */
    @WorkerThread
    protected void drawRo(@NonNull RO ro, @NonNull PointF sP, @NonNull PointF eP, final float scale) {

        Log.i(TAG, "[evaluate]绘制RO" + ro.id() + "," + Thread.currentThread().getName());

        final Rect rRect = DeponderHelper.hitRect(ro.itemView());

        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);

        //当前中点.
        final PointF center = DeponderHelper.centerPointF(sP, eP);

        //当前长度.
        final float length = PointF.length(diff.x, diff.y);
        //当前角度.
        final double angle = Math.atan2(diff.x, diff.y);

        final Matrix d = new Matrix();

        d.setRectToRect(DeponderHelper.hitRectF(ro.itemView()), new RectF(center.x - length / 2, center.y - rRect.height() * scale / 2, center.x + length / 2, center.y + rRect.height() * scale / 2), Matrix.ScaleToFit.FILL);

        //设置角度.
        d.postRotate(Math.round(90 - angle / Math.PI * 180), center.x, center.y);

        ro.vArr().forEach(o -> {
            if (o.itemView().getAnimation() == null) {
                o.itemView().startAnimation(o.animator());
            }
            PointF c = DeponderHelper.centerPointFInternal(o);
            o.matrix().setScale(rRect.width() / length * scale, scale, c.x, c.y);
        });

        ro.matrix().set(d);

        Log.i(TAG, "drawRo");
    }


    static class NT<PO extends PlanetOption> {

        public NT(@NonNull PO p) {
            this.p = p;
            this.newton = new Matrix();
            this.pointF = DeponderHelper.centerPointF(p);
            this.count = new AtomicInteger();
//            this.speed = new Matrix();
        }

        public void update() {
            newton.reset();
            count.set(0);
            this.pointF.set(DeponderHelper.centerPointF(p));
        }

        /**
         * Planet
         */
        @NonNull
        final PO p;

        /**
         * ePlanet
         */
        @NonNull
        final Matrix newton;

        @NonNull
        final PointF pointF;
        @NonNull
        final AtomicInteger count;

//        @NonNull
//        final Matrix speed;

    }

    static class Environment<RO extends RubberOption> {

        public Environment(long time, float scale, Map<String, RO> map) {
            this.time = time;
            this.scale = scale;
            this.map = map;
        }

        long time;
        float scale;
        Map<String, RO> map;

    }
}
