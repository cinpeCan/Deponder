package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.cinpe.deponder.model.MyRootOption;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;

import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Function3;
import io.reactivex.rxjava3.functions.Function4;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.Timed;

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
        this(lifecycleOwner, MyRootOption.builder()
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
        submitRubber(rList);
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
//                        ro.itemView().setVisibility(View.GONE);
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

    static class Newton<PO extends PlanetOption, RO extends RubberOption> {

        public Newton(@NonNull PO sPlanet, @NonNull PO ePlanet, @Nullable RO rubberOption) {
            this.sPlanet = sPlanet;
            this.ePlanet = ePlanet;
            this.rubber = rubberOption;
            this.vector = new Matrix();
        }

        /**
         * 评估.
         */
        private void evaluate() {


            //最终的力.
            final PointF newton = new PointF();

//            final Matrix sMatrix = this.sPlanet.matrix();
//            final Matrix eMatrix = this.ePlanet.matrix();

//            final Matrix invert = new Matrix();
//            sMatrix.invert(invert);

            //获取矩阵差.
            Matrix matrix = DeponderHelper.matrixDiff(this.sPlanet.matrix(), this.ePlanet.matrix());
//            this.vector.setConcat(invert, eMatrix);


            //如果不处于斥力感应范围,则reset.
            float[] values = new float[9];
            matrix.getValues(values);

            //缩放
            final float mScale = values[Matrix.MSCALE_Y];

            //角度.
            double angle = Math.atan2(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);

            Log.i(TAG, "计算出的角度:" + angle);

            //缩放后的斜边.
            float radius = DeponderHelper.pythagorean(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
//            this.vector.mapRadius(radius);

            //评估rubber连接到sPlanet和ePlanet造成的影响.

            //这里values [Matrix.MSCALE_X] = values[Matrix.MSCALE_Y]必成立.

            if (rubber != null) {

                //弹簧的自然长度.
                final float natural = rubber.naturalLength() * mScale;

                //计算弹簧的形变量.
                float rubberDiff = DeponderHelper.dDistance(radius, natural);

                PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));
                Log.i(TAG, "计算弹簧的形变量:" + rubberDiffDistance);

                //计算弹簧的力.
                PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, rubber.elasticityCoefficient());

                //记录弹簧的力.
                newton.offset(rubberNewton.x, rubberNewton.y);

            }


            Log.d(TAG, "评估() called[半径]=" + radius + ",[矩阵]=" + this.vector);

            final float mInternalPressure = sPlanet.mInternalPressure() * mScale;
            if (Math.abs(radius) > mInternalPressure) {
                //超出感应范围->重置.
//                this.vector.reset();
            } else {
                float planetDiff = DeponderHelper.dDistance(radius, mInternalPressure);

                //感应范围内, 距离->形变
                PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));

                //感应范围内, 形变->力
                PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, sPlanet.elasticityCoefficient());
                newton.offset(planetDiffNewton.x, planetDiffNewton.y);
            }
            this.vector.setTranslate(newton.x, newton.y);
        }

        /**
         * sPlanet
         */
        @NonNull
        final PO sPlanet;

        /**
         * ePlanet
         */
        @NonNull
        final PO ePlanet;

        @Nullable
        final RO rubber;

        /**
         * e -> s 的矢量.
         * 力.
         */
        final Matrix vector;

    }


    static class Listener extends MainThreadDisposable implements NAnimator.ApplyTransformationListener {

        NAnimator nAnimator;
        //        Observer<Float> observer;
        FlowableEmitter<Float> emitter;

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
            nAnimator.setApplyTransformationListener(null);
        }
    }

    private void bind(@NonNull RootOption option, @NonNull LiveData<Collection<PO>> poClt, @NonNull LiveData<Collection<RO>> roClt, @NonNull LiveData<Float> scaleLD) {

        //scale
        Flowable<Float> scaleFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, scaleLD)).subscribeOn(Schedulers.io());

        //po.
        Flowable<Collection<PO>> poCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, poClt)).defaultIfEmpty(Collections.emptyList()).subscribeOn(Schedulers.io());

        //ro.
        Flowable<Map<String, RO>> roCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, roClt)).defaultIfEmpty(Collections.emptyList()).map(clt -> clt.stream().collect(Collectors.<RO, String, RO>toMap(BaseOption::id, ro -> ro))).subscribeOn(Schedulers.io());

        Flowable<Long> timer = Flowable.<Float>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
                .doOnNext(f -> Log.i(TAG, "[evaluate]FLOAT:" + f)).timeInterval().map(Timed::time).map(t -> Math.min(16L, t)).subscribeOn(Schedulers.io());

        timer.withLatestFrom(poCltFlowable, roCltFlowable, scaleFlowable, new Function4<Long, Collection<PO>, Map<String, RO>, Float, Object>() {
            @Override
            public Object apply(Long timed, Collection<PO> pos, Map<String, RO> stringROMap, Float aFloat) throws Throwable {
                Log.i(TAG, "[evaluate]" + timed + "====================================================================================================================================================================================");
                return Flowable.fromIterable(pos)
                        .compose(upstream -> upstream
                                .doOnNext(po -> {
                                    //分析四边的压力.
                                    final PointF newtonRoot = evaluateRootInternalPressure(po, aFloat);
                                    po.acceleration().offset(newtonRoot.x / po.quality(), newtonRoot.y / po.quality());
                                })
                                .doAfterNext(po -> {
                                    //绘制po.
                                    drawPo(po, timed, aFloat);
                                })
                                //转newton.
                                .flatMap(new Function<PO, Publisher<PO>>() {
                                    final AtomicInteger index = new AtomicInteger();

                                    @Override
                                    public Publisher<PO> apply(PO po) throws Throwable {
                                        Log.i(TAG, "[个数]当前的序号是:" + index.get());
                                        return upstream.skip(index.incrementAndGet()).switchIfEmpty(index.get() > 1 ? Flowable.empty() : upstream);
                                    }
                                }, new BiFunction<PO, PO, Newton<PO, RO>>() {
                                    @Override
                                    public Newton<PO, RO> apply(PO s, PO e) throws Throwable {

                                        //进行解析.
                                        if (TextUtils.equals(s.id(), e.id())) {
                                            Log.i(TAG, "出现了!相同怪!");

                                            return new Newton<>(s, e, null);
                                        } else {
                                            return stringROMap
                                                    .containsKey(s.id() + e.id()) ? new Newton<>(s, e, stringROMap.get(s.id() + e.id())) : new Newton<>(e, s, stringROMap.get(e.id() + s.id()));
                                        }
                                    }
                                })
                                .doOnNext(n -> {
                                    //分析内力
                                    final PointF f = evaluate(n.sPlanet, n.ePlanet, n.rubber, aFloat);
                                    n.sPlanet.acceleration().offset(-f.x / n.sPlanet.quality(), -f.y / n.sPlanet.quality());
                                    n.ePlanet.acceleration().offset(f.x / n.ePlanet.quality(), f.y / n.ePlanet.quality());
                                })
                                .doOnNext(n -> drawRo(n.rubber, n.sPlanet, n.ePlanet, aFloat))

                        )
                        .blockingLast();

            }
        }).timeInterval().map(Timed::time)
                .subscribe(new DefSubscriber<Long>() {
                    @Override
                    public void onNext(Long transformationTimed) {
                        super.onNext(transformationTimed);
                        Log.i(TAG, "[evaluate] 最终耗时:" + transformationTimed);
                    }
                });


    }

    /**
     * 计算四边对po的压力.(而不是加速度)
     */
    @WorkerThread
    protected @NonNull
    PointF evaluateRootInternalPressure(@NonNull PO po, final float scale) {

        Log.i(TAG, "[evaluate]====================分析四边[" + po.id() + "]====================================");


        //先获取到root的[四个边距]的[有效]矢量和.
        //正方向取力的方向.
        final PointF wallDistance = new PointF();
//        return wallDistance;


        if (!po.itemView().isPressed()) {

            //root的参数.
            Rect rootRect = new Rect();
            rootOption.itemView().getHitRect(rootRect);
            rootOption.rectF().set(rootRect);
            rootOption.matrix().mapRect(rootOption.rectF());

            //先获取当前的矩形.
            final Rect rect = DeponderHelper.hitRect(po.itemView());
            final float[] floats = DeponderHelper.values(po.matrix());
            final PointF centerPoint = new PointF(rect.centerX() + floats[Matrix.MTRANS_X], rect.centerY() + floats[Matrix.MTRANS_Y]);

            Log.i(TAG, "步骤一,小矩形:" + rect + ",小矩阵:" + po.matrix() + ",中心点:" + centerPoint + ",精确rectF:" + po.rectF());

            //分析四边.
            float l = centerPoint.x;
            float t = centerPoint.y;
            float r = rootOption.rectF().width() - centerPoint.x;
            float b = rootOption.rectF().height() - centerPoint.y;

            //四边产生的力.
            final float internalPressure = rootOption.mInternalPressure() * scale;
            l = l < internalPressure ? internalPressure - l : 0;
            t = t < internalPressure ? internalPressure - t : 0;
            r = r < internalPressure ? internalPressure - r : 0;
            b = b < internalPressure ? internalPressure - b : 0;

            wallDistance.x = l - r;//向右
            wallDistance.y = t - b;//向下

            //step#1
            wallDistance.set(DeponderHelper.calculate(wallDistance, rootOption.elasticityCoefficient()));

            Log.d(TAG, "步骤一() 加速度 = [" + wallDistance + "]" + ",边界距离向量:" + wallDistance + ",大矩形:" + rootOption.rectF() + ",小矩形:" + po.rectF() + ",感应范围:" + rootOption.mInternalPressure());

            //处理缩放.
            wallDistance.set(wallDistance.x, wallDistance.y);
        }

        return wallDistance;
    }

    AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 评估.
     */
    @WorkerThread
    protected PointF evaluate(@NonNull PO s, @NonNull PO e, @Nullable RO r, final float scale) {

        Log.i(TAG, "[evaluate]分析内力" + atomicInteger.incrementAndGet() + ":" + "[" + s.id() + "," + e.id() + "]" + (r == null ? "" : r.id()));

        //最终的力.
        final PointF newton = new PointF();
        if (TextUtils.equals(s.id(), e.id())) return newton;

        final PointF sP = DeponderHelper.centerPointF(s);
        final PointF eP = DeponderHelper.centerPointF(e);
        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);

        //角度.
        double angle = Math.atan2(diff.x, diff.y);

        Log.i(TAG, "计算出的角度:" + angle);

        //缩放后的斜边.
        float radius = diff.length();

        if (r != null) {

            //弹簧的自然长度.
            final float natural = r.naturalLength() * scale;

            //计算弹簧的形变量.
            float rubberDiff = DeponderHelper.dDistance(radius, natural);

            PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));

            //计算弹簧的力.
            PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, r.elasticityCoefficient());

            Log.i(TAG, "[评估]斜边长:" + radius + ",自然长度:" + natural + ",计算弹簧的形变量" + rubberDiff + ",计算弹簧的坐标形变量:" + rubberDiffDistance + ",弹簧力:" + rubberNewton);

            //记录弹簧的力.
            newton.offset(rubberNewton.x, rubberNewton.y);

        }

        Log.d(TAG, "评估() called[半径]=" + radius + ",[矩阵]=" + newton);

        final float mInternalPressure = s.mInternalPressure() * scale;
        if (Math.abs(radius) > mInternalPressure) {
            //超出感应范围->重置.
//                this.vector.reset();
            Log.i(TAG, "[评估]PO - 跳过" + radius);
        } else {
            float planetDiff = DeponderHelper.dDistance(radius, mInternalPressure);

            //感应范围内, 距离->形变
            PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));

            //感应范围内, 形变->力
            PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, s.elasticityCoefficient());
            Log.i(TAG, "[评估]PO - newton:" + newton);

            newton.offset(planetDiffNewton.x, planetDiffNewton.y);

            Log.i(TAG, "[评估]PO - 斜边长:" + radius + ",自然长度:" + mInternalPressure + ",计算弹簧的形变量" + planetDiff + ",计算弹簧的坐标形变量:" + planetDiffDistance + ",弹簧力:" + planetDiffNewton + ",newton:" + newton);
        }

        return newton;
    }


    /**
     * 绘制po.
     */
    @WorkerThread
    protected void drawPo(@NonNull PO po, long interval, final float scale) {

        Log.i(TAG, "[evaluate]绘制PO" + po.id());

        if (po.itemView().isPressed()) {
            return;
        }

        //损耗
        po.acceleration().offset(-po.speed().x * rootOption.mRootDensity(), -po.speed().y * rootOption.mRootDensity());

        //todo 防抖.
        //float minAcceleration = DeponderHelper.MIN_ACCELERATION * scale;
        //po.acceleration().set(Math.abs(po.acceleration().x) < minAcceleration ? 0 : po.acceleration().x, Math.abs(po.acceleration().y) < minAcceleration ? 0 : po.acceleration().y);

        //进行位移.
        PointF p = DeponderHelper.calculate(po.speed(), po.acceleration(), interval);
        Log.i(TAG, "po[速度]=" + po.speed() + ",[加速度]=" + po.acceleration() + ",[间隔时间]=" + interval + ",[位移]=" + p);

        float[] values = DeponderHelper.values(po.matrix());
        Rect rect = DeponderHelper.hitRect(po.itemView());


        float tempX = scale / values[Matrix.MSCALE_X];
        float tempY = scale / values[Matrix.MSCALE_Y];
//        po.matrix().postScale(temp, temp, (rect.width() >> 1) + values[Matrix.MTRANS_X], (rect.height() >> 1) + values[Matrix.MTRANS_Y]);

        Matrix matrix = new Matrix();
//        Log.i(TAG, "scale:" + scale + ",centerX:" + rect.centerX() + ",centerY:" + rect.centerY() + ",matrixTx" + values[Matrix.MTRANS_X] + ",matrixTy" + values[Matrix.MTRANS_Y]);
//        float oCenterX = (rect.width() / 2f) * values[Matrix.MSCALE_X] * (tempX - 1);
//        float oCenterY = (rect.height() / 2f) * values[Matrix.MSCALE_Y] * (tempY - 1);

//        PointF centerPointF=DeponderHelper.centerPointF(po);
//
        matrix.postScale(tempX, tempY, (rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
//        matrix.postScale(tempX, tempY, centerPointF.x, centerPointF.y);
//        matrix.postTranslate(p.x + values[Matrix.MTRANS_X]*tempX, p.y + values[Matrix.MTRANS_Y]*tempY);
        matrix.postTranslate(p.x, p.y);
//
        po.matrix().postConcat(matrix);


//        po.matrix().postTranslate(p.x, p.y);
        Log.i("NAnimator", "回调中的hashCode:" + po.matrix().hashCode());
        //记录位移末速度.
        po.speed().offset(po.acceleration().x * interval, po.acceleration().y * interval);
        po.acceleration().set(0, 0);
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

}
