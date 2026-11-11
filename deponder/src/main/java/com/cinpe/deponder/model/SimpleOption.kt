package com.cinpe.deponder.model

import android.view.View
import com.cinpe.deponder.DeponderDslMarker
import com.cinpe.deponder.option.BaseOption

public class SimpleOption private constructor(
    private val itemView: View,
) : BaseOption {

    private val memoizedId: String by lazy { itemView.hashCode().toString() }

    override fun itemView(): View = itemView

    override fun id(): String = memoizedId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleOption) return false
        return itemView == other.itemView
    }

    override fun hashCode(): Int = 1_000_003 xor itemView.hashCode()

    override fun toString(): String = "SimpleOption{itemView=$itemView}"

    @DeponderDslMarker
    public class Builder internal constructor() {
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var itemView: View? = null

        public fun itemView(itemView: View): Builder = apply {
            this.itemView = itemView
        }

        public fun build(): SimpleOption {
            val view = itemView ?: throw IllegalStateException("Missing required properties: itemView")
            return SimpleOption(view)
        }
    }

    public companion object {
        @JvmStatic
        public fun create(itemView: View): SimpleOption =
            builder().itemView(itemView).build()

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
