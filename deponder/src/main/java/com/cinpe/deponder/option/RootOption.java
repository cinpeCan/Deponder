package com.cinpe.deponder.option;
import android.view.ViewGroup;
import androidx.annotation.NonNull;


/**
 * @Description: 根布局.
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class RootOption extends BaseOption {

    @NonNull
    @Override
    public abstract ViewGroup itemView();

    /**
     * 空间初始化缩放值(当行星过多或过少时,可能进行缩放)
     */
    public abstract float initScale();

    /**
     * 空间最大缩放值(在初始化缩放值基础上.)
     */
    public abstract float maxScale();

    /**
     * 空间最小缩放值(在初始化缩放值基础上.)
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
    public abstract float elasticityCoefficient();


}
