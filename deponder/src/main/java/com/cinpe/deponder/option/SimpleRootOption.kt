package com.cinpe.deponder.option

import android.view.ViewGroup

/**
 * @Description: viewGroup option.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 */
public interface SimpleRootOption : BaseOption {

    override fun itemView(): ViewGroup

    /**
     * 初始化缩放值
     */
    public fun initScale(): Float

    /**
     * 空间最大缩放值(在初始化缩放值基础上缩放.)
     */
    public fun maxScale(): Float

    /**
     * 空间最小缩放值(在初始化缩放值基础上缩放.)
     */
    public fun minScale(): Float

    /**
     * 空间中以太密度.
     */
    public fun mRootDensity(): Float

    /**
     * 斥力的辐射范围.(px)
     */
    public fun mInternalPressure(): Float

    /**
     * 弹性系数.
     */
    public fun elasticityCoefficientStart(): Float
    public fun elasticityCoefficientTop(): Float
    public fun elasticityCoefficientEnd(): Float
    public fun elasticityCoefficientBot(): Float

    /**
     * 输入防抖（最小输入间隔）
     */
    public fun minInterval(): Long
}
