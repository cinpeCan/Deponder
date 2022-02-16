package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.icu.text.Edits;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import androidx.annotation.BinderThread;
import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.MyRootOption;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.FlowableOperator;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Function3;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.Timed;

/**
 * @Description: T 唯一标识接口.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class Deponder<PO extends PlanetOption, RO extends RubberOption> {

    public static final String TAG = "Deponder";

    private final RootOption rootOption;

    final LifecycleOwner owner;

    final MutableLiveData<Collection<PO>> poClt = new MutableLiveData<>(Collections.emptyList());
    final MutableLiveData<Collection<RO>> roClt = new MutableLiveData<>(Collections.emptyList());
    final MutableLiveData<Float> scaleLD = new MutableLiveData<>(1f);

//    @NonNull
//    private final DiffUtil.ItemCallback<T> mDiffCallback;


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
        pre(this.rootOption);
    }

    public void submit(@NonNull Collection<View> pList) {
        submit(pList, null);
    }

    public void submit(@NonNull Collection<View> pList, @Nullable Collection<RO> rList) {
        submit(pList, rList, 1f);
    }

    public void submit(@NonNull Collection<View> pList, @Nullable Collection<RO> rList, float scale) {
        this.scaleLD.postValue(scale);
        //todo 比对差异.

        submitPlanet(pList.stream().map(this::functionP).collect(Collectors.toList()));
        submitRubber(rList);


    }

    /**
     * 提交P
     */
    private void submitPlanet(@NonNull Collection<PO> pList) {

        //todo 比对差异.

        //todo 移除之前所有动画绑定.

        poClt.setValue(pList);

        //todo view添加到rootView.
    }

    /**
     * 提交R
     */
    private void submitRubber(Collection<RO> rList) {

        //todo 检查差异?

        //todo 移除之前所有动画绑定.

        roClt.setValue(rList);

        //todo view添加到rootView.

    }

    protected abstract PO functionP(View p);

//    protected abstract RO functionR(View r);

    /**
     * startAll
     */
    private void start() {

        Log.i(TAG, "start");
        this.rootOption.itemView().startAnimation(this.rootOption.animator());

    }


    /**
     * pauseAll
     */
    private void pause() {

        //todo 怎么pause.

//        poMap.values().parallelStream().forEach(po -> po.animator().cancel());
//        roMap.values().parallelStream().forEach(ro -> ro.animator().cancel());
    }

    /**
     * cancelAll
     */
    public void cancel() {
        pause();
        poClt.postValue(null);
        roClt.postValue(null);

//        poMap.values().parallelStream().forEach(po -> po.itemView().setAnimation(null));
//        roMap.values().parallelStream().forEach(ro -> ro.itemView().setAnimation(null));
//        poMap.clear();
//        roMap.clear();
    }


    static class Newton<PO extends PlanetOption, RO extends RubberOption> {

        public Newton(@NonNull PO sPlanet, @NonNull PO ePlanet, @Nullable RO rubberOption) {
            this.sPlanet = sPlanet;
            this.ePlanet = ePlanet;
            this.rubber = rubberOption;
            this.vector = new Matrix();
//            evaluate();
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


    class Listener extends MainThreadDisposable implements NAnimator.ApplyTransformationListener {

        NAnimator nAnimator;
        //        Observer<Float> observer;
        FlowableEmitter<Transformation> emitter;

        public Listener(NAnimator nAnimator, FlowableEmitter<Transformation> emitter) {
            this.nAnimator = nAnimator;
            this.emitter = emitter;
        }

        @Override
        public void onApplyTransformation(Transformation t) {
            emitter.onNext(t);
        }

        @Override
        protected void onDispose() {
            nAnimator.setApplyTransformationListener(null);
        }
    }


    /**
     * 测试的内容.
     */
//    Flowable<Long> longFlowable;
    private void pre(@NonNull RootOption option) {


        //用于 用于绘制po.
        Flowable<Collection<PO>> poCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, poClt))
                .defaultIfEmpty(Collections.emptyList())
                .doOnSubscribe(s -> start())
                .doOnNext(p -> Log.i(TAG, "发射po集合" + p))
                .doOnNext(clt -> {
                    if (!clt.isEmpty()) {
                        Log.i(TAG, "设置touch:" + clt);
                        DeponderHelper.bindDelegateRootTouch(this.rootOption, clt);
//                        clt.forEach(po -> {
//                            po.itemView().setAnimation(po.animator());
//                            DeponderHelper.bindPlanet(po);
//
//                            po.matrix().postTranslate(300, 300);
//                        });

                    }
                });
//                .concatMapIterable(iterable -> iterable);

        //用于 用于绘制ro.
        Flowable<Collection<RO>> roCltFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, roClt))
                .defaultIfEmpty(Collections.emptyList())
                .doOnNext(p -> Log.i(TAG, "发射ro集合"));

        //scale
        Flowable<Float> scaleFlowable = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(owner, scaleLD)).doOnNext(p -> Log.i(TAG, "发射缩放" + p));

        /**
         * 帧回调.
         */
        Flowable<Timed<Transformation>> timer = Flowable.<Transformation>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
                .timeInterval()
