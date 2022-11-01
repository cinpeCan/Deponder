package com.cinpe.deponder;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.SimpleRootOption;
import com.google.common.base.Equivalence;

import org.reactivestreams.Publisher;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.functions.BiFunction;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/5
 * @Version: 0.01
 */
public class DeponderHelper {

    public static final String TAG = "DeponderHelper";

    /**
     * 空间初始化缩放值(当行星过多或过少时,可能进行缩放)
     */
    public final static float DEFAULT_INIT_SCALE = 1f;

    /**
     * 空间最大缩放值(在初始化缩放值基础上.)
     */
    public final static float DEFAULT_MAX_SCALE = 1.5f;

    /**
     * 空间最小缩放值(在初始化缩放值基础上.)
     */
    public final static float DEFAULT_MIN_SCALE = .5f;

    /**
     * 空间中以太密度.
     */
    public final static float DEFAULT_ROOT_DENSITY = 0.0006f;

    /**
     * 默认planet质量
     */
    public final static float DEFAULT_QUALITY_PROPERTY = 2.293f;

    /**
     * root的弹性系数.
     */
//    public final static float DEFAULT_ROOT_ELASTICITY_COEFFICIENT = 1.44f;

    public final static float DEFAULT_Internal_Pressure_START = 1.44f;
    public final static float DEFAULT_Internal_Pressure_TOP = 1.44f;
    public final static float DEFAULT_Internal_Pressure_END = 1.44f;
    public final static float DEFAULT_Internal_Pressure_BOTTOM = 1.44f;

    /**
     * planet的弹性系数.
     */
    public final static float DEFAULT_ELASTICITY_COEFFICIENT = 1.33f;

    /**
     * rubber的弹性系数.
     */
    public final static float DEFAULT_RUBBER_ELASTICITY_COEFFICIENT = 1.68f;

    /**
     * 四壁内压的感应距离.(px)
     */
    public final static int DEFAULT_Internal_Pressure = 300;


    /**
     * planet的感应距离.(px)
     */
    public final static int DEFAULT_PLANET_INTERNAL_PRESSURE = 220;

    /**
     * rubber的自然长度.(px)
     */
    public final static int DEFAULT_RUBBER_NATURAL_LENGTH = 300;

    /**
     * distance阈值.(高倍放大时防抖,低于阈值不进行绘制)
     */
    public final static float MIN_ACCELERATION = 0.01f;

    /**
     * 连接
     */
    public final static float INCREMENTAL_SCALE = 0.15f;

    /**
     * rubber的tag.
     */
    public final static String TAG_OF_UN_RUBBER = "UN_RUBBER_RUBBER";

    public final static Equivalence<Object> equivalence = new Equivalence<Object>() {
        @Override
        protected boolean doEquivalent(@NonNull Object a, @NonNull Object b) {
            return Objects.equals(a, b);
        }

        @Override
        protected int doHash(@NonNull Object o) {
            return o.hashCode();
        }
    };

