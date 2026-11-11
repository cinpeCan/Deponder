package com.cinpe.deponder.option

import com.cinpe.deponder.itf.FrontalAreaProperty
import com.cinpe.deponder.itf.QualityProperty

/**
 * @Description: 行星 Planet wapper   .
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 */
public interface PlanetOption : BaseOption, QualityProperty, FrontalAreaProperty {

    override fun quality(): Float

    /**
     * 迎风面积.(弃用)
     * Frontal area.(Deprecated)
     */
    @Deprecated("Frontal area is deprecated and unused.")
    override fun frontalArea(): Float

    /**
     * 斥力的辐射范围.(px)
     * Radiation range of repulsion.(px)
     */
    public fun mInternalPressure(): Float

    /**
     * 弹性系数.
     * 当其它planet对象靠近时, 力的计算简化为 距离和弹性系数一维积分.
     * Elasticity coefficient.
     * When another planet approaches, the calculation of the force is simplified to the one-dimensional integral of the distance and the elastic coefficient.
     */
    public fun elasticityCoefficient(): Float
}
