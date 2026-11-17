package com.cinpe.deponder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.DeponderRubberInsets
import com.cinpe.deponder.core.RubberPhysics
import com.cinpe.deponder.core.RubberPose
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Built-in, ready-to-use spring link [View] built on [deponderRubber].
 *
 * Designed specifically for Android's hardware `RenderNode` pipeline even when the parent
 * `deponderRoot` container retains Android's default `clipChildren = true`:
 * - `RubberViewBinding` scales this view's outer `RenderNode` horizontally across the resolved
 *   [insets] span (`outerScaleX = spanLengthPx / width`), so `[0, width]` maps to the entire link
 *   span without any parent bounds truncation.
 * - Internally, directional arrowheads (`NONE`, `END`, `START`, `BOTH`) and the floating [label]
 *   apply `invRatio = effectiveScale / outerScaleX` so they render on screen at exact undistorted
 *   `(effectiveScale, effectiveScale)` proportions inside `[0, width] x [0, height]`.
 */
public class DeponderRubberView @JvmOverloads public constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density: Float = resources.displayMetrics.density.coerceAtLeast(1e-3f)
    private val scaledDensity: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        1f,
        resources.displayMetrics,
    ).coerceAtLeast(1e-3f)
    private val labelOffsetPx: Float = DeponderDefaults.Rubber.DEFAULT_LABEL_OFFSET_DP * density

    private var _label: String? = null
    private var _arrowMode: DeponderArrowMode = DeponderArrowMode.NONE
    private var _physics: RubberPhysics = RubberPhysics()
    private var _insets: DeponderRubberInsets = DeponderRubberInsets.Default

    @ColorInt
    private var _lineColor: Int = DEFAULT_LINE_COLOR

    private var _lineThicknessPx: Float = DeponderDefaults.Rubber.DEFAULT_THICKNESS_DP * density
    private var _arrowSizePx: Float = DeponderDefaults.Rubber.DEFAULT_ARROW_SIZE_DP * density

    @ColorInt
    private var _labelColor: Int = DEFAULT_LABEL_COLOR

    private var _labelTextSizePx: Float = DeponderDefaults.Rubber.DEFAULT_LABEL_TEXT_SIZE_SP * scaledDensity

    @ColorInt
    private var _labelBackgroundColor: Int = DEFAULT_LABEL_BG_COLOR

    private val stemBaseWidthPx: Int = max(1, (4f * density).roundToInt())
    private val stemView: LineStemView
    private val startArrowView: ArrowHeadView
    private val endArrowView: ArrowHeadView
    private val labelView: FloatingLabelView

    private var lastPose: RubberPose? = null
    private var lastSpanLengthPx: Float = 0f
    private var lastEffectiveScale: Float = 1f
    private var lastOuterScaleX: Float = 1f

    /** Optional text label floating above the spring line (`null` or empty hides the label). */
    public var label: String?
        get() = _label
        set(value) {
            val normalized = value?.takeIf { it.isNotEmpty() }
            if (_label != normalized) {
                _label = normalized
                syncChildMeasurementsAndRedraw()
            }
        }

    /** Directional arrowhead mode (`NONE`, `END`, `START`, `BOTH`). */
    public var arrowMode: DeponderArrowMode
        get() = _arrowMode
        set(value) {
            if (_arrowMode != value) {
                _arrowMode = value
                syncChildMeasurementsAndRedraw()
            }
        }

    /**
     * Pure Hooke spring physics configuration ([RubberPhysics.Default] content-adaptive rest length
     * by default, or initialized from XML `app:deponderElasticity` / `app:deponderNaturalLength`).
     */
    public var physics: RubberPhysics
        get() = _physics
        set(value) {
            if (_physics != value) {
                _physics = value
                val rubberBinding = getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
                rubberBinding?.updatePhysicsOnly(value)
            }
        }

    internal fun syncPhysicsFromBinding(value: RubberPhysics) {
        _physics = value
    }

    /**
     * Endpoint safe-area clearance ([DeponderRubberInsets.Default] contour avoidance by default,
     * [DeponderRubberInsets.fixed] for explicit clearance distances, or [DeponderRubberInsets.None]
     * for continuous center-to-center lines). Center-anchored ([DeponderRubberInsets.Mode.NONE])
     * endpoints automatically omit directional arrowheads.
     */
    public var insets: DeponderRubberInsets
        get() = _insets
        set(value) {
            if (_insets != value) {
                val hadArrows = hasEffectiveStartArrow() || hasEffectiveEndArrow()
                _insets = value
                val hasArrowsNow = hasEffectiveStartArrow() || hasEffectiveEndArrow()
                if (hadArrows != hasArrowsNow) {
                    requestLayout()
                }
                val rubberBinding = getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
                if (rubberBinding != null) {
                    rubberBinding.updateInsetsOnly(value)
                } else {
                    applySubViewTransforms(lastPose, lastSpanLengthPx, lastEffectiveScale, lastOuterScaleX)
                }
            }
        }

    internal fun syncInsetsFromBinding(value: DeponderRubberInsets) {
        if (_insets != value) {
            val hadArrows = hasEffectiveStartArrow() || hasEffectiveEndArrow()
            _insets = value
            val hasArrowsNow = hasEffectiveStartArrow() || hasEffectiveEndArrow()
            if (hadArrows != hasArrowsNow) {
                requestLayout()
            }
        }
    }

    private fun hasEffectiveStartArrow(): Boolean =
        (_arrowMode == DeponderArrowMode.START || _arrowMode == DeponderArrowMode.BOTH) && _insets.hasStartSafeArea

    private fun hasEffectiveEndArrow(): Boolean =
        (_arrowMode == DeponderArrowMode.END || _arrowMode == DeponderArrowMode.BOTH) && _insets.hasEndSafeArea

    /** ARGB color of the spring line and directional arrowheads. */
    @get:ColorInt
    public var lineColor: Int
        get() = _lineColor
        set(@ColorInt value) {
            if (_lineColor != value) {
                _lineColor = value
                syncLineColors()
                applySubViewTransforms(lastPose, lastSpanLengthPx, lastEffectiveScale, lastOuterScaleX)
            }
        }

    /** Line stroke thickness in `dp`. */
    public var lineThicknessDp: Float
        get() = _lineThicknessPx / density
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "lineThicknessDp must be finite and >= 0f, but was $value"
            }
            val px = (value * density).coerceIn(1f, MAX_STYLE_DIMENSION_PX)
            if (_lineThicknessPx != px) {
                _lineThicknessPx = px
                syncChildMeasurementsAndRedraw()
            }
        }

    /** Arrowhead length in `dp`. */
    public var arrowSizeDp: Float
        get() = _arrowSizePx / density
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "arrowSizeDp must be finite and >= 0f, but was $value"
            }
            val px = (value * density).coerceIn(0f, MAX_STYLE_DIMENSION_PX)
            if (_arrowSizePx != px) {
                _arrowSizePx = px
                syncChildMeasurementsAndRedraw()
            }
        }

    /** ARGB text color of the floating [label]. */
    @get:ColorInt
    public var labelColor: Int
        get() = _labelColor
        set(@ColorInt value) {
            if (_labelColor != value) {
                _labelColor = value
                labelView.syncPaints()
                labelView.invalidate()
            }
        }

    /** Text size of the floating [label] in `sp`. */
    public var labelTextSizeSp: Float
        get() = _labelTextSizePx / scaledDensity
        set(value) {
            require(value.isFinite() && value > 0f) {
                "labelTextSizeSp must be finite and > 0f, but was $value"
            }
            val px = (value * scaledDensity).coerceIn(1f, MAX_STYLE_DIMENSION_PX)
            if (_labelTextSizePx != px) {
                _labelTextSizePx = px
                syncChildMeasurementsAndRedraw()
            }
        }

    /** Optional ARGB background color behind [label] (`0x00000000` transparent by default). */
    @get:ColorInt
    public var labelBackgroundColor: Int
        get() = _labelBackgroundColor
        set(@ColorInt value) {
            if (_labelBackgroundColor != value) {
                _labelBackgroundColor = value
                syncChildMeasurementsAndRedraw()
            }
        }

    init {
        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.TRANSPARENT)

        var xmlStartId: String? = null
        var xmlEndId: String? = null
        var xmlElasticity: Float = DeponderDefaults.Rubber.ELASTICITY
        var xmlNaturalLengthDp: Float = DeponderDefaults.AUTO_LENGTH

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.DeponderRubberView, defStyleAttr, 0)
            try {
                xmlStartId = a.getString(R.styleable.DeponderRubberView_deponderStartId)
                xmlEndId = a.getString(R.styleable.DeponderRubberView_deponderEndId)
                xmlElasticity = a.getFloat(
                    R.styleable.DeponderRubberView_deponderElasticity,
                    DeponderDefaults.Rubber.ELASTICITY,
                )
                xmlNaturalLengthDp = if (
                    a.getType(R.styleable.DeponderRubberView_deponderNaturalLength) == TypedValue.TYPE_DIMENSION
                ) {
                    val naturalPx = a.getDimension(R.styleable.DeponderRubberView_deponderNaturalLength, 0f)
                    (naturalPx / density).coerceAtLeast(0f)
                } else {
                    DeponderDefaults.AUTO_LENGTH
                }
                val gapDp = if (a.hasValue(R.styleable.DeponderRubberView_deponderInsetGap)) {
                    val gapPx = a.getDimension(
                        R.styleable.DeponderRubberView_deponderInsetGap,
                        DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP * density,
                    )
                    gapPx / density
                } else {
                    DeponderDefaults.Rubber.DEFAULT_ENDPOINT_GAP_DP
                }
                _insets = if (a.getType(R.styleable.DeponderRubberView_deponderInsets) == TypedValue.TYPE_DIMENSION) {
                    val fixedPx = a.getDimension(R.styleable.DeponderRubberView_deponderInsets, 0f)
                    DeponderRubberInsets.fixed((fixedPx / density).coerceAtLeast(0f))
                } else {
                    when (a.getInt(R.styleable.DeponderRubberView_deponderInsets, 0)) {
                        1 -> DeponderRubberInsets.None
                        2 -> DeponderRubberInsets.startOnly(gapDp)
                        3 -> DeponderRubberInsets.endOnly(gapDp)
                        else -> DeponderRubberInsets.contour(gapDp)
                    }
                }
                _label = a.getString(R.styleable.DeponderRubberView_deponderLabel)?.takeIf { it.isNotEmpty() }
                _arrowMode = when (a.getInt(R.styleable.DeponderRubberView_deponderArrowMode, 0)) {
                    1 -> DeponderArrowMode.END
                    2 -> DeponderArrowMode.START
                    3 -> DeponderArrowMode.BOTH
                    else -> DeponderArrowMode.NONE
                }
                _lineColor = a.getColor(R.styleable.DeponderRubberView_deponderLineColor, DEFAULT_LINE_COLOR)
                _lineThicknessPx = a.getDimension(
                    R.styleable.DeponderRubberView_deponderLineThickness,
                    DeponderDefaults.Rubber.DEFAULT_THICKNESS_DP * density,
                ).coerceAtLeast(1f)
                _arrowSizePx = a.getDimension(
                    R.styleable.DeponderRubberView_deponderArrowSize,
                    DeponderDefaults.Rubber.DEFAULT_ARROW_SIZE_DP * density,
                ).coerceAtLeast(0f)
                _labelColor = a.getColor(R.styleable.DeponderRubberView_deponderLabelColor, DEFAULT_LABEL_COLOR)
                _labelTextSizePx = a.getDimension(
                    R.styleable.DeponderRubberView_deponderLabelTextSize,
                    DeponderDefaults.Rubber.DEFAULT_LABEL_TEXT_SIZE_SP * scaledDensity,
                ).coerceAtLeast(1f)
                _labelBackgroundColor = a.getColor(
                    R.styleable.DeponderRubberView_deponderLabelBackgroundColor,
                    DEFAULT_LABEL_BG_COLOR,
                )
            } finally {
                a.recycle()
            }
        }

        stemView = LineStemView(context, _lineColor)
        startArrowView = ArrowHeadView(context, _lineColor, pointsRight = false)
        endArrowView = ArrowHeadView(context, _lineColor, pointsRight = true)
        labelView = FloatingLabelView(context)

        addView(
            stemView,
            LayoutParams(stemBaseWidthPx, max(1, _lineThicknessPx.roundToInt()), Gravity.CENTER),
        )
        val initArrowW = max(1, ceil(_arrowSizePx).toInt())
        val initArrowH = max(1, ceil(_arrowSizePx * ARROW_HEIGHT_RATIO).toInt())
        addView(startArrowView, LayoutParams(initArrowW, initArrowH, Gravity.CENTER))
        addView(endArrowView, LayoutParams(initArrowW, initArrowH, Gravity.CENTER))
        addView(
            labelView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )

        syncChildMeasurementsAndRedraw()

        _physics = RubberPhysics(
            elasticity = xmlElasticity,
            naturalLengthDp = xmlNaturalLengthDp,
        )

        if (!xmlStartId.isNullOrEmpty() && !xmlEndId.isNullOrEmpty()) {
            deponderRubber(
                startId = xmlStartId,
                endId = xmlEndId,
                physics = _physics,
                insets = _insets,
            )
        }
    }

    /** Fluent Java/Kotlin helper to configure label text. */
    public fun withLabel(label: String?): DeponderRubberView = apply {
        this.label = label
    }

    /** Fluent Java/Kotlin helper to configure directional [DeponderArrowMode]. */
    public fun withArrowMode(arrowMode: DeponderArrowMode): DeponderRubberView = apply {
        this.arrowMode = arrowMode
    }

    /** Fluent Java/Kotlin helper to configure spring [RubberPhysics]. */
    public fun withPhysics(physics: RubberPhysics): DeponderRubberView = apply {
        this.physics = physics
    }

    /** Fluent Java/Kotlin helper to configure endpoint [DeponderRubberInsets]. */
    public fun withInsets(insets: DeponderRubberInsets): DeponderRubberView = apply {
        this.insets = insets
    }

    /** Fluent Java/Kotlin helper to configure line color and thickness in `dp`. */
    @JvmOverloads
    public fun withLineStyle(
        @ColorInt color: Int,
        thicknessDp: Float = this.lineThicknessDp,
    ): DeponderRubberView = apply {
        this.lineColor = color
        this.lineThicknessDp = thicknessDp
    }

    /** Fluent Java/Kotlin helper to configure floating label colors and text size in `sp`. */
    @JvmOverloads
    public fun withLabelStyle(
        @ColorInt textColor: Int,
        textSizeSp: Float = this.labelTextSizeSp,
        @ColorInt backgroundColor: Int = this.labelBackgroundColor,
    ): DeponderRubberView = apply {
        this.labelColor = textColor
        this.labelTextSizeSp = textSizeSp
        this.labelBackgroundColor = backgroundColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        labelView.recomputeLabelMetrics()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val pillW = labelView.measuredLabelWidthPx
        val pillH = labelView.measuredLabelHeightPx
        val hasStart = hasEffectiveStartArrow()
        val hasEnd = hasEffectiveEndArrow()
        val arrowH = if (hasStart || hasEnd) ceil(_arrowSizePx * ARROW_HEIGHT_RATIO).toInt() else 0
        val activeArrowsW = (if (hasStart) ceil(_arrowSizePx).toInt() else 0) +
            (if (hasEnd) ceil(_arrowSizePx).toInt() else 0)
        val vertMargin = 2f * density
        val symmetricTopClearance = if (pillH > 0) {
            ceil(_lineThicknessPx + (labelOffsetPx + pillH + vertMargin) * 2f).toInt()
        } else {
            0
        }
        val desiredW = max(stemBaseWidthPx, max(pillW, activeArrowsW))
        val desiredH = max(
            max(1, ceil(_lineThicknessPx + vertMargin * 2f).toInt()),
            max(ceil(arrowH + vertMargin * 2f).toInt(), symmetricTopClearance),
        )
        setMeasuredDimension(
            resolveSize(desiredW, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val pose = lastPose
        if (pose != null && pose.isActive && lastSpanLengthPx > 0f && width > 0) {
            val recomputedOuterScaleX = if (lastEffectiveScale > 0f) lastSpanLengthPx / width.toFloat() else 0f
            lastOuterScaleX = recomputedOuterScaleX
            scaleX = recomputedOuterScaleX
        }
        applySubViewTransforms(pose, lastSpanLengthPx, lastEffectiveScale, lastOuterScaleX)
    }

    internal fun nonStretchedContentWidthPx(): Int = labelView.measuredLabelWidthPx

    internal fun adaptiveContentWidthPx(): Int {
        val declaredArrowsCount = when (_arrowMode) {
            DeponderArrowMode.NONE -> 0
            DeponderArrowMode.END, DeponderArrowMode.START -> 1
            DeponderArrowMode.BOTH -> 2
        }
        val arrowW = if (_arrowSizePx > 0f) ceil(_arrowSizePx).toInt() else 0
        return labelView.measuredLabelWidthPx + declaredArrowsCount * arrowW
    }

    internal val isStartArrowRendered: Boolean
        get() = startArrowView.scaleX > 0f && startArrowView.scaleY > 0f

    internal val isEndArrowRendered: Boolean
        get() = endArrowView.scaleX > 0f && endArrowView.scaleY > 0f

    internal fun applyDefaultRubberPose(
        pose: RubberPose?,
        spanLengthPx: Float,
        effectiveScale: Float,
        outerScaleX: Float,
    ) {
        lastPose = pose
        lastSpanLengthPx = spanLengthPx
        lastEffectiveScale = effectiveScale
        lastOuterScaleX = outerScaleX
        applySubViewTransforms(pose, spanLengthPx, effectiveScale, outerScaleX)
    }

    internal fun resetDefaultRubberPose() {
        lastPose = null
        lastSpanLengthPx = 0f
        lastEffectiveScale = 1f
        lastOuterScaleX = 1f
        stemView.scaleX = 1f
        stemView.translationX = 0f
        stemView.translationY = 0f
        startArrowView.scaleX = 0f
        startArrowView.scaleY = 0f
        startArrowView.translationX = 0f
        startArrowView.translationY = 0f
        endArrowView.scaleX = 0f
        endArrowView.scaleY = 0f
        endArrowView.translationX = 0f
        endArrowView.translationY = 0f
        labelView.scaleX = 1f
        labelView.scaleY = 1f
        updateFloatingLabelOffset(invRatio = 1f)
    }

    private fun subPixelCenterFixX(child: View, fallbackW: Float): Float {
        if (width <= 0) return 0f
        val cw = if (child.width > 0) child.width.toFloat() else fallbackW
        return width * 0.5f - (child.left + cw * 0.5f)
    }

    private fun subPixelCenterFixY(child: View, fallbackH: Float): Float {
        if (height <= 0) return 0f
        val ch = if (child.height > 0) child.height.toFloat() else fallbackH
        return height * 0.5f - (child.top + ch * 0.5f)
    }

    private fun applySubViewTransforms(
        pose: RubberPose?,
        spanLengthPx: Float,
        effectiveScale: Float,
        outerScaleX: Float,
    ) {
        if (pose == null || !pose.isActive || spanLengthPx <= 0f || effectiveScale <= 1e-4f || outerScaleX <= 1e-6f) {
            stemView.scaleX = 0f
            stemView.translationX = 0f
            startArrowView.scaleX = 0f
            startArrowView.scaleY = 0f
            endArrowView.scaleX = 0f
            endArrowView.scaleY = 0f
            labelView.scaleX = 0f
            labelView.scaleY = 0f
            return
        }

        // Convert screen-proportional local span (`spanLengthPx / effectiveScale`) into this View's
        // `[0, width]` pre-transform coordinate space (`invRatio = effectiveScale / outerScaleX`),
        // guaranteeing 100% of stem, arrows, and text stay strictly inside `[0, width]`!
        val invRatio = effectiveScale / outerScaleX
        updateFloatingLabelOffset(invRatio)

        val localSpan = spanLengthPx / effectiveScale
        val hasStart = hasEffectiveStartArrow()
        val hasEnd = hasEffectiveEndArrow()
        val arrowW = if (startArrowView.width > 0) startArrowView.width.toFloat() else ceil(_arrowSizePx)
        val arrowH = if (startArrowView.height > 0) {
            startArrowView.height.toFloat()
        } else {
            ceil(_arrowSizePx * ARROW_HEIGHT_RATIO)
        }

        val minArrowSpan = (if (hasStart) arrowW else 0f) + (if (hasEnd) arrowW else 0f)
        val arrowScale = if (_arrowSizePx <= 0f || localSpan <= 0f) {
            0f
        } else if (minArrowSpan > 0f && localSpan < minArrowSpan) {
            (localSpan / minArrowSpan).coerceIn(0f, 1f)
        } else {
            1f
        }
        val effArrowW = arrowW * arrowScale
        val effArrowH = arrowH * arrowScale

        if (hasStart && effArrowW > 0f) {
            startArrowView.pivotX = arrowW * 0.5f
            startArrowView.pivotY = arrowH * 0.5f
            startArrowView.scaleX = arrowScale * invRatio
            startArrowView.scaleY = arrowScale
            startArrowView.translationX =
                subPixelCenterFixX(startArrowView, arrowW) + (-localSpan * 0.5f + effArrowW * 0.5f) * invRatio
            startArrowView.translationY = subPixelCenterFixY(startArrowView, arrowH)
        } else {
            startArrowView.scaleX = 0f
            startArrowView.scaleY = 0f
            startArrowView.translationX = 0f
        }

        if (hasEnd && effArrowW > 0f) {
            val endW = if (endArrowView.width > 0) endArrowView.width.toFloat() else arrowW
            val endH = if (endArrowView.height > 0) endArrowView.height.toFloat() else arrowH
            val effEndW = endW * arrowScale
            endArrowView.pivotX = endW * 0.5f
            endArrowView.pivotY = endH * 0.5f
            endArrowView.scaleX = arrowScale * invRatio
            endArrowView.scaleY = arrowScale
            endArrowView.translationX =
                subPixelCenterFixX(endArrowView, endW) + (localSpan * 0.5f - effEndW * 0.5f) * invRatio
            endArrowView.translationY = subPixelCenterFixY(endArrowView, endH)
        } else {
            endArrowView.scaleX = 0f
            endArrowView.scaleY = 0f
            endArrowView.translationX = 0f
        }

        val isOpaque = Color.alpha(_lineColor) == 255
        val startInset = if (hasStart && effArrowW > 0f) {
            computeArrowStemInset(effArrowW, effArrowH, _lineThicknessPx, isOpaque)
        } else {
            0f
        }
        val endInset = if (hasEnd && effArrowW > 0f) {
            computeArrowStemInset(effArrowW, effArrowH, _lineThicknessPx, isOpaque)
        } else {
            0f
        }

        val stemLocalLen = max(0f, localSpan - startInset - endInset)
        val stemW = (if (stemView.width > 0) stemView.width else stemBaseWidthPx).toFloat()
        val stemH = (if (stemView.height > 0) stemView.height else max(1, _lineThicknessPx.roundToInt())).toFloat()
        stemView.pivotX = stemW * 0.5f
        stemView.pivotY = stemH * 0.5f
        stemView.scaleX = if (stemW > 0f && stemLocalLen > 0f) (stemLocalLen * invRatio) / stemW else 0f
        stemView.scaleY = 1f
        stemView.translationX = subPixelCenterFixX(stemView, stemW) + ((startInset - endInset) * 0.5f) * invRatio
        stemView.translationY = subPixelCenterFixY(stemView, stemH)
    }

    private fun updateFloatingLabelOffset(invRatio: Float) {
        val pillW = labelView.measuredLabelWidthPx.toFloat()
        val pillH = labelView.measuredLabelHeightPx.toFloat()
        if (pillW > 0f && pillH > 0f) {
            labelView.pivotX = pillW * 0.5f
            labelView.pivotY = pillH * 0.5f
            labelView.scaleX = invRatio
            labelView.scaleY = 1f
            labelView.translationX = subPixelCenterFixX(labelView, pillW)
            labelView.translationY =
                subPixelCenterFixY(labelView, pillH) - (_lineThicknessPx * 0.5f + labelOffsetPx + pillH * 0.5f)
        } else {
            labelView.scaleX = 0f
            labelView.scaleY = 0f
            labelView.translationX = 0f
            labelView.translationY = 0f
        }
    }

    private fun syncLineColors() {
        stemView.updateColor(_lineColor)
        startArrowView.updateColor(_lineColor)
        endArrowView.updateColor(_lineColor)
    }

    private fun syncChildMeasurementsAndRedraw() {
        val nextStemH = max(1, _lineThicknessPx.roundToInt())
        val stemLp = stemView.layoutParams as LayoutParams
        if (stemLp.height != nextStemH) {
            stemLp.height = nextStemH
            stemView.layoutParams = stemLp
        }

        val nextArrowW = max(1, ceil(_arrowSizePx).toInt())
        val nextArrowH = max(1, ceil(_arrowSizePx * ARROW_HEIGHT_RATIO).toInt())
        val startLp = startArrowView.layoutParams as LayoutParams
        if (startLp.width != nextArrowW || startLp.height != nextArrowH) {
            startLp.width = nextArrowW
            startLp.height = nextArrowH
            startArrowView.layoutParams = startLp
        }
        val endLp = endArrowView.layoutParams as LayoutParams
        if (endLp.width != nextArrowW || endLp.height != nextArrowH) {
            endLp.width = nextArrowW
            endLp.height = nextArrowH
            endArrowView.layoutParams = endLp
        }

        syncLineColors()
        startArrowView.invalidate()
        endArrowView.invalidate()
        labelView.recomputeLabelMetrics()
        labelView.requestLayout()
        labelView.invalidate()
        requestLayout()
        val rubberBinding = getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
        if (rubberBinding?.attachedController != null) {
            rubberBinding.syncAdaptiveLengthAndRefreshPose()
        } else {
            applySubViewTransforms(lastPose, lastSpanLengthPx, lastEffectiveScale, lastOuterScaleX)
        }
    }

    private class LineStemView(
        context: Context,
        @ColorInt initialColor: Int,
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = initialColor
        }

        fun updateColor(@ColorInt color: Int) {
            if (paint.color != color) {
                paint.color = color
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    private class ArrowHeadView(
        context: Context,
        @ColorInt initialColor: Int,
        private val pointsRight: Boolean,
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = initialColor
        }
        private val path = Path()

        fun updateColor(@ColorInt color: Int) {
            if (paint.color != color) {
                paint.color = color
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            path.reset()
            if (pointsRight) {
                path.moveTo(w, h * 0.5f)
                path.lineTo(0f, 0f)
                path.lineTo(0f, h)
            } else {
                path.moveTo(0f, h * 0.5f)
                path.lineTo(w, 0f)
                path.lineTo(w, h)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private inner class FloatingLabelView(context: Context) : View(context) {
        private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        private val pillRect = RectF()

        var measuredLabelWidthPx: Int = 0
            private set
        var measuredLabelHeightPx: Int = 0
            private set

        fun syncPaints() {
            pillPaint.color = _labelBackgroundColor
            textPaint.color = _labelColor
            textPaint.textSize = _labelTextSizePx
        }

        fun recomputeLabelMetrics() {
            syncPaints()
            val text = _label
            if (text.isNullOrEmpty()) {
                measuredLabelWidthPx = 0
                measuredLabelHeightPx = 0
                return
            }
            val hasBg = Color.alpha(_labelBackgroundColor) > 0
            val horizPad = if (hasBg) 5f * density else 2f * density
            val vertPad = if (hasBg) 1.5f * density else 0f
            val fm = textPaint.fontMetrics
            val textW = ceil(textPaint.measureText(text))
            val textH = ceil(fm.descent - fm.ascent)
            measuredLabelWidthPx = max(1, ceil(textW + horizPad * 2f).toInt())
            measuredLabelHeightPx = max(1, ceil(textH + vertPad * 2f).toInt())
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            recomputeLabelMetrics()
            setMeasuredDimension(
                resolveSize(measuredLabelWidthPx, widthMeasureSpec),
                resolveSize(measuredLabelHeightPx, heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val text = _label
            if (text.isNullOrEmpty() || width <= 0 || height <= 0) return
            syncPaints()

            if (Color.alpha(_labelBackgroundColor) > 0) {
                val corner = height * 0.5f
                pillRect.set(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(pillRect, corner, corner, pillPaint)
            }

            val centerX = width * 0.5f
            val centerY = height * 0.5f
            val fm = textPaint.fontMetrics
            val baselineY = centerY - (fm.ascent + fm.descent) * 0.5f
            canvas.drawText(text, centerX, baselineY, textPaint)
        }
    }

    public companion object {
        private const val ARROW_HEIGHT_RATIO: Float = 1.35f
        private const val ARROW_STEM_INSET_RATIO: Float = 0.75f
        private const val MAX_STYLE_DIMENSION_PX: Float = 16_384f

        public const val DEFAULT_LINE_COLOR: Int = 0xFF78909C.toInt()
        public const val DEFAULT_LABEL_COLOR: Int = 0xFF37474F.toInt()
        public const val DEFAULT_LABEL_BG_COLOR: Int = 0x00000000

        /**
         * Insets the rectangular line stem cleanly into the flat base of the arrowhead so zero line pixels
         * ever poke through the sharp vertex (meeting flush at `effArrowW` when translucent or extra-thick).
         */
        internal fun computeArrowStemInset(
            effArrowW: Float,
            effArrowH: Float,
            lineThicknessPx: Float,
            isOpaque: Boolean = true,
        ): Float {
            if (effArrowW <= 0f) return 0f
            if (!isOpaque) return effArrowW
            val heightAtInset = effArrowH * ARROW_STEM_INSET_RATIO
            return if (lineThicknessPx <= heightAtInset * 0.9f) {
                effArrowW * ARROW_STEM_INSET_RATIO
            } else {
                effArrowW
            }
        }
    }
}
