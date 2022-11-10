package com.cinpe.deponder.option;

import android.view.ViewGroup;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;


/**
 * @Description: viewGroup option.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 */
public interface SimpleRootOption extends BaseOption {

    @NonNull
    @Override
    ViewGroup itemView();

    /**
     * 初始化缩放值
     */
    float initScale();

    /**
     * 空间最大缩放值(在初始化缩放值基础上缩放.)
     */
    float maxScale();

    /**
     * 空间最小缩放值(在初始化缩放值基础上缩放.)
     */
    float minScale();

    /**
     * 空间中以太密度.
     */
    float mRootDensity();

    /**
     * 斥力的辐射范围.(px)
     */
    float mInternalPressure();

    /**
     * 弹性系数.
     */
    float elasticityCoefficientStart();
    float elasticityCoefficientTop();
    float elasticityCoefficientEnd();
    float elasticityCoefficientBot();

    /**
     * 输入防抖（最小输入间隔）
     */
    long minInterval();


}
