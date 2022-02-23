package com.cinpe.deponder.option;

import android.graphics.Matrix;
import androidx.annotation.NonNull;
import com.cinpe.deponder.itf.FrontalAreaProperty;
import com.cinpe.deponder.itf.QualityProperty;

/**
 * @Description: 行星.(自变对象)
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class PlanetOption extends BaseOption implements QualityProperty, FrontalAreaProperty {

    @Override
    public abstract float quality();

    /**
     * 迎风面积.(对角线长度)
     */
    @Override
    public abstract float frontalArea();

    /**
     * 斥力的辐射范围.(px)
     */
    public abstract float mInternalPressure();

    /**
     * 当前速度矢量.
     */
    @NonNull
    public abstract Matrix speed();

//    /**
//     * 当前加速度矢量.
//     */
//    @NonNull
//    public abstract PointF acceleration();

    /**
     * 弹性系数.
     */
    public abstract float elasticityCoefficient();

//    @NonNull
//    @Override
//    public final RectF rectF() {
//        final Rect rect = new Rect();
//        itemView().getHitRect(rect);
//        final RectF rectF = new RectF(rect);
//        float[] floats = new float[9];
//        matrix().getValues(floats);
//        rectF.offset(floats[Matrix.MTRANS_X], floats[Matrix.MTRANS_Y]);
//        final Matrix matrix = new Matrix();
//        matrix.postScale(floats[Matrix.MSCALE_X], floats[Matrix.MSCALE_Y], rectF.centerX(), rectF.centerY());
//        matrix.mapRect(rectF);
//        return rectF;
//    }

}
