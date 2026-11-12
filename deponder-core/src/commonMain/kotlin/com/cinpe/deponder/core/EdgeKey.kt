package com.cinpe.deponder.core

import kotlin.jvm.JvmInline

/**
 * Unboxed 2D float vector packed into a single primitive [Long] to eliminate per-frame heap
 * allocations on 60–144Hz VSYNC hot paths.
 */
@JvmInline
public value class Vec2(public val packed: Long) {
    public constructor(x: Float, y: Float) : this(
        (x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFF_FFFFL),
    )

    public val x: Float
        get() = Float.fromBits((packed ushr 32).toInt())

    public val y: Float
        get() = Float.fromBits(packed.toInt())

    public operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    public operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    public operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)

    public companion object {
        public val Zero: Vec2 = Vec2(0L)
    }
}

/**
 * Platform-independent endpoint ID pair (`startId -> endId`) preserving user-specified visual direction.
 */
public data class NodePair(
    public val startId: String,
    public val endId: String,
)

/**
 * Collision-free, symmetric undirected edge identifier for a pair of distinct planet IDs.
 *
 * Orders endpoints via strict lexicographical order (`String.compareTo`) so `EdgeKey("a", "A") == EdgeKey("A", "a")`,
 * and encodes length prefixes (`"${first.length}:$first#$second"`) so `("1", "23")` and `("12", "3")` never collide.
 */
public class EdgeKey(a: String, b: String) {
    public val first: String
    public val second: String
    private val cachedHash: Int

    init {
        if (a <= b) {
            first = a
            second = b
        } else {
            first = b
            second = a
        }
        cachedHash = 31 * first.hashCode() + second.hashCode()
    }

    public val encoded: String
        get() = encodeOrdered(first, second)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeKey) return false
        return cachedHash == other.cachedHash && first == other.first && second == other.second
    }

    override fun hashCode(): Int = cachedHash

    override fun toString(): String = encoded

    public companion object {
        public fun encode(sId: String, eId: String): String =
            if (sId <= eId) encodeOrdered(sId, eId) else encodeOrdered(eId, sId)

        private fun encodeOrdered(lo: String, hi: String): String =
            "${lo.length}:$lo#$hi"
    }
}
