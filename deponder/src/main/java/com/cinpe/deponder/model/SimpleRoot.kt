package com.cinpe.deponder.model

import android.view.ViewGroup
import com.cinpe.deponder.DeponderDslMarker
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.option.SimpleRootOption
import kotlin.math.max

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
public class SimpleRoot private constructor(
    private val itemView: ViewGroup,
    private val initScale: Float,
    private val maxScale: Float,
    private val minScale: Float,
    private val mRootDensity: Float,
    private val mInternalPressure: Float,
    private val elasticityCoefficientStart: Float,
    private val elasticityCoefficientTop: Float,
    private val elasticityCoefficientEnd: Float,
    private val elasticityCoefficientBot: Float,
    private val minInterval: Long,
) : SimpleRootOption {

    private val memoizedId: String by lazy { itemView.hashCode().toString() }

    override fun itemView(): ViewGroup = itemView

    override fun id(): String = memoizedId

    override fun initScale(): Float = initScale

    override fun maxScale(): Float = maxScale

    override fun minScale(): Float = minScale

    override fun mRootDensity(): Float = mRootDensity

    override fun mInternalPressure(): Float = mInternalPressure

    override fun elasticityCoefficientStart(): Float = elasticityCoefficientStart

    override fun elasticityCoefficientTop(): Float = elasticityCoefficientTop

    override fun elasticityCoefficientEnd(): Float = elasticityCoefficientEnd

    override fun elasticityCoefficientBot(): Float = elasticityCoefficientBot

    override fun minInterval(): Long = minInterval

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleRoot) return false
        return itemView == other.itemView &&
            initScale.toBits() == other.initScale.toBits() &&
            maxScale.toBits() == other.maxScale.toBits() &&
            minScale.toBits() == other.minScale.toBits() &&
            mRootDensity.toBits() == other.mRootDensity.toBits() &&
            mInternalPressure.toBits() == other.mInternalPressure.toBits() &&
            elasticityCoefficientStart.toBits() == other.elasticityCoefficientStart.toBits() &&
            elasticityCoefficientTop.toBits() == other.elasticityCoefficientTop.toBits() &&
            elasticityCoefficientEnd.toBits() == other.elasticityCoefficientEnd.toBits() &&
            elasticityCoefficientBot.toBits() == other.elasticityCoefficientBot.toBits() &&
            minInterval == other.minInterval
    }

    override fun hashCode(): Int {
        var h = 1
        h = h * 1_000_003 xor itemView.hashCode()
        h = h * 1_000_003 xor initScale.toBits()
        h = h * 1_000_003 xor maxScale.toBits()
        h = h * 1_000_003 xor minScale.toBits()
        h = h * 1_000_003 xor mRootDensity.toBits()
        h = h * 1_000_003 xor mInternalPressure.toBits()
        h = h * 1_000_003 xor elasticityCoefficientStart.toBits()
        h = h * 1_000_003 xor elasticityCoefficientTop.toBits()
        h = h * 1_000_003 xor elasticityCoefficientEnd.toBits()
        h = h * 1_000_003 xor elasticityCoefficientBot.toBits()
        h = h * 1_000_003 xor minInterval.hashCode()
        return h
    }

    override fun toString(): String =
        "SimpleRoot{itemView=$itemView, initScale=$initScale, maxScale=$maxScale, " +
            "minScale=$minScale, mRootDensity=$mRootDensity, mInternalPressure=$mInternalPressure, " +
            "elasticityCoefficientStart=$elasticityCoefficientStart, " +
            "elasticityCoefficientTop=$elasticityCoefficientTop, " +
            "elasticityCoefficientEnd=$elasticityCoefficientEnd, " +
            "elasticityCoefficientBot=$elasticityCoefficientBot, minInterval=$minInterval}"

    @DeponderDslMarker
    public class Builder internal constructor() {
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var itemView: ViewGroup? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var initScale: Float = DeponderHelper.DEFAULT_INIT_SCALE

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var maxScale: Float = DeponderHelper.DEFAULT_MAX_SCALE

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var minScale: Float = DeponderHelper.DEFAULT_MIN_SCALE

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var mRootDensity: Float = DeponderHelper.DEFAULT_ROOT_DENSITY

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var mInternalPressure: Float = DeponderHelper.DEFAULT_Internal_Pressure.toFloat()

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficientStart: Float = DeponderHelper.DEFAULT_Internal_Pressure_START

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficientTop: Float = DeponderHelper.DEFAULT_Internal_Pressure_TOP

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficientEnd: Float = DeponderHelper.DEFAULT_Internal_Pressure_END

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficientBot: Float = DeponderHelper.DEFAULT_Internal_Pressure_BOTTOM

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var minInterval: Long = 0L

        public fun itemView(itemView: ViewGroup): Builder = apply { this.itemView = itemView }

        public fun initScale(initScale: Float): Builder = apply { this.initScale = initScale }

        public fun maxScale(maxScale: Float): Builder = apply { this.maxScale = maxScale }

        public fun minScale(minScale: Float): Builder = apply { this.minScale = minScale }

        public fun mRootDensity(mRootDensity: Float): Builder = apply { this.mRootDensity = mRootDensity }

        public fun mInternalPressure(mInternalPressure: Float): Builder = apply {
            this.mInternalPressure = mInternalPressure
        }

        public fun elasticityCoefficientStart(elasticityCoefficientStart: Float): Builder = apply {
            this.elasticityCoefficientStart = elasticityCoefficientStart
        }

        public fun elasticityCoefficientTop(elasticityCoefficientTop: Float): Builder = apply {
            this.elasticityCoefficientTop = elasticityCoefficientTop
        }

        public fun elasticityCoefficientEnd(elasticityCoefficientEnd: Float): Builder = apply {
            this.elasticityCoefficientEnd = elasticityCoefficientEnd
        }

        public fun elasticityCoefficientBot(elasticityCoefficientBot: Float): Builder = apply {
            this.elasticityCoefficientBot = elasticityCoefficientBot
        }

        public fun minInterval(minInterval: Long): Builder = apply { this.minInterval = minInterval }

        public fun build(): SimpleRoot {
            val view = itemView ?: throw IllegalStateException("Missing required properties: itemView")
            return SimpleRoot(
                itemView = view,
                initScale = initScale,
                maxScale = maxScale,
                minScale = minScale,
                mRootDensity = mRootDensity,
                mInternalPressure = mInternalPressure,
                elasticityCoefficientStart = elasticityCoefficientStart,
                elasticityCoefficientTop = elasticityCoefficientTop,
                elasticityCoefficientEnd = elasticityCoefficientEnd,
                elasticityCoefficientBot = elasticityCoefficientBot,
                minInterval = max(0L, minInterval),
            )
        }
    }

    public companion object {
        @JvmStatic
        public fun create(
            itemView: ViewGroup,
            initScale: Float,
            maxScale: Float,
            minScale: Float,
            mRootDensity: Float,
            mInternalPressure: Float,
            elasticityCoefficientStart: Float,
            elasticityCoefficientTop: Float,
            elasticityCoefficientEnd: Float,
            elasticityCoefficientBot: Float,
            minInterval: Long,
        ): SimpleRoot = builder()
            .itemView(itemView)
            .initScale(initScale)
            .maxScale(maxScale)
            .minScale(minScale)
            .mRootDensity(mRootDensity)
            .mInternalPressure(mInternalPressure)
            .elasticityCoefficientStart(elasticityCoefficientStart)
            .elasticityCoefficientTop(elasticityCoefficientTop)
            .elasticityCoefficientEnd(elasticityCoefficientEnd)
            .elasticityCoefficientBot(elasticityCoefficientBot)
            .minInterval(minInterval)
            .build()

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
