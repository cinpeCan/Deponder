package com.cinpe.deponder.option;

import android.view.ViewGroup;

import androidx.annotation.NonNull;


/**
 * @Description: viewGroup option.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 */
public abstract class SimpleRootOption extends BaseOption {

    @NonNull
    @Override
    public abstract ViewGroup itemView();

    /**
     * 初始化缩放值
     */
    public abstract float initScale();

    /**
     * 空间最大缩放值(在初始化缩放值基础上缩放.)
     */
    public abstract float maxScale();

    /**
     * 空间最小缩放值(在初始化缩放值基础上缩放.)
     */
    public abstract float minScale();

    /**
     * 空间中以太密度.
     */
    public abstract float mRootDensity();

    /**
     * 斥力的辐射范围.(px)
     */
    public abstract float mInternalPressure();

    /**
     * 弹性系数.
     */
    public abstract float elasticityCoefficientStart();
    public abstract float elasticityCoefficientTop();
    public abstract float elasticityCoefficientEnd();
    public abstract float elasticityCoefficientBot();


}
