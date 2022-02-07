package com.cinpe.deponder;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.MyRootOption;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;

import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.MainThreadDisposable;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;
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
public abstract class DeponderProxy<PO extends PlanetOption, RO extends RubberOption> implements DeponderControl<PO, RO> {

    public static final String TAG = "DeponderProxy";

    private final RootOption rootOption;

    private final Map<String, PO> poMap;
    private final Map<String, RO> roMap;

    Flowable<Timed<Transformation>> flowable;

//    /**
//     * 相互作用力的缓存
//     */
//    private Map<String, Newton> newtonMap = new HashMap<>();

    BindAdapter adapter;

    float mScale = 1f;

//    @NonNull
//    private final DiffUtil.ItemCallback<T> mDiffCallback;


    public DeponderProxy(@NonNull ViewGroup rootView) {

        this(MyRootOption.builder()
                .initScale(DeponderHelper.DEFAULT_INIT_SCALE)
                .maxScale(DeponderHelper.DEFAULT_MAX_SCALE)
                .minScale(DeponderHelper.DEFAULT_MIN_SCALE)
                .mRootDensity(DeponderHelper.DEFAULT_ROOT_DENSITY)
                .id(UUID.randomUUID().toString())
                .itemView(rootView)
                .build());


    }

    public DeponderProxy(@NonNull RootOption rootOption) {

        this.rootOption = rootOption;

        this.poMap = new HashMap<>();
        this.roMap = new HashMap<>();

        init();


    }


    private void init() {
        if (this.flowable == null) {
            DeponderHelper.bindDelegateRootTouch(this.rootOption, this.poMap.values());
            bindRootMotion(this.rootOption);
            this.flowable.subscribe(new DefSubscriber<>());
        }
    }

    @Override
    public void submit(@NonNull Collection<View> pList) {
        submit(pList, null);
    }

    @Override
    public void submit(@NonNull Collection<View> pList, @Nullable Collection<RO> rList) {
        submit(pList, rList, 1f);
    }

    @Override
    public void submit(@NonNull Collection<View> pList, @Nullable Collection<RO> rList, float scale) {
        this.mScale = scale;
        //todo 比对差异.

        //移除原来所有动画.
        cancel();

        submitPlanet(pList);
        if (rList != null) {
            submitRubber(rList);
        }

        start();
    }

    /**
     * 提交P
     */
    private void submitPlanet(@NonNull Collection<View> pList) {

        //todo 比对差异.

        //更新map.
        poMap.putAll(pList.stream().map(this::functionP).collect(Collectors.<PO, String, PO>toMap(BaseOption::id, p -> p)));

    }

//    /**
//     * 提交R
//     */
//    private void submitRubber(Collection<View> rList) {
//        roMap.putAll(rList.stream().map(this::functionR).collect(Collectors.<RO, String, RO>toMap(BaseOption::id, ro -> ro)));
//        roMap.values().forEach(ro -> rootOption.itemView().addView(ro.itemView()));
//    }

    /**
     * 提交R
     */
    private void submitRubber(Collection<RO> rList) {
        roMap.putAll(rList.stream().collect(Collectors.<RO, String, RO>toMap(ro -> ro.id() + ro.eId(), ro -> ro)));
        roMap.values().forEach(ro -> rootOption.itemView().addView(ro.itemView()));
    }

    protected abstract PO functionP(View p);

//    protected abstract RO functionR(View r);

    /**
     * startAll
     */
    private void start() {

        rootOption.animator().start();
        poMap.values().forEach(po -> {
            po.itemView().setAnimation(po.animator());
            DeponderHelper.bindPlanet(po);
            po.itemView().post(() -> {
                Rect rect = new Rect();
                po.itemView().getHitRect(rect);
//                mX = rect.centerX();
//                mY = rect.centerY();
                float mX = rect.width() >> 1;
                float mY = rect.height() >> 1;
                Log.i(TAG, "缩放中心点:" + mX + "," + mY + ",rect:" + rect + ",matrix:" + po.matrix());
                po.matrix().postScale(this.mScale, this.mScale, mX, mY);

                RectF rectF = new RectF(rect);
                RectF dst = new RectF();
                po.matrix().mapRect(dst, rectF);

                Log.i(TAG, "缩放中心点后:" + "rectF:" + rectF + ",det:" + dst + ",matrix:" + po.matrix());
            });
        });
        roMap.values().forEach(ro -> ro.itemView().setAnimation(ro.animator()));
        this.rootOption.itemView().setAnimation(this.rootOption.animator());

    }


