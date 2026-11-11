package com.cinpe.deponder.internal

import android.graphics.PointF
import android.graphics.RectF
import com.cinpe.deponder.DeponderHelper
import com.cinpe.deponder.model.SimplePlanet
import com.cinpe.deponder.model.SimpleRubber
import com.cinpe.deponder.option.PlanetOption
import com.cinpe.deponder.option.SimpleRootOption
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Unboxed 2D float vector packed into a single primitive [Long] to eliminate per-frame heap allocations
 * on the 60/120Hz VSYNC physics hot path.
 */
@JvmInline
internal value class Vec2(val packed: Long) {
    constructor(x: Float, y: Float) : this(
        (x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFF_FFFFL),
    )

    val x: Float
        get() = Float.fromBits((packed ushr 32).toInt())

    val y: Float
        get() = Float.fromBits(packed.toInt())

    fun toPointF(): PointF = PointF(x, y)

    companion object {
        val Zero: Vec2 = Vec2(0L)
    }
}

/**
 * Mutable per-frame state for a single planet node (unboxed primitive float force, velocity, and center state).
 */
internal class PlanetNode<PO : PlanetOption>(
    var p: PO,
) {
    var vx: Float = 0f
    var vy: Float = 0f
    var fx: Float = 0f
    var fy: Float = 0f
    var px: Float = 0f
    var py: Float = 0f
    var count: Int = 0

    init {
        syncCenter()
    }

    fun syncCenter() {
        val v = p.itemView()
        px = v.left + v.width * 0.5f + v.translationX
        py = v.top + v.height * 0.5f + v.translationY
    }

    fun update() {
        fx = 0f
        fy = 0f
        count = 0
        if (p.itemView().isPressed) {
            vx = 0f
            vy = 0f
        }
        syncCenter()
    }
}

/**
 * Pure-float result of rubber geometry calculation for unit testing.
 */
internal data class RubberTransform(
    val centerX: Float,
    val centerY: Float,
    val length: Float,
    val rotationDeg: Float,
    val childScaleX: Float,
    val childScaleY: Float,
)

/**
 * Pure-float, zero-allocation physics engine for Deponder (`step`, `drawPlanet`, `drawRubber`).
 *
 * Pure numeric entry points (`wallForcePure`, `pairForcePure`, `integratePlanetPure`,
 * `computeRubberTransformPure`) are exposed as `internal` so unit tests can verify them directly
 * without View overhead.
 */