    /**
     * dp->px
     */
    protected static @Px
    int dp2px(@NonNull Context context, @Dimension int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * hitRect
     */
    protected static @NonNull
    Rect hitRect(@NonNull View v) {
        Rect rect = new Rect();
        v.getHitRect(rect);
        return rect;
    }

    /**
     * hitRectF
     */
    protected static @NonNull
    RectF hitRectF(@NonNull View v) {
        Rect rect = new Rect();
        v.getHitRect(rect);
        return new RectF(rect);
    }

    /**
     * hitRectF
     */
    protected static @NonNull
    RectF planetCurrentHitRectF(@NonNull View v) {
        NAnimator animation = (NAnimator) v.getAnimation();
        RectF rectF = hitRectF(v);
        Matrix temp = new Matrix();
        float[] floats = DeponderHelper.values(animation.getMMatrix());
        temp.postScale(floats[Matrix.MSCALE_X], floats[Matrix.MSCALE_Y], rectF.width() * .5f, rectF.height() * .5f);
        temp.mapRect(rectF);
        rectF.offset(floats[Matrix.MTRANS_X], floats[Matrix.MTRANS_Y]);
        return rectF;
    }

    /**
     * getValues
     */
    protected static @NonNull
    float[] values(@NonNull Matrix matrix) {
        float[] floats = new float[9];
        matrix.getValues(floats);
        return floats;
    }

    /**
     * centerPointF
     */
    protected static @NonNull
    PointF centerPointF(PointF s, PointF e) {
        return new PointF((s.x + e.x) / 2, (s.y + e.y) / 2);
    }

    /**
     * centerPointF
     */
    protected static @NonNull
    PointF centerPointF(BaseOption p) {
        RectF src = DeponderHelper.hitRectF(p.itemView());
        float[] values = values(p.matrix());
        float dw = src.width() * (values[Matrix.MSCALE_X] - 1) * 0.5f;
        float dh = src.height() * (values[Matrix.MSCALE_Y] - 1) * 0.5f;
        return new PointF(src.centerX() + values[Matrix.MTRANS_X] + dw, src.centerY() + values[Matrix.MTRANS_Y] + dh);
    }


    protected static @NonNull
    PointF centerPointFInternal(BaseOption p) {
        Rect rect = DeponderHelper.hitRect(p.itemView());
        float[] values = new float[9];
        p.matrix().getValues(values);
        return new PointF((rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
    }

    /**
     * 获取矩阵差.
     */
    protected static Matrix matrixDiff(Matrix sMatrix, Matrix eMatrix) {
        final Matrix invert = new Matrix();
        sMatrix.invert(invert);
        Matrix matrix = new Matrix();
        boolean concat = matrix.setConcat(invert, eMatrix);
        if (concat) {
            return matrix;
        } else {
            throw new IllegalStateException("IllegalState Matrix: [sMatrix] = " + sMatrix + " [eMatrix] = " + eMatrix);
        }

    }

    /**
     * 距离->形变距离.
     * 斥力方向为正
     *
     * @param distance 实际距离.
     */
    protected static float dDistance(float distance, @FloatRange(from = 0) float internalPressure) {
        if (distance == 0) {
            //距离为0, 没有力的方向. 合力为0.
            return 0;
        } else if (distance > 0) {
            return internalPressure - distance;
        } else {
            return -internalPressure - distance;
        }
    }

    /**
     * 形变量->计算力
     *
     * @param difDistance 形变距离.
     */
    protected static float calculate(float difDistance, float elasticity_coefficient) {
        return difDistance * elasticity_coefficient;
    }

    protected static PointF calculate(@NonNull PointF difDistance, float elasticity_coefficient) {
        return new PointF(calculate(difDistance.x, elasticity_coefficient), calculate(difDistance.y, elasticity_coefficient));
    }

    /**
     * 距离计算加速度.
     *
     * @param difDistance 形变距离.
     */
    protected static PointF calculate(@NonNull PointF difDistance, float elasticity_coefficient, float m) {
        final PointF newton = calculate(difDistance, elasticity_coefficient);
        return new PointF(newton.x / m, newton.y / m);
    }


    /**
     * 计算位移(矢量),
     *
     * @param speed        速度(px/ms)
     * @param acceleration 加速度(px/ms2)
     * @param dt           时间(ms)
     */
    protected static PointF calculate(PointF speed, PointF acceleration, long dt) {
        float x = Math.round(speed.x * dt + .5f * acceleration.x * Math.pow(dt, 2)) / 1000000f;
        float y = Math.round(speed.y * dt + .5f * acceleration.y * Math.pow(dt, 2)) / 1000000f;
        return new PointF(x, y);
    }


    protected static void bindDelegateRootTouch(@NonNull SimpleRootOption rootOption) {
        rootOption.itemView().post(() -> rootOption.itemView().setTouchDelegate(new DeponderDelegate(rootOption)));
    }

    public static void bindDefTouchPlanet(PlanetOption option) {
        option.itemView().setOnTouchListener(new TouchHelper(option));
    }

    /**
     * planet TouchHelper
     */
    public static class TouchHelper extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

        public TouchHelper(@NonNull PlanetOption option) {
            this.option = option;
            mGestureDetector = new GestureDetector(option.itemView().getContext(), this);
            mScaleGestureDetector = new ScaleGestureDetector(option.itemView().getContext(), this);
        }

        private final PlanetOption option;
        private final GestureDetector mGestureDetector;
        private final ScaleGestureDetector mScaleGestureDetector;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (!v.isEnabled()) {
                return false;
            }

            boolean retVal = mScaleGestureDetector.onTouchEvent(event);
            retVal = mGestureDetector.onTouchEvent(event) || retVal;

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                option.itemView().setPressed(false);

            return retVal;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            this.option.matrix().postScale(detector.getScaleFactor(),
                    detector.getScaleFactor(),
                    detector.getFocusX(),
                    detector.getFocusY());

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //null
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i(TAG, "[planet]onSingleTapUp() called with: e = [" + e + "]");
            return this.option.itemView().performClick();
        }


        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (e1 != null) this.option.matrix().postTranslate(-distanceX, -distanceY);

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            this.option.itemView().setPressed(true);
            this.option.speed().reset();
            return true;
        }


    }

    /**
     * root TouchHelper
     */
    static class RootTouchHelper extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

        public RootTouchHelper(SimpleRootOption option) {
            this.option = option;
            mGestureDetector = new GestureDetector(option.itemView().getContext(), this);
            mScaleGestureDetector = new ScaleGestureDetector(option.itemView().getContext(), this);
        }

        private final SimpleRootOption option;

        private final GestureDetector mGestureDetector;
        private final ScaleGestureDetector mScaleGestureDetector;

