package com.cinpe.deponder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.Px
import com.cinpe.deponder.option.BaseOption
import com.cinpe.deponder.option.PlanetOption
import kotlin.math.roundToLong

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/5
 * @Version: 0.01
 */
public object DeponderHelper {

    public const val TAG: String = "DeponderHelper"

    /**
     * 空间初始化缩放值(当行星过多或过少时,可能进行缩放)
     */
    public const val DEFAULT_INIT_SCALE: Float = 1f

    /**
     * 空间最大缩放值(在初始化缩放值基础上.)
     */
    public const val DEFAULT_MAX_SCALE: Float = 1.5f

    /**
     * 空间最小缩放值(在初始化缩放值基础上.)
     */
    public const val DEFAULT_MIN_SCALE: Float = 0.5f

    /**
     * 空间中以太密度.
     */
    public const val DEFAULT_ROOT_DENSITY: Float = 0.0006f

    /**
     * 默认planet质量
     */
    public const val DEFAULT_QUALITY_PROPERTY: Float = 2.293f

    /**
     * root的弹性系数.
     */
    public const val DEFAULT_Internal_Pressure_START: Float = 1.44f
    public const val DEFAULT_Internal_Pressure_TOP: Float = 1.44f
    public const val DEFAULT_Internal_Pressure_END: Float = 1.44f
    public const val DEFAULT_Internal_Pressure_BOTTOM: Float = 1.44f

    /**
     * planet的弹性系数.
     */
    public const val DEFAULT_ELASTICITY_COEFFICIENT: Float = 1.33f

    /**
     * rubber的弹性系数.
     */
    public const val DEFAULT_RUBBER_ELASTICITY_COEFFICIENT: Float = 1.68f

    /**
     * 四壁内压的感应距离.(px)
     */
    public const val DEFAULT_Internal_Pressure: Int = 300

    /**
     * planet的感应距离.(px)
     */
    public const val DEFAULT_PLANET_INTERNAL_PRESSURE: Int = 220

    /**
     * rubber的自然长度.(px)
     */
    public const val DEFAULT_RUBBER_NATURAL_LENGTH: Int = 300

    /**
     * distance阈值.(高倍放大时防抖,低于阈值不进行绘制)
     */
    public const val MIN_ACCELERATION: Float = 0.01f

    /**
     * 连接
     */
    public const val INCREMENTAL_SCALE: Float = 0.15f

    /**
     * rubber的tag.
     */
    public const val TAG_OF_UN_RUBBER: String = "UN_RUBBER_RUBBER"

