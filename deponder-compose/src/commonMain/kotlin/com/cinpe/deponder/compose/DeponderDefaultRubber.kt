package com.cinpe.deponder.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinpe.deponder.core.DeponderArrowMode
import com.cinpe.deponder.core.DeponderDefaults
import com.cinpe.deponder.core.RubberPhysics
import kotlin.jvm.JvmName
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private const val ARROW_HEIGHT_RATIO: Float = 1.35f
private const val ARROW_STEM_INSET_RATIO: Float = 0.75f

/**
 * Immutable visual style configuration for [DeponderDefaultRubber].
 *
 * Construct customized instances via [DeponderRubberStyle.of] or use [DeponderRubberStyle.Default].
 */
@Immutable
public class DeponderRubberStyle internal constructor(
    @get:JvmName("lineColorArgb")
    public val lineColor: Color,
    @get:JvmName("lineThicknessDp")
    public val lineThickness: Dp,
    @get:JvmName("arrowSizeDp")
    public val arrowSize: Dp,
    @get:JvmName("labelColorArgb")
    public val labelColor: Color,
    @get:JvmName("labelTextSizePacked")
    public val labelTextSize: TextUnit,
    @get:JvmName("labelBackgroundColorArgb")
    public val labelBackgroundColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeponderRubberStyle) return false
        return lineColor == other.lineColor &&
            lineThickness == other.lineThickness &&
            arrowSize == other.arrowSize &&
            labelColor == other.labelColor &&
            labelTextSize == other.labelTextSize &&
            labelBackgroundColor == other.labelBackgroundColor
    }

    override fun hashCode(): Int {
        var result = lineColor.hashCode()
        result = 31 * result + lineThickness.hashCode()
        result = 31 * result + arrowSize.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + labelTextSize.hashCode()
        result = 31 * result + labelBackgroundColor.hashCode()
        return result
    }

    override fun toString(): String =
        "DeponderRubberStyle(lineColor=$lineColor, lineThickness=$lineThickness, arrowSize=$arrowSize, " +
            "labelColor=$labelColor, labelTextSize=$labelTextSize, " +
            "labelBackgroundColor=$labelBackgroundColor)"

    public companion object {
        @get:JvmName("defaultLineColorArgb")
        public val DefaultLineColor: Color = Color(0xFF78909C)

        @get:JvmName("defaultLabelColorArgb")
        public val DefaultLabelColor: Color = Color(0xFF37474F)

        @get:JvmName("defaultLabelBackgroundColorArgb")
        public val DefaultLabelBackgroundColor: Color = Color.Transparent

        public val Default: DeponderRubberStyle = DeponderRubberStyle(
            lineColor = DefaultLineColor,
            lineThickness = DeponderDefaults.Rubber.DEFAULT_THICKNESS_DP.dp,
            arrowSize = DeponderDefaults.Rubber.DEFAULT_ARROW_SIZE_DP.dp,
            labelColor = DefaultLabelColor,
            labelTextSize = DeponderDefaults.Rubber.DEFAULT_LABEL_TEXT_SIZE_SP.sp,
            labelBackgroundColor = DefaultLabelBackgroundColor,
        )

        /**
         * Creates a customized [DeponderRubberStyle] for [DeponderDefaultRubber].
         */
        @JvmName("create")
        public fun of(
            lineColor: Color = DefaultLineColor,
            lineThickness: Dp = DeponderDefaults.Rubber.DEFAULT_THICKNESS_DP.dp,
            arrowSize: Dp = DeponderDefaults.Rubber.DEFAULT_ARROW_SIZE_DP.dp,
            labelColor: Color = DefaultLabelColor,
            labelTextSize: TextUnit = DeponderDefaults.Rubber.DEFAULT_LABEL_TEXT_SIZE_SP.sp,
            labelBackgroundColor: Color = DefaultLabelBackgroundColor,
        ): DeponderRubberStyle = DeponderRubberStyle(
            lineColor = lineColor,
            lineThickness = lineThickness,
            arrowSize = arrowSize,
            labelColor = labelColor,
            labelTextSize = labelTextSize,
            labelBackgroundColor = labelBackgroundColor,
        )
    }
}

