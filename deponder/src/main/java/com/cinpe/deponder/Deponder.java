package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.cinpe.deponder.model.SimpleRootOption;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;
import com.google.common.collect.ImmutableList;

import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function4;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.Timed;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * @Description: T 唯一标识接口.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public class Deponder<PO extends PlanetOption, RO extends RubberOption> {

    public static final String TAG = "Deponder";

    @NonNull
    final LifecycleOwner owner;

    @NonNull
    final private RootOption rootOption;
    final private MutableLiveData<Collection<PO>> poClt = new MutableLiveData<>(Collections.emptyList());
    final private MutableLiveData<Collection<RO>> roClt = new MutableLiveData<>(Collections.emptyList());
    final private MutableLiveData<Float> scaleLD = new MutableLiveData<>(1f);

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
        init();
    }


    private void init() {
        bind(this.rootOption, poClt, roClt, scaleLD);
        this.rootOption.itemView().startAnimation(this.rootOption.animator());
        DeponderHelper.bindDelegateRootTouch(this.rootOption);
    }


    public void submit(@NonNull Collection<PO> pList, @Nullable Collection<RO> rList, float scale) {
        //todo 比对差异.
        submitPlanet(pList);
        submitRubber(rList == null ? ImmutableList.of() : rList);
        submitScale(scale);
    }

    /**
     * 提交P
     */
    public void submitPlanet(@NonNull Collection<PO> pList) {

        Observable.fromIterable(pList)
                .distinct(BaseOption::id)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(l -> {
                    l.forEach(po -> {
                        if (po.itemView().getAnimation() != po.animator()) {
                            po.itemView().startAnimation(po.animator());
                            DeponderHelper.bindPlanet(po);
                        }
                    });
                })
                .subscribe(new DefSingleSubscriber<>(poClt::setValue));
    }

    /**
     * 提交R
     */
    public void submitRubber(@NonNull Collection<RO> rList) {
        Observable.fromIterable(rList)
                .distinct(BaseOption::id)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(l -> {
                    Collection<RO> clt = roClt.getValue();
                    if (clt != null) clt.stream().map(BaseOption::itemView).forEach(v -> {
                        v.clearAnimation();
                        rootOption.itemView().removeView(v);
                    });
                    l.forEach(ro -> {
                        rootOption.itemView().addView(ro.itemView());
                        ro.itemView().startAnimation(ro.animator());
                    });
                })
                .subscribe(new DefSingleSubscriber<>(roClt::setValue));
    }

    /**
     * 提交scale
     */
    public void submitScale(float scale) {
        scaleLD.postValue(scale);
    }
