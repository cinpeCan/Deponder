package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRoot;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;
import com.cinpe.deponder.option.SimpleRootOption;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.Timed;
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
public abstract class Deponder<P, R> implements DeponderControl<P, R>, BindAdapter<P, R> {

    private static final String TAG = "Deponder";

    @NonNull
    final private LifecycleOwner owner;
    @NonNull
    final private SimpleRootOption rootOption;
    @NonNull
    final private MutableLiveData<Collection<P>> pClt;
    @NonNull
    final private MutableLiveData<Collection<R>> rClt;
    @NonNull
    final private LiveData<Collection<SimplePlanet>> poClt;
    @NonNull
    final private LiveData<Collection<SimpleRubber>> roClt;
    @NonNull
    final private MutableLiveData<Float> scaleLD;

    public Deponder(@NonNull LifecycleOwner lifecycleOwner, @NonNull ViewGroup rootView) {
        this(lifecycleOwner, SimpleRoot.builder()
                .initScale(DeponderHelper.DEFAULT_INIT_SCALE)
                .maxScale(DeponderHelper.DEFAULT_MAX_SCALE)
                .minScale(DeponderHelper.DEFAULT_MIN_SCALE)
                .mRootDensity(DeponderHelper.DEFAULT_ROOT_DENSITY)
                .itemView(rootView)
                .build());
    }

