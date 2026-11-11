package com.cinpe.deponder.model

import android.view.View
import android.view.ViewGroup
import com.cinpe.deponder.DeponderDslMarker
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.option.BaseOption
import com.cinpe.deponder.option.RubberOption
import java.util.Collections

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
public class SimpleRubber private constructor(
    private val itemView: View,
    private val sId: String,
    private val eId: String,
    private val elasticityCoefficient: Float,
    private val naturalLength: Int,
    existingVArr: List<BaseOption>? = null,
) : RubberOption {

    private val memoizedId: String by lazy { DeponderHelper.concatId(sId, eId) }
    private val memoizedVArr: List<BaseOption> by lazy {
        existingVArr ?: run {
            val group = itemView as? ViewGroup ?: return@lazy emptyList()
            val result = ArrayList<BaseOption>()
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i) ?: continue
                val tag = child.tag
                if (tag is String && tag == DeponderHelper.TAG_OF_UN_RUBBER) {
                    result.add(SimpleOption.create(child))
                }
            }
            Collections.unmodifiableList(result)
        }
    }

    internal fun withRuntimeStateFrom(other: SimpleRubber): SimpleRubber {
        if (this === other || this.itemView !== other.itemView) return this
        return SimpleRubber(
            itemView = itemView,
            sId = sId,
            eId = eId,
            elasticityCoefficient = elasticityCoefficient,
            naturalLength = naturalLength,
            existingVArr = other.vArr(),
        )
    }

    override fun itemView(): View = itemView

    override fun id(): String = memoizedId

    override fun sId(): String = sId

    override fun eId(): String = eId

    override fun elasticityCoefficient(): Float = elasticityCoefficient

    override fun naturalLength(): Int = naturalLength

    override fun vArr(): List<BaseOption> = memoizedVArr

    public fun toBuilder(): Builder = Builder(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleRubber) return false
        return itemView == other.itemView &&
            sId == other.sId &&
            eId == other.eId &&
            elasticityCoefficient.toBits() == other.elasticityCoefficient.toBits() &&
            naturalLength == other.naturalLength
    }

    override fun hashCode(): Int {
        var h = 1
        h = h * 1_000_003 xor itemView.hashCode()
        h = h * 1_000_003 xor sId.hashCode()
        h = h * 1_000_003 xor eId.hashCode()
        h = h * 1_000_003 xor elasticityCoefficient.toBits()
        h = h * 1_000_003 xor naturalLength
        return h
    }

    override fun toString(): String =
        "SimpleRubber{itemView=$itemView, sId=$sId, eId=$eId, " +
            "elasticityCoefficient=$elasticityCoefficient, naturalLength=$naturalLength}"

    @DeponderDslMarker
    public class Builder internal constructor() {
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var itemView: View? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var sId: String? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var eId: String? = null

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var elasticityCoefficient: Float = DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT

        @get:JvmSynthetic
        @set:JvmSynthetic
        public var naturalLength: Int = DeponderHelper.DEFAULT_RUBBER_NATURAL_LENGTH

        internal constructor(source: SimpleRubber) : this() {
            this.itemView = source.itemView
            this.sId = source.sId
            this.eId = source.eId
            this.elasticityCoefficient = source.elasticityCoefficient
            this.naturalLength = source.naturalLength
        }

        public fun itemView(itemView: View): Builder = apply { this.itemView = itemView }

        public fun sId(id: String): Builder = apply { this.sId = id }

        public fun eId(eId: String): Builder = apply { this.eId = eId }

        public fun elasticityCoefficient(elasticityCoefficient: Float): Builder = apply {
            this.elasticityCoefficient = elasticityCoefficient
        }

        public fun naturalLength(naturalLength: Int): Builder = apply {
            this.naturalLength = naturalLength
        }

        public fun build(): SimpleRubber {
            val missing = buildList {
                if (itemView == null) add("itemView")
                if (sId == null) add("sId")
                if (eId == null) add("eId")
            }
            if (missing.isNotEmpty()) {
                throw IllegalStateException("Missing required properties: ${missing.joinToString(" ")}")
            }
            val start = sId!!
            val end = eId!!
            if (start == end) {
                throw IllegalStateException("Start and End points cannot be same")
            }
            return SimpleRubber(
                itemView = itemView!!,
                sId = start,
                eId = end,
                elasticityCoefficient = elasticityCoefficient,
                naturalLength = naturalLength,
            )
        }
    }

    public companion object {
        @JvmStatic
        public fun create(
            itemView: View,
            sId: String,
            eId: String,
            elasticityCoefficient: Float,
            naturalLength: Int,
        ): SimpleRubber = builder()
            .itemView(itemView)
            .sId(sId)
            .eId(eId)
            .elasticityCoefficient(elasticityCoefficient)
            .naturalLength(naturalLength)
            .build()

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