//
//    static class Newton<PO extends PlanetOption, RO extends RubberOption> {
//
//        public Newton(@NonNull PO sPlanet, @NonNull PO ePlanet, @Nullable RO rubberOption) {
//            this.sPlanet = sPlanet;
//            this.ePlanet = ePlanet;
//            this.rubber = rubberOption;
//            this.vector = new Matrix();
//        }
//
//        /**
//         * 评估.
//         */
//        private void evaluate() {
//
//
//            //最终的力.
//            final PointF newton = new PointF();
//
////            final Matrix sMatrix = this.sPlanet.matrix();
////            final Matrix eMatrix = this.ePlanet.matrix();
//
////            final Matrix invert = new Matrix();
////            sMatrix.invert(invert);
//
//            //获取矩阵差.
//            Matrix matrix = DeponderHelper.matrixDiff(this.sPlanet.matrix(), this.ePlanet.matrix());
////            this.vector.setConcat(invert, eMatrix);
//
//
//            //如果不处于斥力感应范围,则reset.
//            float[] values = new float[9];
//            matrix.getValues(values);
//
//            //缩放
//            final float mScale = values[Matrix.MSCALE_Y];
//
//            //角度.
//            double angle = Math.atan2(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
//
//            Log.i(TAG, "计算出的角度:" + angle);
//
//            //缩放后的斜边.
//            float radius = DeponderHelper.pythagorean(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
////            this.vector.mapRadius(radius);
//
//            //评估rubber连接到sPlanet和ePlanet造成的影响.
//
//            //这里values [Matrix.MSCALE_X] = values[Matrix.MSCALE_Y]必成立.
//
//            if (rubber != null) {
//
//                //弹簧的自然长度.
//                final float natural = rubber.naturalLength() * mScale;
//
//                //计算弹簧的形变量.
//                float rubberDiff = DeponderHelper.dDistance(radius, natural);
//
//                PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));
//                Log.i(TAG, "计算弹簧的形变量:" + rubberDiffDistance);
//
//                //计算弹簧的力.
//                PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, rubber.elasticityCoefficient());
//
//                //记录弹簧的力.
//                newton.offset(rubberNewton.x, rubberNewton.y);
//
//            }
//
//
//            Log.d(TAG, "评估() called[半径]=" + radius + ",[矩阵]=" + this.vector);
//
//            final float mInternalPressure = sPlanet.mInternalPressure() * mScale;
//            if (Math.abs(radius) > mInternalPressure) {
//                //超出感应范围->重置.
////                this.vector.reset();
//            } else {
//                float planetDiff = DeponderHelper.dDistance(radius, mInternalPressure);
//
//                //感应范围内, 距离->形变
//                PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));
//
//                //感应范围内, 形变->力
//                PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, sPlanet.elasticityCoefficient());
//                newton.offset(planetDiffNewton.x, planetDiffNewton.y);
//            }
//            this.vector.setTranslate(newton.x, newton.y);
//        }
//
//        /**
//         * sPlanet
//         */
//        @NonNull
//        final PO sPlanet;
//
//        /**
//         * ePlanet
//         */
//        @NonNull
//        final PO ePlanet;
//
//        @Nullable
//        final RO rubber;
//
//        /**
//         * e -> s 的矢量.
//         * 力.
//         */
//        final Matrix vector;
//
//    }


    static class Listener extends MainThreadDisposable implements NAnimator.ApplyTransformationListener {

        final NAnimator nAnimator;
        //        Observer<Float> observer;
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

        //scale
        Flowable<Float> scaleFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, scaleLD)).subscribeOn(Schedulers.io());

        //po.
        Flowable<Collection<PO>> poCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, poClt)).defaultIfEmpty(Collections.emptyList()).subscribeOn(Schedulers.io());

        //ro.
        Flowable<Map<String, RO>> roCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, roClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().collect(Collectors.<RO, String, RO>toMap(BaseOption::id, ro -> ro))).subscribeOn(Schedulers.io());

        Flowable<Collection<NT<PO>>> ntCltFlowable = poCltFlowable.map(clt -> clt.stream().map(p -> {
            Log.i(TAG, "[evaluate]执行了初始化NT:" + p.id());
            return new NT<>(p);
        }).collect(Collectors.toList()));

        Flowable<Long> timer = Flowable.<Float>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.LATEST)
                .doOnNext(f -> Log.i(TAG, "[evaluate]FLOAT:" + f)).timeInterval().map(Timed::time).map(t -> Math.min(16L, t)).subscribeOn(Schedulers.io());


