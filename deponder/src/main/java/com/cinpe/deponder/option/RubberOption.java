package com.cinpe.deponder.option;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * @Description: 橡皮筋(因变对象)
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public interface RubberOption extends BaseOption {

    /**
     * the start Planet id.
     */
    @NonNull
    String sId();

    /**
     * the end Planet id.
     */
    @NonNull
    String eId();

    /**
     * 弹性系数.
     */
    float elasticityCoefficient();

    /**
     * 自然长度(px).
     */
    int naturalLength();

    /**
     * 可拉伸的矢量View. 例如9.png
     */
    List<BaseOption> vArr();

}
