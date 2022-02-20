package com.cinpe.deponder.option;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.NAnimator;
import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * @Description: 橡皮筋(因变对象)
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public abstract class RubberOption extends BaseOption {

    /**
     * the start Planet id.
     */
    @NonNull
    public abstract String sId();

    /**
     * the end Planet id.
     */
    @NonNull
    public abstract String eId();

    /**
     * 弹性系数.
     */
    public abstract float elasticityCoefficient();

    /**
     * 自然长度(px).
     */
    public abstract int naturalLength();

    /**
     * 可拉伸的矢量View. 例如9.png
     */
    public abstract List<BaseOption> vArr();

}
