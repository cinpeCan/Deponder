package com.cinpe.deponder.option;

import android.graphics.Matrix;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.NAnimator;

/**
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
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