//                .skipWhile(t -> t.time() > 16)
//                .map(Timed::time)
                .doOnNext(l -> Log.i(TAG, "间隔时间:" + l));

        //用于 evaluate.
        //用于 绘制ro.
        Flowable<? extends Iterable<Newton<PO, RO>>> newtonFlowable = Flowable.combineLatest(
                poCltFlowable,
                roCltFlowable.defaultIfEmpty(Collections.emptyList()),
                Pair::create
        ).debounce(50, TimeUnit.MILLISECONDS)
                .doOnNext(pair -> Log.i(TAG, "发射Pair"))
                .concatMap(pair ->
                        {
                            Log.i(TAG, "实际发射Pair" + pair);

                            final Map<String, RO> stringROMap = Observable.fromIterable(pair.second)
                                    .doOnNext(ro -> {
                                        int i = rootOption.itemView().indexOfChild(ro.itemView());
                                        if (i < 0) {
                                            rootOption.itemView().addView(ro.itemView());
                                            ro.itemView().startAnimation(ro.animator());
                                        }
                                    })
                                    .doOnNext(ro -> Log.i(TAG, "发射RO"))
                                    //设置ro的animal.
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .doOnNext(ro -> Log.i(TAG, "设置完ro动画"))
                                    .toMap(ro -> ro.id() + ro.eId(), ro -> ro)
                                    .blockingGet();

                            Log.i(TAG, "完成了map" + stringROMap);

                            return Flowable.fromIterable(pair.first)
                                    .compose(Upstream -> Upstream
                                            //设置PO动画
                                            .doOnNext(po -> {
                                                if(po.itemView().getAnimation() instanceof NAnimator){

                                                }else {
                                                    Log.i(TAG, "绑定动画和触摸");
                                                    po.itemView().startAnimation(po.animator());
                                                    DeponderHelper.bindPlanet(po);
                                                }
                                            })
                                            //设置PO触摸
                                            .subscribeOn(AndroidSchedulers.mainThread())
                                            .observeOn(Schedulers.io())
                                            //转newton.
                                            .flatMap(new Function<PO, Publisher<PO>>() {
                                                final AtomicInteger index = new AtomicInteger();

                                                @Override
                                                public Publisher<PO> apply(PO po) throws Throwable {
                                                    return Upstream.skip(index.incrementAndGet()).defaultIfEmpty(po);
                                                }
                                            }, new BiFunction<PO, PO, Newton<PO, RO>>() {
                                                @Override
                                                public Newton<PO, RO> apply(PO s, PO e) throws Throwable {

                                                    if (TextUtils.equals(s.id(), e.id())) {
                                                        Log.i(TAG, "出现了!相同怪!");

                                                        return new Newton<>(s, e, null);
                                                    } else {
                                                        return stringROMap
                                                                .containsKey(s.id() + e.id()) ? new Newton<>(s, e, stringROMap.get(s.id() + e.id())) : new Newton<>(e, s, stringROMap.get(e.id() + s.id()));

                                                    }

                                                }
                                            })


                                    )
                                    .doOnNext(n -> Log.i(TAG, "发射了牛顿" + n))
                                    .toList()
                                    .toFlowable()
//                                    .blockingGet()
                                    ;

                        }

                )
                .doOnNext(n -> Log.i(TAG, "发射牛顿"));


        timer.withLatestFrom(newtonFlowable, new BiFunction<Timed<Transformation>, Iterable<Newton<PO, RO>>, Iterable<PO>>() {
            @Override
            public Iterable<PO> apply(Timed<Transformation> timed, Iterable<Newton<PO, RO>> newtons) throws Throwable {

                return Flowable.fromIterable(newtons)
                        .compose(upstream -> upstream
                                .doOnNext(n -> {
                                    PointF f = evaluate(n.sPlanet, n.ePlanet, n.rubber);
                                    n.sPlanet.acceleration().offset(-f.x / n.sPlanet.quality(), -f.y / n.sPlanet.quality());
                                    n.ePlanet.acceleration().offset(f.x / n.ePlanet.quality(), f.y / n.ePlanet.quality());
                                })
                                //绘出ro.
                                .doAfterNext(n -> drawRo(n.rubber, n.sPlanet, n.ePlanet))
                                //转换.
                                .scan(new HashMap<String, PO>(), new BiFunction<HashMap<String, PO>, Newton<PO, RO>, HashMap<String, PO>>() {
                                    @Override
                                    public HashMap<String, PO> apply(HashMap<String, PO> stringPOHashMap, Newton<PO, RO> newton) throws Throwable {
                                        //分析相互作用力.
                                        stringPOHashMap.putIfAbsent(newton.sPlanet.id(), newton.sPlanet);
                                        stringPOHashMap.putIfAbsent(newton.ePlanet.id(), newton.ePlanet);
                                        return stringPOHashMap;
                                    }
                                })
                                .flatMapIterable(m -> m.values())
                                //分析四边.
                                .doOnNext(po -> {
                                    PointF newtonRoot = evaluateRootInternalPressure(po);
                                    po.acceleration().offset(newtonRoot.x / po.quality(), newtonRoot.y / po.quality());
                                })
                                //绘出po.
                                .doOnNext(po -> drawPo(po, timed.time()))
                        )
                        .toList()
                        .blockingGet();
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe(new DefSubscriber<>());
    }

    public void submitCte(@NonNull Collection<PO> pList, @NonNull Collection<RO> rList, float scale) {


    }


    /**
     * 计算四边对po的压力.(而不是加速度)
     */
    @WorkerThread
    protected @NonNull
    PointF evaluateRootInternalPressure(@NonNull PO po) {

        //先获取到root的[四个边距]的[有效]矢量和.
        //正方向取力的方向.
        final PointF wallDistance = new PointF();

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
            final float mScale = floats[Matrix.MSCALE_X];

            Log.i(TAG, "步骤一,小矩形:" + rect + ",小矩阵:" + po.matrix() + ",中心点:" + centerPoint + ",精确rectF:" + po.rectF());

            //分析四边.
            float l = centerPoint.x;
            float t = centerPoint.y;
            float r = rootOption.rectF().width() - centerPoint.x;
            float b = rootOption.rectF().height() - centerPoint.y;

            //四边产生的力.
            final float internalPressure = rootOption.mInternalPressure() * mScale;
            l = l < internalPressure ? internalPressure - l : 0;
            t = t < internalPressure ? internalPressure - t : 0;
            r = r < internalPressure ? internalPressure - r : 0;
            b = b < internalPressure ? internalPressure - b : 0;

            wallDistance.x = l - r;//向右
            wallDistance.y = t - b;//向下

            //step#1
            wallDistance.set(DeponderHelper.calculate(wallDistance, rootOption.elasticityCoefficient()));

            Log.d(TAG, "步骤一() 加速度 = [" + wallDistance + "]" + ",边界距离向量:" + wallDistance + ",大矩形:" + rootOption.rectF() + ",小矩形:" + po.rectF() + ",感应范围:" + rootOption.mInternalPressure());

        }

        return wallDistance;
    }

    /**
     * 评估.
     */
    @WorkerThread
    protected PointF evaluate(@NonNull PO s, @NonNull PO e, @Nullable RO r) {


        //最终的力.
        final PointF newton = new PointF();

        if (TextUtils.equals(s.id(), e.id())) return newton;

//            final Matrix sMatrix = this.sPlanet.matrix();
//            final Matrix eMatrix = this.ePlanet.matrix();

//            final Matrix invert = new Matrix();
//            sMatrix.invert(invert);

        //获取矩阵差.
        Matrix matrix = DeponderHelper.matrixDiff(s.matrix(), e.matrix());
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

        if (r != null) {

            //弹簧的自然长度.
            final float natural = r.naturalLength() * mScale;

            //计算弹簧的形变量.
            float rubberDiff = DeponderHelper.dDistance(radius, natural);

            PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));
            Log.i(TAG, "计算弹簧的形变量:" + rubberDiffDistance);

            //计算弹簧的力.
            PointF rubberNewton = DeponderHelper.calculate(rubberDiffDistance, r.elasticityCoefficient());

            //记录弹簧的力.
            newton.offset(rubberNewton.x, rubberNewton.y);

        }


        Log.d(TAG, "评估() called[半径]=" + radius + ",[矩阵]=" + newton);

        final float mInternalPressure = s.mInternalPressure() * mScale;
        if (Math.abs(radius) > mInternalPressure) {
            //超出感应范围->重置.
//                this.vector.reset();
        } else {
            float planetDiff = DeponderHelper.dDistance(radius, mInternalPressure);

            //感应范围内, 距离->形变
            PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));

            //感应范围内, 形变->力
            PointF planetDiffNewton = DeponderHelper.calculate(planetDiffDistance, s.elasticityCoefficient());
            newton.offset(planetDiffNewton.x, planetDiffNewton.y);
        }

        return newton;
    }


    /**
     * 绘制po.
     */
    @WorkerThread
    protected void drawPo(@NonNull PO po, long interval) {

        if (po.itemView().isPressed()) {
            return;
        }

        //摩擦损耗.
//                        po.speed().offset(-po.speed().x * rootOption.mRootDensity() * interval, -po.speed().y * rootOption.mRootDensity() * interval);
        po.acceleration().offset(-po.speed().x * rootOption.mRootDensity(), -po.speed().y * rootOption.mRootDensity());

        //进行位移.
        PointF p = DeponderHelper.calculate(po.speed(), po.acceleration(), interval);
        Log.i(TAG, "po[速度]=" + po.speed() + ",[加速度]=" + po.acceleration() + ",[间隔时间]=" + interval + ",[位移]=" + p);
        po.matrix().postTranslate(p.x, p.y);
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
    protected void drawRo(@Nullable RO ro, @NonNull PO sPo, @NonNull PO ePo) {

        if (ro == null) return;

        final Rect sRect = DeponderHelper.hitRect(sPo.itemView());
        final Rect eRect = DeponderHelper.hitRect(ePo.itemView());
        final Rect rRect = DeponderHelper.hitRect(ro.itemView());
        final float[] sValues = DeponderHelper.values(sPo.matrix());
        final float[] eValues = DeponderHelper.values(ePo.matrix());
        final float[] rValues = DeponderHelper.values(ro.matrix());
        final PointF sP = new PointF(sRect.centerX() + sValues[Matrix.MTRANS_X], sRect.centerY() + sValues[Matrix.MTRANS_Y]);
        final PointF eP = new PointF(eRect.centerX() + eValues[Matrix.MTRANS_X], eRect.centerY() + eValues[Matrix.MTRANS_Y]);
        final PointF rP = new PointF(rRect.centerX() + rValues[Matrix.MTRANS_X], rRect.centerY() + rValues[Matrix.MTRANS_Y]);
        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);
        //当前中点.
        final PointF center = DeponderHelper.centerPointF(sP, eP);
        //当前长度.
        final float length = PointF.length(diff.x, diff.y);
        //当前角度.
        final double angle = Math.atan2(diff.x, diff.y);

        final Matrix d = new Matrix();

        //设置长度.
        d.postScale(length / rRect.width(), sValues[Matrix.MSCALE_X], rRect.centerX(), rRect.centerY());
        //设置位移.
        d.postTranslate(center.x - rRect.centerX(), center.y - rRect.centerY());
        //设置角度.
        d.postRotate(Math.round(90 - angle / Math.PI * 180), center.x, center.y);

        ro.matrix().set(d);

        Log.i(TAG, "drawRo");
    }

}
