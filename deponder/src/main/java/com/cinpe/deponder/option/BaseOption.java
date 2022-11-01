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
public interface BaseOption {

    @NonNull
    View itemView();

    @NonNull
    String id();

    @NonNull
    NAnimator animator();

    /**
     * 矩阵
     */
    @NonNull
    Matrix matrix();

}
