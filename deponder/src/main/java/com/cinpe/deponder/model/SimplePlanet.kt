package com.cinpe.deponder.model

import android.view.View
import com.cinpe.deponder.DeponderDslMarker
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.option.PlanetOption

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
public class SimplePlanet private constructor(
    private val itemView: View,
    private val id: String,
    private val quality: Float,
    private val frontalArea: Float,
    private val mInternalPressure: Float,
    private val elasticityCoefficient: Float,
) : PlanetOption {

    override fun itemView(): View = itemView

    override fun id(): String = id

    override fun quality(): Float = quality

    @Deprecated("Frontal area is deprecated and unused.")
    override fun frontalArea(): Float = frontalArea

    override fun mInternalPressure(): Float = mInternalPressure

    override fun elasticityCoefficient(): Float = elasticityCoefficient

    public fun toBuilder(): Builder = Builder(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimplePlanet) return false
        return itemView == other.itemView &&
            id == other.id &&
            quality.toBits() == other.quality.toBits() &&
            frontalArea.toBits() == other.frontalArea.toBits() &&
            mInternalPressure.toBits() == other.mInternalPressure.toBits() &&
            elasticityCoefficient.toBits() == other.elasticityCoefficient.toBits()
    }

    override fun hashCode(): Int {
        var h = 1
        h = h * 1_000_003 xor itemView.hashCode()
        h = h * 1_000_003 xor id.hashCode()
        h = h * 1_000_003 xor quality.toBits()
        h = h * 1_000_003 xor frontalArea.toBits()
        h = h * 1_000_003 xor mInternalPressure.toBits()
        h = h * 1_000_003 xor elasticityCoefficient.toBits()
        return h
    }

    override fun toString(): String =
        "SimplePlanet{itemView=$itemView, id=$id, quality=$quality, " +
            "frontalArea=$frontalArea, mInternalPressure=$mInternalPressure, " +
            "elasticityCoefficient=$elasticityCoefficient}"

    @DeponderDslMarker
    public class Builder internal constructor() {
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var itemView: View? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var id: String? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var quality: Float = DeponderHelper.DEFAULT_QUALITY_PROPERTY

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var frontalArea: Float = DeponderHelper.DEFAULT_INIT_SCALE

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var mInternalPressure: Float = DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE.toFloat()

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficient: Float = DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT

        internal constructor(source: SimplePlanet) : this() {
            this.itemView = source.itemView
            this.id = source.id
            this.quality = source.quality
            @Suppress("DEPRECATION")
            this.frontalArea = source.frontalArea
            this.mInternalPressure = source.mInternalPressure
            this.elasticityCoefficient = source.elasticityCoefficient
        }

        public fun itemView(itemView: View): Builder = apply { this.itemView = itemView }

        public fun id(id: String): Builder = apply { this.id = id }

        public fun quality(quality: Float): Builder = apply { this.quality = quality }

        public fun frontalArea(frontalArea: Float): Builder = apply { this.frontalArea = frontalArea }

        public fun mInternalPressure(mInternalPressure: Float): Builder = apply {
            this.mInternalPressure = mInternalPressure
        }

        public fun elasticityCoefficient(elasticityCoefficient: Float): Builder = apply {
            this.elasticityCoefficient = elasticityCoefficient
        }

        public fun build(): SimplePlanet {
            val view = itemView ?: throw IllegalStateException("Missing required properties: itemView")
            val resolvedId = id?.takeIf { it.isNotEmpty() } ?: view.hashCode().toString()
            return SimplePlanet(
                itemView = view,
                id = resolvedId,
                quality = quality,
                frontalArea = frontalArea,
                mInternalPressure = mInternalPressure,
                elasticityCoefficient = elasticityCoefficient,
            )
        }
    }

    public companion object {
        @JvmStatic
        public fun create(
            itemView: View,
            id: String,
            quality: Float,
            frontalArea: Float,
            mInternalPressure: Float,
            elasticityCoefficient: Float,
        ): SimplePlanet = builder()
            .itemView(itemView)
            .id(id)
            .quality(quality)
            .frontalArea(frontalArea)
            .mInternalPressure(mInternalPressure)
            .elasticityCoefficient(elasticityCoefficient)
            .build()

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