/**
 * Built-in, zero-`compose-foundation` spring link component built purely on [deponderRubber] + GPU
 * `placeWithLayer` transforms (0 recompositions, 0 `DrawScope` re-recordings per physics frame, and
 * zero `CompositingStrategy.Offscreen` bounds clipping).
 *
 * Renders:
 * 1. Endpoint-anchored directional arrowheads ([DeponderArrowMode]: `NONE`, `END`, `START`, `BOTH`) placed
 *    cleanly at the contour-cleared link endpoints, with the line stem inset inside arrow bases so zero line
 *    pixels ever protrude through arrow tips.
 * 2. An optional floating [label] positioned cleanly above the continuous spring line with a fully transparent
 *    default background (`Color.Transparent`) so crossing links are never masked.
 */
@Composable
public fun DeponderDefaultRubber(
    startId: String,
    endId: String,
    modifier: Modifier = Modifier,
    physics: RubberPhysics = RubberPhysics(),
    label: String? = null,
    arrowMode: DeponderArrowMode = DeponderArrowMode.NONE,
    style: DeponderRubberStyle = DeponderRubberStyle.Default,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val nonBlankLabel = label?.takeIf { it.isNotEmpty() }

    val textLayoutResult: TextLayoutResult? = remember(
        nonBlankLabel,
        textMeasurer,
        style.labelTextSize,
        style.labelColor,
        density,
    ) {
        nonBlankLabel?.let { text ->
            textMeasurer.measure(
                text = text,
                style = TextStyle(
                    color = style.labelColor,
                    fontSize = style.labelTextSize,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }

    val stemBaseWidthPx = max(1, with(density) { 4.dp.roundToPx() })
    val lineThicknessPx = max(1, with(density) { style.lineThickness.roundToPx() })
    val arrowLengthPx = with(density) { style.arrowSize.toPx() }.coerceAtLeast(0f)
    val arrowWidthPx = max(1, ceil(arrowLengthPx).toInt())
    val arrowHeightPx = max(1, ceil(arrowLengthPx * ARROW_HEIGHT_RATIO).toInt())
    val labelOffsetPx = with(density) { DeponderDefaults.Rubber.DEFAULT_LABEL_OFFSET_DP.dp.toPx() }

    val hasBg = style.labelBackgroundColor.alpha > 0f
    val horizPadPx = with(density) { (if (hasBg) 5.dp else 2.dp).toPx() }
    val vertPadPx = with(density) { (if (hasBg) 1.5.dp else 0.dp).toPx() }

    val pillWidthPx = if (textLayoutResult != null) {
        max(1, textLayoutResult.size.width + (horizPadPx * 2f).roundToInt())
    } else {
        0
    }
    val pillHeightPx = if (textLayoutResult != null) {
        max(1, textLayoutResult.size.height + (vertPadPx * 2f).roundToInt())
    } else {
        0
    }

    val isOpaqueLine = style.lineColor.alpha >= 0.999f
    val hasStartArrow = arrowMode == DeponderArrowMode.START || arrowMode == DeponderArrowMode.BOTH
    val hasEndArrow = arrowMode == DeponderArrowMode.END || arrowMode == DeponderArrowMode.BOTH

    val symmetricLabelHeight = if (pillHeightPx > 0) {
        ceil(lineThicknessPx + (labelOffsetPx + pillHeightPx) * 2f).toInt()
    } else {
        0
    }
    val containerWidth = max(stemBaseWidthPx, max(pillWidthPx, arrowWidthPx * 2))
    val containerHeight = max(
        lineThicknessPx,
        max(if (hasStartArrow || hasEndArrow) arrowHeightPx else 0, symmetricLabelHeight),
    )

    val holder = remember { RubberRefHolder() }

    Layout(
        modifier = modifier
            .deponderRubber(startId = startId, endId = endId, physics = physics)
            .then(DefaultRubberBindingElement(holder, pillWidthPx)),
        content = {
            // Child 0: Direct stretched line stem (never wrapped in CompositingStrategy.Offscreen!)
            Layout(modifier = SolidRectDrawElement(style.lineColor)) { _, _ ->
                layout(stemBaseWidthPx, lineThicknessPx) {}
            }
            // Child 1: Start arrow (points left)
            Layout(modifier = ArrowHeadDrawElement(style.lineColor, pointsRight = false)) { _, _ ->
                layout(arrowWidthPx, arrowHeightPx) {}
            }
            // Child 2: End arrow (points right)
            Layout(modifier = ArrowHeadDrawElement(style.lineColor, pointsRight = true)) { _, _ ->
                layout(arrowWidthPx, arrowHeightPx) {}
            }
            // Child 3: Floating label cleanly above the spring line
            Layout(
                modifier = FloatingLabelDrawElement(
                    pillBackgroundColor = style.labelBackgroundColor,
                    textLayoutResult = textLayoutResult,
                ),
            ) { _, _ ->
                layout(pillWidthPx, pillHeightPx) {}
            }
        },
    ) { measurables, _ ->
        val stemP = measurables[0].measure(Constraints.fixed(stemBaseWidthPx, lineThicknessPx))
        val startP = measurables[1].measure(Constraints.fixed(arrowWidthPx, arrowHeightPx))
        val endP = measurables[2].measure(Constraints.fixed(arrowWidthPx, arrowHeightPx))
        val labelP = measurables[3].measure(Constraints.fixed(pillWidthPx, pillHeightPx))

        layout(containerWidth, containerHeight) {
            val stemLeft = (containerWidth - stemP.width) / 2
            val stemTop = (containerHeight - stemP.height) / 2
            val stemFixX = containerWidth * 0.5f - (stemLeft + stemP.width * 0.5f)
            val stemFixY = containerHeight * 0.5f - (stemTop + stemP.height * 0.5f)

            stemP.placeWithLayer(stemLeft, stemTop) {
                val rubber = holder.rubberNode
                rubber?.attachedState?.frameTick
                val pose = rubber?.boundPose
                transformOrigin = TransformOrigin.Center
                if (rubber == null || pose == null || !pose.isActive || pose.length <= 0f) {
                    scaleX = 0f
                    translationX = 0f
                } else {
                    val effScale = rubber.computeEffectiveContainerScale(pose)
                    val localSpan = if (effScale > 1e-4f) pose.length / effScale else 0f
                    val arrW = startP.width.toFloat()
                    val arrH = startP.height.toFloat()
                    val arrScale = computeComposeArrowScale(localSpan, arrW, hasStartArrow, hasEndArrow)
                    val effArrW = arrW * arrScale
                    val effArrH = arrH * arrScale
                    val sInset = if (hasStartArrow) {
                        computeComposeArrowStemInset(effArrW, effArrH, lineThicknessPx.toFloat(), isOpaqueLine)
                    } else {
                        0f
                    }
                    val eInset = if (hasEndArrow) {
                        computeComposeArrowStemInset(effArrW, effArrH, lineThicknessPx.toFloat(), isOpaqueLine)
                    } else {
                        0f
                    }
                    val stemLen = max(0f, localSpan - sInset - eInset)
                    scaleX = if (stemP.width > 0 && stemLen > 0f) stemLen / stemP.width.toFloat() else 0f
                    scaleY = 1f
                    translationX = stemFixX + (sInset - eInset) * 0.5f
                    translationY = stemFixY
                }
            }

            val startLeft = (containerWidth - startP.width) / 2
            val startTop = (containerHeight - startP.height) / 2
            val startFixX = containerWidth * 0.5f - (startLeft + startP.width * 0.5f)
            val startFixY = containerHeight * 0.5f - (startTop + startP.height * 0.5f)

            startP.placeWithLayer(startLeft, startTop) {
                val rubber = holder.rubberNode
                rubber?.attachedState?.frameTick
                val pose = rubber?.boundPose
                transformOrigin = TransformOrigin.Center
                if (!hasStartArrow || arrowLengthPx <= 0f || rubber == null || pose == null || !pose.isActive || pose.length <= 0f) {
                    scaleX = 0f
                    scaleY = 0f
                    translationX = 0f
                } else {
                    val effScale = rubber.computeEffectiveContainerScale(pose)
                    val localSpan = if (effScale > 1e-4f) pose.length / effScale else 0f
                    val arrW = startP.width.toFloat()
                    val arrScale = computeComposeArrowScale(localSpan, arrW, hasStartArrow, hasEndArrow)
                    val effArrW = arrW * arrScale
                    scaleX = arrScale
                    scaleY = arrScale
                    translationX = startFixX - localSpan * 0.5f + effArrW * 0.5f
                    translationY = startFixY
                }
            }

            val endLeft = (containerWidth - endP.width) / 2
            val endTop = (containerHeight - endP.height) / 2
            val endFixX = containerWidth * 0.5f - (endLeft + endP.width * 0.5f)
            val endFixY = containerHeight * 0.5f - (endTop + endP.height * 0.5f)

            endP.placeWithLayer(endLeft, endTop) {
                val rubber = holder.rubberNode
                rubber?.attachedState?.frameTick
                val pose = rubber?.boundPose
                transformOrigin = TransformOrigin.Center
                if (!hasEndArrow || arrowLengthPx <= 0f || rubber == null || pose == null || !pose.isActive || pose.length <= 0f) {
                    scaleX = 0f
                    scaleY = 0f
                    translationX = 0f
                } else {
                    val effScale = rubber.computeEffectiveContainerScale(pose)
                    val localSpan = if (effScale > 1e-4f) pose.length / effScale else 0f
                    val arrW = endP.width.toFloat()
                    val arrScale = computeComposeArrowScale(localSpan, arrW, hasStartArrow, hasEndArrow)
                    val effArrW = arrW * arrScale
                    scaleX = arrScale
                    scaleY = arrScale
                    translationX = endFixX + localSpan * 0.5f - effArrW * 0.5f
                    translationY = endFixY
                }
            }

            if (pillWidthPx > 0 && pillHeightPx > 0) {
                val labelLeft = (containerWidth - pillWidthPx) / 2
                val labelTop = (containerHeight - pillHeightPx) / 2
                val labelFixX = containerWidth * 0.5f - (labelLeft + pillWidthPx * 0.5f)
                val labelFixY = containerHeight * 0.5f - (labelTop + pillHeightPx * 0.5f)
                labelP.placeWithLayer(labelLeft, labelTop) {
                    translationX = labelFixX
                    translationY = labelFixY - (lineThicknessPx * 0.5f + labelOffsetPx + pillHeightPx * 0.5f)
                }
            }
        }
    }
}

internal fun computeComposeArrowScale(
    localSpan: Float,
    arrowLengthPx: Float,
    hasStart: Boolean,
    hasEnd: Boolean,
): Float {
    if (arrowLengthPx <= 0f || localSpan <= 0f) return 0f
    val minSpan = (if (hasStart) arrowLengthPx else 0f) + (if (hasEnd) arrowLengthPx else 0f)
    return if (minSpan > 0f && localSpan < minSpan) {
        (localSpan / minSpan).coerceIn(0f, 1f)
    } else {
        1f
    }
}

internal fun computeComposeArrowStemInset(
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

private class RubberRefHolder {
    var rubberNode: DeponderRubberNode? = null
}

private data class DefaultRubberBindingElement(
    val holder: RubberRefHolder,
    val nonStretchedWidthPx: Int,
) : ModifierNodeElement<DefaultRubberBindingNode>() {
    override fun create(): DefaultRubberBindingNode = DefaultRubberBindingNode(holder, nonStretchedWidthPx)

    override fun update(node: DefaultRubberBindingNode) {
        node.update(holder, nonStretchedWidthPx)
    }
}

private class DefaultRubberBindingNode(
    var holder: RubberRefHolder,
    var nonStretchedWidthPx: Int,
) : Modifier.Node() {
    override fun onAttach() {
        val rubber = findNearestAncestor(DeponderRubberTraverseKey) as? DeponderRubberNode
        holder.rubberNode = rubber
        rubber?.explicitNonStretchedWidthPx = nonStretchedWidthPx
    }

    fun update(newHolder: RubberRefHolder, newWidthPx: Int) {
        holder = newHolder
        nonStretchedWidthPx = newWidthPx
        val rubber = findNearestAncestor(DeponderRubberTraverseKey) as? DeponderRubberNode
        holder.rubberNode = rubber
        rubber?.explicitNonStretchedWidthPx = newWidthPx
        rubber?.attachedState?.notifyFrameChanged()
    }

    override fun onDetach() {
        holder.rubberNode?.explicitNonStretchedWidthPx = -1
        holder.rubberNode = null
    }
}

private data class SolidRectDrawElement(
    val color: Color,
) : ModifierNodeElement<SolidRectDrawNode>() {
    override fun create(): SolidRectDrawNode = SolidRectDrawNode(color)

    override fun update(node: SolidRectDrawNode) {
        node.color = color
    }
}

private class SolidRectDrawNode(
    var color: Color,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        drawRect(color = color)
        drawContent()
    }
}

private data class ArrowHeadDrawElement(
    val color: Color,
    val pointsRight: Boolean,
) : ModifierNodeElement<ArrowHeadDrawNode>() {
    override fun create(): ArrowHeadDrawNode = ArrowHeadDrawNode(color, pointsRight)

    override fun update(node: ArrowHeadDrawNode) {
        node.color = color
        node.pointsRight = pointsRight
    }
}

private class ArrowHeadDrawNode(
    var color: Color,
    var pointsRight: Boolean,
) : Modifier.Node(), DrawModifierNode {
    private val arrowPath = Path()

    override fun ContentDrawScope.draw() {
        val w = size.width
        val h = size.height
        if (w > 0f && h > 0f) {
            arrowPath.reset()
            if (pointsRight) {
                arrowPath.moveTo(w, h * 0.5f)
                arrowPath.lineTo(0f, 0f)
                arrowPath.lineTo(0f, h)
            } else {
                arrowPath.moveTo(0f, h * 0.5f)
                arrowPath.lineTo(w, 0f)
                arrowPath.lineTo(w, h)
            }
            arrowPath.close()
            drawPath(path = arrowPath, color = color)
        }
        drawContent()
    }
}

private data class FloatingLabelDrawElement(
    val pillBackgroundColor: Color,
    val textLayoutResult: TextLayoutResult?,
) : ModifierNodeElement<FloatingLabelDrawNode>() {
    override fun create(): FloatingLabelDrawNode = FloatingLabelDrawNode(
        pillBackgroundColor = pillBackgroundColor,
        textLayoutResult = textLayoutResult,
    )

    override fun update(node: FloatingLabelDrawNode) {
        node.pillBackgroundColor = pillBackgroundColor
        node.textLayoutResult = textLayoutResult
    }
}

private class FloatingLabelDrawNode(
    var pillBackgroundColor: Color,
    var textLayoutResult: TextLayoutResult?,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        val textResult = textLayoutResult
        if (textResult != null && size.width > 0f && size.height > 0f) {
            if (pillBackgroundColor.alpha > 0f) {
                val corner = size.height * 0.5f
                drawRoundRect(
                    color = pillBackgroundColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
            val textTopLeft = Offset(
                x = (size.width - textResult.size.width) * 0.5f,
                y = (size.height - textResult.size.height) * 0.5f,
            )
            drawText(textLayoutResult = textResult, topLeft = textTopLeft)
        }
        drawContent()
    }
}
