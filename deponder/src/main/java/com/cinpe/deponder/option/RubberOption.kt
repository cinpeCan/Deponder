package com.cinpe.deponder.option

/**
 * @Description: 橡皮筋(因变对象)
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public interface RubberOption : BaseOption {

    /**
     * the start Planet id.
     */
    public fun sId(): String

    /**
     * the end Planet id.
     */
    public fun eId(): String

    /**
     * 弹性系数.
     */
    public fun elasticityCoefficient(): Float

    /**
     * 自然长度(px).
     */
    public fun naturalLength(): Int

    /**
     * 可拉伸的矢量View. 例如9.png
     */
    public fun vArr(): List<BaseOption>
}