    @SuppressWarnings("ConstantConditions")
    public Deponder(@NonNull LifecycleOwner lifecycleOwner, @NonNull SimpleRootOption rootOption) {
        this.owner = lifecycleOwner;
        this.rootOption = rootOption;
        scaleLD = new MutableLiveData<>(rootOption.initScale());
        pClt = new MutableLiveData<>(Collections.emptyList());
        rClt = new MutableLiveData<>(Collections.emptyList());
        poClt = LiveDataReactiveStreams.fromPublisher(Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(this.owner, pClt))
                .filter(clt -> !Objects.isNull(clt))
                .compose(s -> this.rootOption.minInterval() == 0 ? s : s.debounce(this.rootOption.minInterval(), TimeUnit.MILLISECONDS))
                .map(ImmutableList::copyOf)
                .buffer(2, 1)
                .map(pair -> {
                    Map<String, P> mapOld = pair.get(0).stream().collect(Collectors.toMap(this::planetId, p -> p, (s, e) -> e));
                    Map<String, P> mapNew = pair.get(1).stream().collect(Collectors.toMap(this::planetId, p -> p, (s, e) -> e));
                    return Maps.difference(mapOld, mapNew, DeponderHelper.equivalence);
                })
                .filter(dif -> !dif.entriesOnlyOnLeft().isEmpty() || !dif.entriesOnlyOnRight().isEmpty() || !dif.entriesDiffering().isEmpty())
                .scan(new DataPool<P, SimplePlanet>(), (pool, dif) -> {
                    //left
                    dif.entriesOnlyOnLeft().values().forEach(p -> {
                        SimplePlanet po = pool.clt.remove(planetId(p));
                        if (po != null) {
                            pool.cacheClt.add(po);
                        }
                    });
                    //right
                    if (pool.cacheClt.size() >= dif.entriesOnlyOnRight().size()) {
                        pool.reuseClt.addAll(dif.entriesOnlyOnRight().values());
                    } else {
                        List<P> l = new ArrayList<>(dif.entriesOnlyOnRight().values());
                        for (int i = 0; i < pool.cacheClt.size(); i++) {
                            pool.reuseClt.add(l.remove(0));
                        }
                        pool.createClt.addAll(l);
                    }
                    //dif
                    pool.difClt.addAll(dif.entriesDiffering().values().stream().map(MapDifference.ValueDifference::rightValue).collect(Collectors.toList()));
                    return pool;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(pool -> {
                    pool.reuseClt.forEach(p -> {
                        SimplePlanet sp = bindPlanet(pool.cacheClt.remove(pool.cacheClt.size() - 1), p);
                        pool.clt.put(sp.id(), sp);
                        if (sp.itemView().getParent() == null) {
                            rootOption.itemView().addView(sp.itemView());
                            DeponderHelper.bindDefTouchPlanet(sp);
                            sp.itemView().startAnimation(sp.animator());
                        }
                    });
                    pool.createClt.forEach(p -> {
                        SimplePlanet sp = createPlanet(rootOption.itemView(), p);
                        pool.clt.put(sp.id(), sp);
                        rootOption.itemView().addView(sp.itemView());
                        DeponderHelper.bindDefTouchPlanet(sp);
                        sp.itemView().startAnimation(sp.animator());
                        sp.matrix().setTranslate(new Random().nextFloat(), new Random().nextFloat());
                    });
                    pool.difClt.forEach(p -> {
                        bindPlanet(pool.clt.get(planetId(p)), p);
                    });
                    pool.cacheClt.forEach(po -> {
                        if (po.itemView().getParent() != null) {
                            po.itemView().setOnTouchListener(null);
                            po.itemView().clearAnimation();
                            rootOption.itemView().removeView(po.itemView());
                        }
                    });
                    pool.createClt.clear();
                    pool.reuseClt.clear();
                    pool.difClt.clear();
                })
                .map(pool -> ImmutableList.copyOf(pool.clt.values())));

        roClt = LiveDataReactiveStreams.fromPublisher(Flowable.combineLatest(Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(this.owner, rClt))
                        .filter(clt -> !Objects.isNull(clt)), Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(this.owner, poClt))
                        .filter(clt -> !Objects.isNull(clt)), (rr, pp) -> Flowable.fromIterable(pp).collect(() -> new HashMap<String, String>(), (map, p) -> map.put(p.id(), p.id()))
                        .map(m -> Flowable.fromIterable(rr).filter(r -> {
                            Pair<String, String> pair = this.rubberPairId(r);
                            return m.containsKey(pair.first) && m.containsKey(pair.second);
                        }).toList().subscribeOn(Schedulers.computation()).blockingGet()).blockingGet()
                )
                .compose(s -> this.rootOption.minInterval() == 0 ? s : s.debounce(this.rootOption.minInterval(), TimeUnit.MILLISECONDS))
                .map(ImmutableList::copyOf)
                .buffer(2, 1)
                .map(pair -> {
                    Map<String, R> mapOld = pair.get(0).stream().collect(Collectors.toMap(this::rubberId, r -> r, (s, e) -> e));
                    Map<String, R> mapNew = pair.get(1).stream().collect(Collectors.toMap(this::rubberId, r -> r, (s, e) -> e));
                    return Maps.difference(mapOld, mapNew, DeponderHelper.equivalence);
                })
                .filter(dif -> !dif.entriesOnlyOnLeft().isEmpty() || !dif.entriesOnlyOnRight().isEmpty() || !dif.entriesDiffering().isEmpty())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .scan(Pair.create(ImmutableMap.<String, SimpleRubber>of(), Lists.<SimpleRubber>newArrayList()), (pair, dif) -> {
                    //left
                    dif.entriesOnlyOnLeft().keySet().stream().map(pair.first::get).forEach(ro -> {
                        for (BaseOption o :
                                ro.vArr()) {
                            o.itemView().clearAnimation();
                        }
                        ro.itemView().clearAnimation();
                        rootOption.itemView().removeView(ro.itemView());
                        pair.second.add(ro);
                    });
                    //right
                    Map<String, SimpleRubber> right = dif.entriesOnlyOnRight().values().stream().map(p -> pair.second.isEmpty() ? createRubber(this.rootOption.itemView(), p) : bindRubber(pair.second.remove(0), p))
                            .collect(Collectors.toMap(BaseOption::id, sp -> sp));
                    //diff
                    Map<String, SimpleRubber> diff = dif.entriesDiffering().values().stream().map(p -> bindRubber(pair.first.get(rubberId(p.rightValue())), p.rightValue()))
                            .collect(Collectors.toMap(BaseOption::id, sp -> sp));
                    return Pair.create(ImmutableMap.<String, SimpleRubber>builder().putAll(right).putAll(diff).putAll(dif.entriesInCommon().values().stream().map(p -> pair.first.get(rubberId(p))).collect(Collectors.toMap(BaseOption::id, sp -> sp))).build(), pair.second);
                }).map(p -> p.first)
                .doOnNext(map -> map.values().stream().filter(sp -> Objects.isNull(sp.itemView().getParent())).forEach(ro -> {
                    rootOption.itemView().addView(ro.itemView());
                    ro.itemView().startAnimation(ro.animator());
                    for (BaseOption o :
                            ro.vArr()) {
                        o.itemView().startAnimation(o.animator());
                    }
                })).map(map -> ImmutableList.copyOf(map.values())));
        init();
    }


    private void init() {
        bind(this.rootOption, poClt, roClt, scaleLD);
        this.rootOption.itemView().startAnimation(this.rootOption.animator());
        DeponderHelper.bindDelegateRootTouch(this.rootOption);
    }


    /**
     * submit P
     */
    @Override
    public final void submitPlanet(@NonNull Collection<P> pList) {
        pClt.setValue(pList);
    }

    /**
     * submit R
     */
    @Override
    public final void submitRubber(@NonNull Collection<R> rList) {
        rClt.setValue(rList);
    }

    /**
     * submit scale
     */
    @Override
    public final void submitScale(@FloatRange(from = 0, fromInclusive = false) float scale) {
        if (scale <= rootOption.maxScale() && scale >= rootOption.minScale())
            scaleLD.setValue(scale);
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

    private void bind(@NonNull SimpleRootOption option, @NonNull LiveData<Collection<SimplePlanet>> poClt, @NonNull LiveData<Collection<SimpleRubber>> roClt, @NonNull LiveData<Float> scaleLD) {

        Flowable.<Float>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
                .timeInterval().map(Timed::time).map(t -> Math.min(32L, Math.max(8L, t)))
                .withLatestFrom(Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, scaleLD)).subscribeOn(Schedulers.io()),
                        Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, poClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().map(NT::new).collect(Collectors.toList())).subscribeOn(Schedulers.io()),
                        Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, roClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().collect(Collectors.<SimpleRubber, String, SimpleRubber>toMap(BaseOption::id, ro -> ro))).subscribeOn(Schedulers.io()),
                        Environment::new)
                .flatMap(roEnvironment -> Flowable.fromIterable(roEnvironment.poClt).compose(DeponderHelper.flowableCombinations(upstream -> upstream.doOnNext(s -> s.newton.postConcat(evaluate(s.pointF, roEnvironment.scale))).doAfterNext(s -> {
                            drawPo(s.p, s.newton, roEnvironment.time, roEnvironment.scale);
                            s.update();
                        }), (s, e) -> {
                            if (TextUtils.equals(s.p.id(), e.p.id()))
                                return new Matrix();
                            Matrix m;
                            SimpleRubber ro = roEnvironment.map.get(DeponderHelper.concatId(s.p.id(), e.p.id()));
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
                        .subscribeOn(AndroidSchedulers.mainThread()))
                .subscribeOn(Schedulers.io())
                .ignoreElements()
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

    }


    /**
     * draw po.
     */
    @WorkerThread
    private void drawPo(@NonNull SimplePlanet po, @NonNull Matrix newton, long interval, final float scale) {

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

        float[] values = DeponderHelper.values(po.matrix());
        Rect rect = DeponderHelper.hitRect(po.itemView());

        float tempX = scale / values[Matrix.MSCALE_X];
        float tempY = scale / values[Matrix.MSCALE_Y];
        Matrix matrix = new Matrix();
        matrix.postScale(tempX, tempY, (rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);

        if (p.length() < minAcceleration || po.itemView().isPressed()) {
            po.speed().setTranslate(0, 0);
            p.set(0, 0);
        } else {
            matrix.postTranslate(p.x, p.y);
            //Record the final speed.
            po.speed().postTranslate(acceleration.x * interval, acceleration.y * interval);
        }

        po.matrix().postConcat(matrix);

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

        //force analysis of a four-sided frame
        final float internalPressure = rootOption.mInternalPressure() * scale;
        l = l < internalPressure ? internalPressure - l : 0;
        t = t < internalPressure ? internalPressure - t : 0;
        r = r < internalPressure ? internalPressure - r : 0;
        b = b < internalPressure ? internalPressure - b : 0;

        distance.x = DeponderHelper.calculate(l, rootOption.elasticityCoefficientStart()) - DeponderHelper.calculate(r, rootOption.elasticityCoefficientEnd());//right
        distance.y = DeponderHelper.calculate(t, rootOption.elasticityCoefficientTop()) - DeponderHelper.calculate(b, rootOption.elasticityCoefficientBot());//down

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
     * draw ro.
     */
    @WorkerThread
    private void drawRo(@NonNull SimpleRubber ro, @NonNull PointF sP, @NonNull PointF eP, final float scale) {

        final Rect rRect = DeponderHelper.hitRect(ro.itemView());

        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);

        //current midpoint.
        final PointF center = DeponderHelper.centerPointF(sP, eP);

        //current length.
        final float length = PointF.length(diff.x, diff.y);
        //current angle.
        final double angle = Math.atan2(diff.x, diff.y);

        final Matrix d = new Matrix();

        d.setRectToRect(DeponderHelper.hitRectF(ro.itemView()), new RectF(center.x - length / 2, center.y - rRect.height() * scale / 2, center.x + length / 2, center.y + rRect.height() * scale / 2), Matrix.ScaleToFit.FILL);

        //set the angle.
        d.postRotate(Math.round(90 - angle / Math.PI * 180), center.x, center.y);

        ro.vArr().forEach(o -> {
//            if (o.itemView().getAnimation() == null) {
//                o.itemView().startAnimation(o.animator());
//            }
            PointF c = DeponderHelper.centerPointFInternal(o);
            o.matrix().setScale(rRect.width() / length * scale, scale, c.x, c.y);
        });

        ro.matrix().set(d);

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

    static class Environment<NT, RO extends RubberOption> {

        public Environment(long time, float scale, Collection<NT> poClt, Map<String, RO> map) {
            this.time = time;
            this.scale = scale;
            this.map = map;
            this.poClt = poClt;
        }

        final long time;
        final float scale;
        final Map<String, RO> map;
        final Collection<NT> poClt;

    }

    static class DataDif<T> {

        final Collection<T> removeClt;
        final Collection<T> bindClt;
        final Collection<T> createClt;
        final Collection<T> reuseClt;

        public DataDif(Collection<T> removeClt, Collection<T> bindClt, Collection<T> createClt, Collection<T> reuseClt) {
            this.removeClt = removeClt;
            this.bindClt = bindClt;
            this.createClt = createClt;
            this.reuseClt = reuseClt;
        }
    }

    static class DataPool<P, PO> {

        final public List<P> difClt = new ArrayList<>();
        final public List<P> reuseClt = new ArrayList<>();
        final public List<P> createClt = new ArrayList<>();

        final public List<PO> cacheClt = new ArrayList<>();

        final public Map<String, PO> clt = new HashMap<>();

    }
}
