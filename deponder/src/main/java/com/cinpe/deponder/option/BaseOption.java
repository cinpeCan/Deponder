package com.cinpe.deponder.option;

import android.graphics.Matrix;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.NAnimator;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class BaseOption {

    @NonNull
    public abstract View itemView();

    @NonNull
    public abstract String id();

    @NonNull
    public abstract NAnimator animator();

    /**
     * 矩阵
     */
    @NonNull
    public abstract Matrix matrix();

}