    /**
     * dp->px
     */
    @Px
    internal fun dp2px(context: Context, @Dimension dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

    /**
     * hitRect
     */
    internal fun hitRect(v: View): Rect {
        val rect = Rect()
        v.getHitRect(rect)
        return rect
    }

    /**
     * hitRectF
     */
    internal fun hitRectF(v: View): RectF {
        val rect = Rect()
        v.getHitRect(rect)
        return RectF(rect)
    }

    /**
     * centerPointF
     */
    internal fun centerPointF(s: PointF, e: PointF): PointF =
        PointF((s.x + e.x) / 2f, (s.y + e.y) / 2f)

    /**
     * centerPointF of a planet/option view in parent coordinates.
     */
    internal fun centerPointF(p: BaseOption): PointF {
        val v = p.itemView()
        return PointF(
            v.left + v.width * 0.5f + v.translationX,
            v.top + v.height * 0.5f + v.translationY,
        )
    }

    /**
     * Applies one-step planet frame displacement and scale onto [v]'s RenderNode properties.
     */
    internal fun applyPlanetTransform(v: View, dx: Float, dy: Float, scale: Float) {
        if (dx != 0f) v.translationX += dx
        if (dy != 0f) v.translationY += dy
        if (v.scaleX != scale) v.scaleX = scale
        if (v.scaleY != scale) v.scaleY = scale
    }

    /**
     * Applies one-step rubber line position, stretch, scale, and continuous rotation onto [v]'s RenderNode properties.
     */
    internal fun applyRubberTransform(
        v: View,
        centerX: Float,
        centerY: Float,
        length: Float,
        scale: Float,
        rotationDeg: Float,
    ) {
        val w = v.width
        val h = v.height
        v.translationX = centerX - (v.left + w * 0.5f)
        v.translationY = centerY - (v.top + h * 0.5f)
        v.scaleX = if (w > 0 && length > 0f) length / w.toFloat() else 1f
        v.scaleY = scale
        v.rotation = rotationDeg
    }

    /**
     * 距离->形变距离.
     * 斥力方向为正
     *
     * @param distance 实际距离.
     */
    internal fun dDistance(distance: Float, @FloatRange(from = 0.0) internalPressure: Float): Float =
        when {
            distance == 0f -> 0f
            distance > 0f -> internalPressure - distance
            else -> -internalPressure - distance
        }

    /**
     * 形变量->计算力
     *
     * @param difDistance 形变距离.
     */
    internal fun calculate(difDistance: Float, elasticityCoefficient: Float): Float =
        difDistance * elasticityCoefficient

    internal fun calculate(difDistance: PointF, elasticityCoefficient: Float): PointF =
        PointF(
            calculate(difDistance.x, elasticityCoefficient),
            calculate(difDistance.y, elasticityCoefficient),
        )

    /**
     * 距离计算加速度.
     *
     * @param difDistance 形变距离.
     */
    internal fun calculate(difDistance: PointF, elasticityCoefficient: Float, m: Float): PointF {
        val newton = calculate(difDistance, elasticityCoefficient)
        return PointF(newton.x / m, newton.y / m)
    }

    /**
     * 计算位移(矢量),
     *
     * @param speed        速度(px/ms)
     * @param acceleration 加速度(px/ms2)
     * @param dt           时间(ms)
     */
    internal fun calculate(speed: PointF, acceleration: PointF, dt: Long): PointF {
        val dtSq = (dt * dt).toDouble()
        val x = ((speed.x * dt).toDouble() + (0.5f * acceleration.x).toDouble() * dtSq).roundToLong() / 1_000_000f
        val y = ((speed.y * dt).toDouble() + (0.5f * acceleration.y).toDouble() * dtSq).roundToLong() / 1_000_000f
        return PointF(x, y)
    }

    internal fun calculateAxisDisplacement(v0: Float, a: Float, dt: Long): Float {
        val dtSq = (dt * dt).toDouble()
        return ((v0 * dt).toDouble() + (0.5f * a).toDouble() * dtSq).roundToLong() / 1_000_000f
    }

    @JvmStatic
    public fun bindDefTouchPlanet(option: PlanetOption) {
        option.itemView().setOnTouchListener(TouchHelper(option))
    }

    @JvmStatic
    public fun concatId(pair: Pair<String, String>): String =
        concatId(pair.first, pair.second)

    @JvmStatic
    public fun concatId(sId: String, eId: String): String =
        if (String.CASE_INSENSITIVE_ORDER.compare(sId, eId) <= 0) sId + eId else eId + sId

    /**
     * planet TouchHelper (single-finger drag, click, and bring-to-front on press).
     */
    public open class TouchHelper(
        private val option: PlanetOption,
    ) : GestureDetector.SimpleOnGestureListener(),
        View.OnTouchListener {

        private val mGestureDetector: GestureDetector =
            GestureDetector(option.itemView().context, this)

        @SuppressLint("ClickableViewAccessibility") // Click accessibility is delegated via GestureDetector.onSingleTapUp -> performClick().
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (!v.isEnabled) {
                return false
            }

            val rawEvent = MotionEvent.obtain(event)
            rawEvent.offsetLocation(event.rawX - event.x, event.rawY - event.y)
            val retVal = mGestureDetector.onTouchEvent(rawEvent)
            rawEvent.recycle()

            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                option.itemView().isPressed = false
            }

            return retVal
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            Log.i(TAG, "[planet]onSingleTapUp() called with: e = [$e]")
            return this.option.itemView().performClick()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            val v = this.option.itemView()
            v.translationX -= distanceX
            v.translationY -= distanceY
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            val v = this.option.itemView()
            v.isPressed = true
            v.bringToFront()
            return true
        }
    }
}