        @Override
        public boolean onTouch(View v, MotionEvent event) {


            if (!v.isEnabled()) {
                return false;
            }

            boolean retVal = mScaleGestureDetector.onTouchEvent(event);
            retVal = mGestureDetector.onTouchEvent(event) || retVal;

            return retVal;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            option.matrix().postScale(detector.getScaleFactor(),
                    detector.getScaleFactor(),
                    detector.getFocusX(),
                    detector.getFocusY());

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
//            null
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return this.option.itemView().performClick();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            this.option.matrix().postTranslate(-distanceX, -distanceY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    protected static <O> CombinationsPair<O, Pair<O, O>> combinationsPair() {
        return new CombinationsPair<O, Pair<O, O>>() {
            @NonNull
            @Override
            protected BiFunction<O, O, Pair<O, O>> biFunction() {
                return Pair::create;
            }
        };
    }

    protected static <O, P> FlowableCombinationsPair<O, P> flowableCombinationsPair(@NonNull BiFunction<O, O, P> biFunction) {
        return new FlowableCombinationsPair<O, P>() {
            @NonNull
            @Override
            protected BiFunction<O, O, P> biFunction() {
                return biFunction;
            }
        };
    }

    protected static <O, P> FlowableTransformer<O, P> flowableCombinations(@NonNull FlowableTransformer<O, O> UpstreamTransformer, @NonNull BiFunction<O, O, P> biFunction) {
        return new FlowableCombinationsPair<O, P>() {
            @NonNull
            @Override
            protected BiFunction<O, O, P> biFunction() {
                return biFunction;
            }

            @NonNull
            @Override
            protected FlowableTransformer<O, O> compose() {
                return UpstreamTransformer;
            }
        };
    }

    protected static <O, P> CombinationsPair<O, P> combinationsPair(@NonNull BiFunction<O, O, P> biFunction) {
        return new CombinationsPair<O, P>() {
            @NonNull
            @Override
            protected BiFunction<O, O, P> biFunction() {
                return biFunction;
            }
        };
    }


    private static abstract class CombinationsPair<O, P> implements ObservableTransformer<O, P> {

        final private AtomicInteger i = new AtomicInteger();

        @Override
        public @NonNull
        final ObservableSource<P> apply(@NonNull Observable<O> upstream) {
            return upstream.flatMap(o -> upstream.skip(i.incrementAndGet()), biFunction());
        }

        abstract protected @NonNull
        BiFunction<O, O, P> biFunction();

    }


    private static abstract class FlowableCombinationsPair<O, P> implements FlowableTransformer<O, P> {

        final private AtomicInteger i = new AtomicInteger();

        @Override
        public @NonNull
        final Publisher<P> apply(@NonNull Flowable<O> upstream) {
            return upstream.compose(compose())
                    .flatMap(o -> upstream.skip(i.incrementAndGet()), biFunction());
        }

        abstract protected @NonNull
        BiFunction<O, O, P> biFunction();

        protected @NonNull
        FlowableTransformer<O, O> compose() {
            return upstream -> upstream;
        }

    }

    public static String concatId(@NonNull Pair<String, String> pair) {
        return concatId(pair.first, pair.second);
    }

    public static String concatId(@NonNull String sId, @NonNull String eId) {
        String[] sortString = {sId, eId};
        Arrays.sort(sortString, String.CASE_INSENSITIVE_ORDER);
        return sortString[0] + sortString[1];
    }

    protected static Transformation mTransformation(@NonNull Animation animation) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        Class<Animation> animationClass = (Class<Animation>) Class.forName("android.view.animation.Animation");
        Field mTransformationField = animationClass.getDeclaredField("mTransformation");
        mTransformationField.setAccessible(true);
        final Transformation mTransformationObj = (Transformation) mTransformationField.get(animation);
        return mTransformationObj;
    }

    protected static Transformation mPreviousTransformation(@NonNull Animation animation) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        Class<Animation> animationClass = (Class<Animation>) Class.forName("android.view.animation.Animation");
        Field mPreviousTransformation = animationClass.getDeclaredField("mPreviousTransformation");
        mPreviousTransformation.setAccessible(true);
        final Transformation mPreviousTransformationObj = (Transformation) mPreviousTransformation.get(animation);
        return mPreviousTransformationObj;
    }

//    protected static Transformation mChildTransformation(@NonNull ViewGroup viewGroup) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
//        Class<ViewGroup> viewGroupClass = ViewGroup.class;
//        Field mChildTransformation = viewGroupClass.getDeclaredField("mChildTransformation");
//        mChildTransformation.setAccessible(true);
//        final Transformation mChildTransformationObj = (Transformation) mChildTransformation.get(viewGroup);
//        return mChildTransformationObj;
//    }
//
//    protected static Transformation mInvalidationTransformation(@NonNull ViewGroup viewGroup) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
//        Class<ViewGroup> viewGroupClass = ViewGroup.class;
//        Field mChildTransformation = viewGroupClass.getDeclaredField("mInvalidationTransformation");
//        mChildTransformation.setAccessible(true);
//        final Transformation mChildTransformationObj = (Transformation) mChildTransformation.get(viewGroup);
//        return mChildTransformationObj;
//    }

}