    /**
     * pauseAll
     */
    private void pause() {
        poMap.values().parallelStream().forEach(po -> po.animator().cancel());
        roMap.values().parallelStream().forEach(ro -> ro.animator().cancel());
    }

    /**
     * cancelAll
     */
    public void cancel() {
        pause();
        poMap.values().parallelStream().forEach(po -> po.itemView().setAnimation(null));
        roMap.values().parallelStream().forEach(ro -> ro.itemView().setAnimation(null));
        poMap.clear();
        roMap.clear();
    }


    static class Newton {

        public Newton(@NonNull PlanetOption sPlanet, @NonNull PlanetOption ePlanet, @Nullable RubberOption rubberOption) {
            this.sPlanet = sPlanet;
            this.ePlanet = ePlanet;
            this.rubber = rubberOption;
            this.vector = new Matrix();
            evaluate();
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
                final float natural = rubber.naturalLength() * values[Matrix.MSCALE_X];

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

            final float mInternalPressure = sPlanet.mInternalPressure() * values[Matrix.MSCALE_X];
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
        final PlanetOption sPlanet;

        /**
         * ePlanet
         */
        @NonNull
        final PlanetOption ePlanet;

        @Nullable
        final RubberOption rubber;

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
     * 绑定rootOption的动画.
     */
    private void bindRootMotion(@NonNull RootOption option) {

//        poMap.values().ea
//
//        Flowable.<Transformation>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
//                .subscribeOn(Schedulers.io())
//                .concatMapIterable(t->poMap.values())
//                .map(po -> Pair.create(po,poMap.values()))


        this.flowable = Flowable.<Transformation>create(emitter -> option.animator().setApplyTransformationListener(new Listener(option.animator(), emitter)), BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .timeInterval()
                .doOnNext(transformationTimed -> {

                    final long interval = Math.min(transformationTimed.time(), 16L);

                    //todo 先完成上一帧的绘制.
                    Log.i(TAG, "先完成上一帧的绘制() called with: interval = [" + interval + "]");

                    poMap.values().forEach(po -> {

                        //摩擦损耗.
//                        po.speed().offset(-po.speed().x * rootOption.mRootDensity() * interval, -po.speed().y * rootOption.mRootDensity() * interval);
                        po.acceleration().offset(-po.speed().x * rootOption.mRootDensity(), -po.speed().y * rootOption.mRootDensity());

                        //进行位移.
                        PointF p = DeponderHelper.calculate(po.speed(), po.acceleration(), interval);
                        Log.i(TAG, "po[速度]=" + po.speed() + ",[加速度]=" + po.acceleration() + ",[间隔时间]=" + interval + ",[位移]=" + p);
                        po.matrix().postTranslate(p.x, p.y);
                        //记录位移末速度.
                        po.speed().offset(po.acceleration().x * interval, po.acceleration().y * interval);
                        po.acceleration().set(0, 0);
                    });

                    roMap.values().forEach(ro -> {
                        //todo ro去哪里记录,角度,自然长度.


                    });


                })
//                .map(t->{
//                    //root的参数.
//                    Rect rootRect = new Rect();
//                    rootOption.itemView().getHitRect(rootRect);
//                    rootOption.rectF().set(rootRect);
//                    rootOption.matrix().mapRect(rootOption.rectF());
//                    rootOption.rectF().offsetTo(-mX, -mY);
//                    return rootOption.rectF();
//                })


                //计算加速度.
                .doOnNext(transformationTimed -> {

                    //临时容器.
                    final Map<String, Newton> newtonMap = new HashMap<>();

                    //root的参数.
                    Rect rootRect = new Rect();
                    rootOption.itemView().getHitRect(rootRect);
                    rootOption.rectF().set(rootRect);
                    rootOption.matrix().mapRect(rootOption.rectF());
                    Log.i(TAG, "步骤一,大矩形:" + rootRect + ",大矩阵:" + rootOption.matrix() + ",大精确矩形:" + rootOption.rectF());

                    Log.i(TAG, "完成当前帧的计算() rootRect = [" + rootRect + "]");

                    poMap.values().forEach(po -> {
                        //todo 完成当前帧的计算.

                        if (po.itemView().isPressed()) {
                            po.speed().set(0, 0);
                        } else {

                            //先获取到root的[四个边距]的[有效]矢量和.
                            //正方向取力的方向.
                            PointF wallDistance = new PointF();

//                            //planet之间力的矢量和.
//                            PointF planetDistance = new PointF();

                            //todo rubber之间[有效]距离的矢量和.


                            //总的加速度.
                            PointF acceleration = new PointF();


                            //先获取当前的矩形.
                            Rect rect = new Rect();
                            po.itemView().getHitRect(rect);

                            float[] floats = new float[9];
                            po.matrix().getValues(floats);
                            PointF centerPoint = new PointF(rect.centerX() + floats[Matrix.MTRANS_X], rect.centerY() + floats[Matrix.MTRANS_Y]);

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
                            acceleration.set(DeponderHelper.calculate(wallDistance, rootOption.elasticityCoefficient(), po.quality()));

                            Log.d(TAG, "步骤一() 加速度 = [" + acceleration + "]" + ",边界距离向量:" + wallDistance + ",大矩形:" + rootOption.rectF() + ",小矩形:" + po.rectF() + ",感应范围:" + rootOption.mInternalPressure());


                            //step#2
                            //获取到其它planet的矢量和.
                            Matrix planetMatrix = new Matrix();

                            poMap.values().stream().filter(otherPo -> !TextUtils.equals(po.id(), otherPo.id()))
                                    .forEach(otherPo -> {

                                        final String mId = po.id() + otherPo.id();
                                        final String invertId = otherPo.id() + po.id();
                                        Newton obj = newtonMap.get(invertId);

                                        Matrix matrix = new Matrix();
                                        if (obj == null) {

                                            obj = newtonMap.get(mId);

                                            if (obj == null) {
                                                //检查是否有rubber.
                                                RO ro = roMap.get(mId);
                                                if (ro == null) ro = roMap.get(invertId);

                                                obj = new Newton(po, otherPo, ro);
                                                newtonMap.put(mId, obj);
                                            }

                                            matrix.set(obj.vector);

                                        } else {

                                            obj.vector.invert(matrix);

                                        }

                                        planetMatrix.postConcat(matrix);

                                    });


                            if (!planetMatrix.isIdentity())
                                Log.d(TAG, "矩阵积() [matrix] = [" + planetMatrix + "]");

                            //todo 处理累计的rubber造成的影响.

                            float[] values = new float[9];
                            planetMatrix.getValues(values);

//                            planetDistance.set(-values[Matrix.MTRANS_X], -values[Matrix.MTRANS_Y]);

                            //planet当前加速度.
                            PointF planetAcceleration = new PointF(-values[Matrix.MTRANS_X] / po.quality(), -values[Matrix.MTRANS_Y] / po.quality());

                            Log.d(TAG, "步骤二() 加速度 = [" + planetAcceleration + "]" + ",步骤一:" + po.quality() + ",边界距离向量:" + wallDistance);

                            //记录planet之间产生的加速度.
                            acceleration.offset(planetAcceleration.x, planetAcceleration.y);


                            //step#3
                            //todo 开始计算rubber的合力.


                            //step#4
                            //记录. 加速度.
                            po.acceleration().set(acceleration);
                        }


                    });
                })
                .doOnError(throwable -> Log.d(TAG, "doOnError() called with: throwable = [" + throwable + "]"))
                .doOnComplete(() -> Log.d(TAG, "doOnComplete() 自动完成了"));

    }

}