internal class PhysicsEngine(
    private val rootOption: SimpleRootOption,
) {

    fun step(
        nodes: List<PlanetNode<SimplePlanet>>,
        rubbers: Map<String, SimpleRubber>,
        dt: Long,
        scale: Float,
    ) {
        val rootView = rootOption.itemView()
        val rootWidth = rootView.width.toFloat()
        val rootHeight = rootView.height.toFloat()
        val rootInternalPressure = rootOption.mInternalPressure()
        val ecStart = rootOption.elasticityCoefficientStart()
        val ecTop = rootOption.elasticityCoefficientTop()
        val ecEnd = rootOption.elasticityCoefficientEnd()
        val ecBot = rootOption.elasticityCoefficientBot()

        for (k in nodes.indices) {
            val s = nodes[k]
            val wallF = wallForceVec(
                px = s.px,
                py = s.py,
                scale = scale,
                rootWidth = rootWidth,
                rootHeight = rootHeight,
                rootInternalPressure = rootInternalPressure,
                ecStart = ecStart,
                ecTop = ecTop,
                ecEnd = ecEnd,
                ecBot = ecBot,
            )
            s.fx += wallF.x
            s.fy += wallF.y
            for (j in k + 1 until nodes.size) {
                val e = nodes[j]
                if (s.p.id() == e.p.id()) continue
                val ro = rubbers[DeponderHelper.concatId(s.p.id(), e.p.id())]
                val pairF: Vec2 = if (ro != null) {
                    if (ro.id().startsWith(s.p.id())) {
                        drawRubber(ro, s.px, s.py, e.px, e.py, scale)
                    } else {
                        drawRubber(ro, e.px, e.py, s.px, s.py, scale)
                    }
                    s.count += 1
                    e.count += 1
                    pairForceVec(
                        sx = s.px,
                        sy = s.py,
                        ex = e.px,
                        ey = e.py,
                        scale = scale,
                        rubberElasticityCoefficient = ro.elasticityCoefficient(),
                        rubberNaturalLength = ro.naturalLength(),
                        elasticityCoefficient = s.p.elasticityCoefficient(),
                        planetInternalPressure = s.p.mInternalPressure() + s.count * DeponderHelper.INCREMENTAL_SCALE,
                    )
                } else {
                    pairForceVec(
                        sx = s.px,
                        sy = s.py,
                        ex = e.px,
                        ey = e.py,
                        scale = scale,
                        rubberElasticityCoefficient = 0f,
                        rubberNaturalLength = 0,
                        elasticityCoefficient = s.p.elasticityCoefficient(),
                        planetInternalPressure = s.p.mInternalPressure() + s.count * DeponderHelper.INCREMENTAL_SCALE,
                    )
                }
                s.fx -= pairF.x
                s.fy -= pairF.y
                e.fx += pairF.x
                e.fy += pairF.y
            }
            drawPlanet(s, dt, scale)
            s.update()
        }
    }

    private fun drawPlanet(node: PlanetNode<SimplePlanet>, interval: Long, scale: Float) {
        val po = node.p
        val view = po.itemView()
        val pressed = view.isPressed
        val quality = po.quality()
        val rootDensity = rootOption.mRootDensity()

        val ax = node.fx / quality - node.vx * rootDensity
        val ay = node.fy / quality - node.vy * rootDensity
        val dx = DeponderHelper.calculateAxisDisplacement(node.vx, ax, interval)
        val dy = DeponderHelper.calculateAxisDisplacement(node.vy, ay, interval)
        val minAcceleration = DeponderHelper.MIN_ACCELERATION * scale

        if (hypot(dx, dy) < minAcceleration || pressed) {
            node.vx = 0f
            node.vy = 0f
            DeponderHelper.applyPlanetTransform(view, 0f, 0f, scale)
        } else {
            node.vx += ax * interval
            node.vy += ay * interval
            DeponderHelper.applyPlanetTransform(view, dx, dy, scale)
        }
    }

    private fun drawRubber(
        ro: SimpleRubber,
        sx: Float,
        sy: Float,
        ex: Float,
        ey: Float,
        scale: Float,
    ) {
        val v = ro.itemView()
        val rWidth = v.width
        val diffX = ex - sx
        val diffY = ey - sy
        val centerX = (sx + ex) * 0.5f
        val centerY = (sy + ey) * 0.5f
        val length = hypot(diffX, diffY)
        val angle = atan2(diffX.toDouble(), diffY.toDouble())
        val rotationDeg = (90.0 - angle * RAD_TO_DEG).toFloat()

        DeponderHelper.applyRubberTransform(
            v = v,
            centerX = centerX,
            centerY = centerY,
            length = length,
            scale = scale,
            rotationDeg = rotationDeg,
        )

        val children = ro.vArr()
        if (children.isNotEmpty()) {
            // Parent `v` already applies `scaleX = length / rWidth` and `scaleY = scale`, so counter-scaling
            // `childScaleX = (rWidth / length) * scale` and `childScaleY = 1f` yields uniform `(scale, scale)` in screen space.
            val childScaleX = if (length > 0f && rWidth > 0) (rWidth.toFloat() / length) * scale else scale
            for (i in children.indices) {
                val childView = children[i].itemView()
                childView.scaleX = childScaleX
                childView.scaleY = 1f
            }
        }
    }

    companion object {
        private const val RAD_TO_DEG: Double = 180.0 / kotlin.math.PI

        internal fun wallForceVec(
            px: Float,
            py: Float,
            scale: Float,
            rootWidth: Float,
            rootHeight: Float,
            rootInternalPressure: Float,
            ecStart: Float,
            ecTop: Float,
            ecEnd: Float,
            ecBot: Float,
        ): Vec2 {
            var l = px
            var t = py
            var r = rootWidth - px
            var b = rootHeight - py

            val internalPressure = rootInternalPressure * scale
            l = if (l < internalPressure) internalPressure - l else 0f
            t = if (t < internalPressure) internalPressure - t else 0f
            r = if (r < internalPressure) internalPressure - r else 0f
            b = if (b < internalPressure) internalPressure - b else 0f

            return Vec2(
                DeponderHelper.calculate(l, ecStart) - DeponderHelper.calculate(r, ecEnd),
                DeponderHelper.calculate(t, ecTop) - DeponderHelper.calculate(b, ecBot),
            )
        }

        internal fun wallForcePure(
            point: PointF,
            scale: Float,
            rootRect: RectF,
            rootInternalPressure: Float,
            ecStart: Float,
            ecTop: Float,
            ecEnd: Float,
            ecBot: Float,
        ): PointF = wallForceVec(
            px = point.x,
            py = point.y,
            scale = scale,
            rootWidth = rootRect.width(),
            rootHeight = rootRect.height(),
            rootInternalPressure = rootInternalPressure,
            ecStart = ecStart,
            ecTop = ecTop,
            ecEnd = ecEnd,
            ecBot = ecBot,
        ).toPointF()

        internal fun pairForceVec(
            sx: Float,
            sy: Float,
            ex: Float,
            ey: Float,
            scale: Float,
            rubberElasticityCoefficient: Float,
            rubberNaturalLength: Int,
            elasticityCoefficient: Float,
            planetInternalPressure: Float,
        ): Vec2 {
            if (sx == ex && sy == ey) return Vec2.Zero

            val diffX = ex - sx
            val diffY = ey - sy
            val angle = atan2(diffX.toDouble(), diffY.toDouble())
            val sinA = sin(angle)
            val cosA = cos(angle)
            val length = hypot(diffX, diffY)

            var fx = 0f
            var fy = 0f

            if (rubberElasticityCoefficient != 0f) {
                val natural = rubberNaturalLength * scale
                val rubberDiff = DeponderHelper.dDistance(length, natural).toDouble()
                val rdx = (sinA * rubberDiff).roundToLong().toFloat()
                val rdy = (cosA * rubberDiff).roundToLong().toFloat()
                fx += DeponderHelper.calculate(rdx, rubberElasticityCoefficient)
                fy += DeponderHelper.calculate(rdy, rubberElasticityCoefficient)
            }

            val mInternalPressure = planetInternalPressure * scale
            if (length < mInternalPressure) {
                val planetDiff = DeponderHelper.dDistance(length, mInternalPressure).toDouble()
                val pdx = (sinA * planetDiff).roundToLong().toFloat()
                val pdy = (cosA * planetDiff).roundToLong().toFloat()
                fx += DeponderHelper.calculate(pdx, elasticityCoefficient)
                fy += DeponderHelper.calculate(pdy, elasticityCoefficient)
            }

            return Vec2(fx, fy)
        }

        internal fun pairForcePure(
            s: PointF,
            e: PointF,
            scale: Float,
            rubberElasticityCoefficient: Float,
            rubberNaturalLength: Int,
            elasticityCoefficient: Float,
            planetInternalPressure: Float,
        ): PointF = pairForceVec(
            sx = s.x,
            sy = s.y,
            ex = e.x,
            ey = e.y,
            scale = scale,
            rubberElasticityCoefficient = rubberElasticityCoefficient,
            rubberNaturalLength = rubberNaturalLength,
            elasticityCoefficient = elasticityCoefficient,
            planetInternalPressure = planetInternalPressure,
        ).toPointF()

        internal fun integratePlanetPure(
            speed: PointF,
            pressed: Boolean,
            quality: Float,
            rootDensity: Float,
            force: PointF,
            interval: Long,
            scale: Float,
        ): PointF {
            val ax = force.x / quality - speed.x * rootDensity
            val ay = force.y / quality - speed.y * rootDensity
            val dx = DeponderHelper.calculateAxisDisplacement(speed.x, ax, interval)
            val dy = DeponderHelper.calculateAxisDisplacement(speed.y, ay, interval)
            val minAcceleration = DeponderHelper.MIN_ACCELERATION * scale

            return if (hypot(dx, dy) < minAcceleration || pressed) {
                speed.set(0f, 0f)
                PointF(0f, 0f)
            } else {
                speed.offset(ax * interval, ay * interval)
                PointF(dx, dy)
            }
        }

        internal fun computeRubberTransformPure(
            rWidth: Int,
            sP: PointF,
            eP: PointF,
            scale: Float,
        ): RubberTransform {
            val diffX = eP.x - sP.x
            val diffY = eP.y - sP.y
            val center = DeponderHelper.centerPointF(sP, eP)
            val length = hypot(diffX, diffY)
            val angle = atan2(diffX.toDouble(), diffY.toDouble())
            val rotationDeg = (90.0 - angle * RAD_TO_DEG).toFloat()
            val childScaleX = if (length > 0f && rWidth > 0) (rWidth.toFloat() / length) * scale else scale
            return RubberTransform(
                centerX = center.x,
                centerY = center.y,
                length = length,
                rotationDeg = rotationDeg,
                childScaleX = childScaleX,
                childScaleY = 1f,
            )
        }
    }
}
