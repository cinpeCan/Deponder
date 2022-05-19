package com.cinpe.deponder.option;

import android.graphics.Matrix;

import androidx.annotation.NonNull;

import com.cinpe.deponder.itf.FrontalAreaProperty;
import com.cinpe.deponder.itf.QualityProperty;

/**
 * @Description: 行星 Planet wapper   .
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 */
public abstract class PlanetOption extends BaseOption implements QualityProperty, FrontalAreaProperty {

    @Override
    public abstract float quality();

    /**
     * 迎风面积.(弃用)
     * Frontal area.(Deprecated)
     */
    @Override
    @Deprecated
    public abstract float frontalArea();

    /**
     * 斥力的辐射范围.(px)
     * Radiation range of repulsion.(px)
     */
    public abstract float mInternalPressure();

    /**
     * 当前速度矢量.
     * current velocity vector.
     */
    @NonNull
    public abstract Matrix speed();

    /**
     * 弹性系数.
     * 当其它planet对象靠近时, 力的计算简化为 距离和弹性系数一维积分.
     * Elasticity coefficient.
     * When another planet approaches, the calculation of the force is simplified to the one-dimensional integral of the distance and the elastic coefficient.
     */
    public abstract float elasticityCoefficient();

}