//        timer.withLatestFrom(scaleFlowable, roCltFlowable, Environment::new)
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(new FlowableSubscriber<Environment<RO>>() {
//
//                    Subscription s;
//                    @Override
//                    public void onSubscribe(@NonNull Subscription s) {
//                        this.s=s;
//                        s.request(Long.MAX_VALUE);
//                    }
//
//                    @Override
//                    public void onNext(Environment<RO> environment) {
//                        Log.i(TAG,"[开始搞事!]"+Thread.currentThread().getName());
//                        ntCltFlowable
//                                .doOnNext(clt->Log.i(TAG,"[开始搞事2!]"+Thread.currentThread().getName()))
//                                .flatMapIterable(clt -> clt)
//                                .compose(upstream -> {
//                                            return upstream
//                                                    .doOnNext(s -> {
//                                                        Log.i(TAG, "[evaluate][刚发射出来的s]" + s.p.id() + ",newton:" + s.newton);
//                                                        s.newton.postConcat(evaluate(s.pointF, environment.scale));
//                                                    })
//                                                    .doAfterNext(s -> {
//                                                        Log.i(TAG, "[evaluate][检查]" + s.p.id() + ",newton:" + s.newton);
//                                                        drawPo(s.p, s.newton, environment.time, environment.scale);
//                                                        //更新
//                                                        s.update();
//                                                    })
////                                            .flatMap(s -> iterable.skip(index.incrementAndGet()), (s, e) -> {
////                                                if (s.equals(e)) return new Matrix();
////                                                Matrix m;
////                                                RO ro = stringROMap.get(DeponderHelper.concatId(s.p.id(), e.p.id()));
////                                                if (ro != null) {
////                                                    drawRo(ro, s.pointF, e.pointF, aFloat);
////                                                    m = evaluate(s.pointF, e.pointF, aFloat, true);
////                                                } else {
////                                                    m = evaluate(s.pointF, e.pointF, aFloat, false);
////                                                }
////                                                Matrix v = new Matrix();
////                                                m.invert(v);
////                                                s.newton.postConcat(v);
////                                                e.newton.postConcat(m);
////                                                return m;
////
////                                            });
//                                                    .flatMap(s -> {
//                                                        AtomicInteger index = new AtomicInteger();
//
//                                                        return upstream.skip(index.incrementAndGet()).map((e -> {
//                                                            if (s.equals(e)) return new Matrix();
//                                                            Matrix m;
//                                                            RO ro = environment.map.get(DeponderHelper.concatId(s.p.id(), e.p.id()));
//                                                            if (ro != null) {
//                                                                if (ro.id().startsWith(s.p.id())) {
//                                                                    drawRo(ro, s.pointF, e.pointF, environment.scale);
//                                                                } else {
//                                                                    drawRo(ro, e.pointF, s.pointF, environment.scale);
//                                                                }
//                                                                m = evaluate(s.pointF, e.pointF, environment.scale, true);
//                                                            } else {
//                                                                m = evaluate(s.pointF, e.pointF, environment.scale, false);
//                                                            }
//                                                            Matrix v = new Matrix();
//                                                            m.invert(v);
//                                                            Log.i(TAG, "[evaluate]完成前 s:" + s.newton + ",e:" + e.newton);
//                                                            s.newton.postConcat(v);
//                                                            e.newton.postConcat(m);
//                                                            Log.i(TAG, "[evaluate]完成 s:" + s.p.id() + " " + v + ",e:" + e.p.id() + " " + m);
//                                                            Log.i(TAG, "[evaluate]完成后 s:" + s.newton + ",e:" + e.newton);
//                                                            return e.newton;
//                                                        }));
//
//                                                    }).subscribeOn(Schedulers.io());
//                                        }
//                                )
//
//                                .subscribeOn(Schedulers.io())
//                                .subscribe(new FlowableSubscriber<Matrix>() {
//                                    Subscription s;
//                                    @Override
//                                    public void onSubscribe(@NonNull Subscription s) {
//                                        this.s=s;
//                                        s.request(Long.MAX_VALUE);
//                                    }
//
//                                    @Override
//                                    public void onNext(Matrix matrix) {
//
//                                    }
//
//                                    @Override
//                                    public void onError(Throwable t) {
//                                        s.cancel();
//                                    }
//
//                                    @Override
//                                    public void onComplete() {
//                                        s.cancel();
//                                    }
//                                });
//                    }
//
//                    @Override
//                    public void onError(Throwable t) {
//                        s.cancel();
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        s.cancel();
//                    }
//                });


        timer.observeOn(Schedulers.io()).withLatestFrom(ntCltFlowable, roCltFlowable, scaleFlowable, new Function4<Long, Collection<NT<PO>>, Map<String, RO>, Float, Object>() {
            @Override
            public Object apply(Long timed, Collection<NT<PO>> pos, Map<String, RO> stringROMap, Float aFloat) throws Throwable {
                Flowable.fromIterable(pos)

                        .compose(DeponderHelper.flowableCombinations(upstream -> upstream.skipWhile(nt -> nt.p.itemView().isPressed())
                                        .doOnNext(s -> {
                                            Log.i(TAG, "[evaluate][刚发射出来的s]" + s.p.id() + ",newton:" + s.newton);
                                            s.newton.postConcat(evaluate(s.pointF, aFloat));
                                        })
                                        .doAfterNext(s -> {
                                            Log.i(TAG, "[evaluate][检查]" + s.p.id() + ",newton:" + s.newton);
                                            drawPo(s.p, s.newton, timed, aFloat);
                                            //更新
                                            s.update();
                                        })
                                , (s, e) -> {
                                    if (TextUtils.equals(s.p.id(), e.p.id()))
                                        return new Matrix();
                                    Matrix m;
                                    RO ro = stringROMap.get(DeponderHelper.concatId(s.p.id(), e.p.id()));
                                    if (ro != null) {
                                        if (ro.id().startsWith(s.p.id())) {
                                            drawRo(ro, s.pointF, e.pointF, aFloat);
                                        } else {
                                            drawRo(ro, e.pointF, s.pointF, aFloat);
                                        }
                                        m = evaluate(s.pointF, e.pointF, aFloat, true);
                                    } else {
                                        m = evaluate(s.pointF, e.pointF, aFloat, false);
                                    }
                                    Matrix v = new Matrix();
                                    m.invert(v);
                                    s.newton.postConcat(v);
                                    e.newton.postConcat(m);
                                    return m;

                                }))
                        .subscribeOn(Schedulers.io())
                        .blockingSubscribe();

                return timed;

            }
        })
                .subscribeOn(Schedulers.io())
//                .onBackpressureBuffer(Integer.MAX_VALUE,true,true,()->Log.i(TAG,"[溢出]"))
                .subscribe(new FlowableSubscriber<Object>() {
                    Subscription s;

                    @Override
                    public void onSubscribe(@NonNull Subscription s) {
                        this.s = s;
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.i(TAG, "[溢出]出现了,溢出怪.!" + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        s.cancel();
                    }
                });

    }


    private void logIndexNTime(final String i, int index, final long dt) {
        Log.i(TAG, "[INDEX][" + i + "],[count]: " + index + ",时间间隔: " + dt);
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

//        //todo 防抖.
//        float minAcceleration = DeponderHelper.MIN_ACCELERATION * scale;
//        po.acceleration().set(Math.abs(po.acceleration().x) < minAcceleration ? 0 : po.acceleration().x, Math.abs(po.acceleration().y) < minAcceleration ? 0 : po.acceleration().y);

        //损耗
        acceleration.offset(-speed.x * rootOption.mRootDensity(), -speed.y * rootOption.mRootDensity());


        //进行位移.
        PointF p = DeponderHelper.calculate(speed, acceleration, interval);
        Log.i(TAG, "po[速度]=" + po.speed() + ",[加速度]=" + acceleration + ",[间隔时间]=" + interval + ",[位移]=" + p);

        float[] values = DeponderHelper.values(po.matrix());
        Rect rect = DeponderHelper.hitRect(po.itemView());


        float tempX = scale / values[Matrix.MSCALE_X];
        float tempY = scale / values[Matrix.MSCALE_Y];
        Matrix matrix = new Matrix();
        matrix.postScale(tempX, tempY, (rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
        matrix.postTranslate(p.x, p.y);
//
        po.matrix().postConcat(matrix);

        Log.i("NAnimator", "回调中的hashCode:" + po.matrix().hashCode());
        //记录位移末速度.
        po.speed().postTranslate(acceleration.x * interval, acceleration.y * interval);
        Log.i(TAG, "drawPo" + po.itemView().getAnimation());
    }

    /**
     * 绘制ro.
     */
    @WorkerThread
    protected void drawRo(@Nullable RO ro, @NonNull PO sPo, @NonNull PO ePo, final float scale) {

        if (ro == null) return;

        Log.i(TAG, "[evaluate]绘制RO" + ro.id() + "," + Thread.currentThread().getName());

        final Rect rRect = DeponderHelper.hitRect(ro.itemView());

        final PointF sP = DeponderHelper.centerPointF(sPo);
        final PointF eP = DeponderHelper.centerPointF(ePo);
        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);

        //当前中点.
        final PointF center = DeponderHelper.centerPointF(sP, eP);

        //当前长度.
        final float length = PointF.length(diff.x, diff.y);
        //当前角度.
        final double angle = Math.atan2(diff.x, diff.y);

        final Matrix d = new Matrix();

        d.setRectToRect(DeponderHelper.hitRectF(ro.itemView()), new RectF(center.x - length / 2, center.y - rRect.height() * scale / 2, center.x + length / 2, center.y + rRect.height() * scale / 2), Matrix.ScaleToFit.FILL);
//
//        //设置角度.
//        d.postRotate(Math.round(90 - angle / Math.PI * 180), center.x, center.y);
//
//        ro.matrix().set(d);

//        d.setRectToRect(DeponderHelper.hitRectF(ro.itemView()), new RectF(center.x - rRect.width() * scale * .5f, center.y - rRect.height() * scale * .5f, center.x + rRect.width() * scale * .5f, center.y + rRect.height() * scale * .5f), Matrix.ScaleToFit.FILL);

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

    @NonNull
    private Matrix evaluate(@NonNull final PointF point, final float scale) {

        Log.i(TAG, "[evaluate]====================分析四边[" + point + "]====================================");

        //root的参数.
        final RectF rootRect = DeponderHelper.hitRectF(rootOption.itemView());

        //先获取到root的[四个边距]的[有效]矢量和.
        //正方向取力的方向.
        PointF distance = new PointF();

        final Matrix matrix = new Matrix();

        //分析四边.
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
    private Matrix evaluate(@NonNull final PointF s, @NonNull final PointF e, final float scale, boolean hasRubber) {

        long input = SystemClock.currentThreadTimeMillis();

        //最终的力.
        final Matrix matrix = new Matrix();
        if (s.equals(e)) return matrix;

        final PointF diff = new PointF(e.x - s.x, e.y - s.y);

        //角度.
        double angle = Math.atan2(diff.x, diff.y);

        Log.i(TAG, "计算出的角度:" + angle);

        //缩放后的斜边.
        final float length = diff.length();

        if (hasRubber) {

            //弹簧的自然长度.
            final float natural = DeponderHelper.DEFAULT_RUBBER_NATURAL_LENGTH * scale;

            //计算弹簧的形变量.
            float rubberDiff = DeponderHelper.dDistance(length, natural);

            PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));

            //计算弹簧的力.
            PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT);

            Log.i(TAG, "[评估]斜边长:" + length + ",自然长度:" + natural + ",计算弹簧的形变量" + rubberDiff + ",计算弹簧的坐标形变量:" + rubberDiffDistance + ",弹簧力:" + rubberNewton);

            //记录弹簧的力.
            matrix.postTranslate(rubberNewton.x, rubberNewton.y);


        }

        final float mInternalPressure = DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE * scale;
        if (length < mInternalPressure) {

            float planetDiff = DeponderHelper.dDistance(length, mInternalPressure);

            //感应范围内, 距离->形变
            PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));

            //感应范围内, 形变->力
            PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT);

            matrix.postTranslate(planetDiffNewton.x, planetDiffNewton.y);

            Log.i(TAG, "[评估]PO - 斜边长:" + length + ",自然长度:" + mInternalPressure + ",计算弹簧的形变量" + planetDiff + ",计算弹簧的坐标形变量:" + planetDiffDistance + ",弹簧力:" + planetDiffNewton + ",newton:" + planetDiffNewton);
        }


        Log.i(TAG, "[evaluate]分析内力" + ",耗时:[" + (SystemClock.currentThreadTimeMillis() - input) + "]:" + "[" + s + "," + e + "]");

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
        }

        public void update() {
            newton.reset();
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
