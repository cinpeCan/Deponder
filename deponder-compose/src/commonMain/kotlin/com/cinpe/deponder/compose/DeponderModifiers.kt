package com.cinpe.deponder.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetBody
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RubberPhysics
import com.cinpe.deponder.core.RubberPose
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal object DeponderRootTraverseKey
internal object DeponderPlanetTraverseKey
internal object DeponderRubberTraverseKey
internal object DeponderStretchTraverseKey
internal object DeponderDragTraverseKey

/**
 * Establishes a non-intrusive physics simulation coordinate space on an unconstrained host container
 * (such as `Box`).
 *
 * Never mutates host `clipToBounds`, child trees, or business layout constraints.
 */
public fun Modifier.deponderRoot(
    state: DeponderState,
): Modifier = this then DeponderRootElement(state)

/**
 * Marks any UI element inside a [deponderRoot] subtree as a point-mass physics node (`Planet`).
 *
 * May be placed at any static nesting depth beneath [deponderRoot], except inside another
 * [deponderPlanet] or [deponderRubber].
 */
public fun Modifier.deponderPlanet(
    id: String,
    physics: PlanetPhysics = PlanetPhysics(),
): Modifier = this then DeponderPlanetElement(id, physics) then DeponderPlanetInnerPointerElement

/**
 * Marks any UI element inside a [deponderRoot] subtree as an undirected spring link (`Rubber`)
 * connecting [startId] and [endId].
 *
 * Automatically clears both endpoint planets' UI contours so neither built-in nor custom developer
 * rubbers get occluded beneath planet nodes. Chain [deponderStretch] on this modifier or on an inner
 * child element to stretch horizontally along the visible span.
 */
public fun Modifier.deponderRubber(
    startId: String,
    endId: String,
    physics: RubberPhysics = RubberPhysics(),
): Modifier = this then DeponderRubberElement(startId, endId, physics) then DeponderRubberInnerAnchorElement

/**
 * Opts the modified [deponderRubber] (or a specific inner child inside a [deponderRubber] subtree)
 * into horizontal endpoint-distance stretching.
 *
 * Automatically prunes nested stretch modifiers if an ancestor inside the same rubber is already
 * stretched, and horizontally center-aligns inner stretched children to the rubber's pivot center.
 */
public fun Modifier.deponderStretch(
    enabled: Boolean = true,
): Modifier = this then DeponderStretchElement(enabled)

/**
 * Attaches a non-intrusive drag probe to a [deponderPlanet] or an inner handle inside a [deponderPlanet].
 *
 * Preserves downstream `.clickable {}` semantics via `touchSlop` gating, handles multi-pointer
 * handoff without coordinate jumps, and solves displacements in static [deponderRoot] space.
 */
public fun Modifier.deponderDrag(
    enabled: Boolean = true,
    bringToFrontOnPress: Boolean = true,
): Modifier = this then DeponderDragElement(enabled, bringToFrontOnPress)

/**
 * Pure public-API check testing whether two attached [Modifier.Node]s on the same ancestor chain
 * belong to the exact same `LayoutNode` (tree-depth invariant: an ancestor node shares the exact
 * same `parentLayoutCoordinates` reference iff it resides on the same `LayoutNode`).
 */
private fun areOnSameLayoutNode(a: Modifier.Node, b: Modifier.Node): Boolean {
    if (!a.isAttached || !b.isAttached) return false
    val coordsA = a.requireLayoutCoordinates()
    val coordsB = b.requireLayoutCoordinates()
    if (!coordsA.isAttached || !coordsB.isAttached) return false
    if (coordsA === coordsB) return true
    val parentA = coordsA.parentLayoutCoordinates
    val parentB = coordsB.parentLayoutCoordinates
    return parentA != null && parentA === parentB
}

private data class DeponderRootElement(
    val state: DeponderState,
) : ModifierNodeElement<DeponderRootNode>() {
    override fun create(): DeponderRootNode = DeponderRootNode(state)

    override fun update(node: DeponderRootNode) {
        node.updateState(state)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "deponderRoot"
        properties["state"] = state
    }
}

internal class DeponderRootNode(
    var state: DeponderState,
) : Modifier.Node(),
    TraversableNode,
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {

    override val traverseKey: Any
        get() = DeponderRootTraverseKey

    var rootCoordinates: LayoutCoordinates? = null
        private set

    override fun onAttach() {
        val density = observeDensity()
        state.attachRoot(this, density)
        coroutineScope.launch {
            snapshotFlow { state.isAwake }
                .collectLatest { awake ->
                    if (!awake) return@collectLatest
                    var lastFrameNanos = 0L
                    withFrameNanos { firstNanos ->
                        lastFrameNanos = firstNanos
                    }
                    while (isActive && state.isAwake) {
                        val dtSeconds = withFrameNanos { nowNanos ->
                            val dt = (nowNanos - lastFrameNanos) * 1e-9f
                            lastFrameNanos = nowNanos
                            dt
                        }
                        if (!state.stepFrame(dtSeconds)) {
                            break
                        }
                    }
                }
        }
    }

    override fun onObservedReadsChanged() {
        if (!isAttached) return
        val density = observeDensity()
        state.updateDensity(density)
    }

    private fun observeDensity(): Float {
        var densityValue = 1f
        observeReads {
            densityValue = currentValueOf(LocalDensity).density
        }
        return densityValue
    }

    fun updateState(newState: DeponderState) {
        if (state === newState) return
        val oldState = state
        if (isAttached) {
            oldState.detachRoot(this)
        }
        state = newState
        if (isAttached) {
            val density = observeDensity()
            newState.attachRoot(this, density)
        }
    }

    override fun onRemeasured(size: IntSize) {
        state.updateViewportSize(size.width, size.height)
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
        for (planet in state.activePlanetsById.values) {
            planet.refreshBaseOriginFromRoot()
        }
        for (rubber in state.activeRubbersByKey.values) {
            rubber.refreshBaseOriginFromRoot()
        }
    }

    override fun onDetach() {
        rootCoordinates = null
        state.detachRoot(this)
    }
}

private data class DeponderPlanetElement(
    val id: String,
    val physics: PlanetPhysics,
) : ModifierNodeElement<DeponderPlanetNode>() {
    override fun create(): DeponderPlanetNode = DeponderPlanetNode(id, physics)

    override fun update(node: DeponderPlanetNode) {
        node.update(id, physics)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "deponderPlanet"
        properties["id"] = id
        properties["physics"] = physics
    }
}

internal class DeponderPlanetNode(
    var planetId: String,
    var physics: PlanetPhysics,
) : Modifier.Node(),
    LayoutModifierNode,
    LayoutAwareModifierNode,
    TraversableNode {

    override val traverseKey: Any
        get() = DeponderPlanetTraverseKey

    var rootNode: DeponderRootNode? = null
        private set

    val attachedState: DeponderState?
        get() = rootNode?.state

    var boundBody: PlanetBody? = null
        private set

    var outerCoordinates: LayoutCoordinates? = null
        private set

    var baseOriginInRootX: Float = 0f
        private set
    var baseOriginInRootY: Float = 0f
        private set
    var hasBaseOrigin: Boolean = false
        private set

    var measuredWidthPx: Int = 0
        private set
    var measuredHeightPx: Int = 0
        private set

    var zOrder: Float by mutableFloatStateOf(0f)

    internal val outerSameNodeDrags = ArrayList<DeponderDragNode>(1)

    override fun onAttach() {
        check(findNearestAncestor(DeponderPlanetTraverseKey) == null) {
            "Modifier.deponderPlanet('$planetId') cannot be nested inside another deponderPlanet."
        }
        check(findNearestAncestor(DeponderRubberTraverseKey) == null) {
            "Modifier.deponderPlanet('$planetId') cannot be nested inside a deponderRubber."
        }
        val foundRoot = findNearestAncestor(DeponderRootTraverseKey) as? DeponderRootNode
            ?: throw IllegalStateException(
                "Modifier.deponderPlanet('$planetId') must be placed inside a Modifier.deponderRoot container.",
            )
        rootNode = foundRoot
        foundRoot.state.registerPlanetNode(this)
    }

    fun update(newId: String, newPhysics: PlanetPhysics) {
        val oldId = planetId
        val oldPhysics = physics
        if (oldId == newId && oldPhysics == newPhysics) return
        planetId = newId
        physics = newPhysics
        val st = attachedState ?: return
        boundBody = st.updatePlanetRegistration(
            node = this,
            oldId = oldId,
            newId = newId,
            newPhysics = newPhysics,
            fallbackCenterX = baseOriginInRootX + measuredWidthPx * 0.5f,
            fallbackCenterY = baseOriginInRootY + measuredHeightPx * 0.5f,
        )
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        outerCoordinates = coordinates
        bindUpstreamSameNodeDrags()
        refreshBaseOriginFromRoot()
    }

    private fun bindUpstreamSameNodeDrags() {
        outerSameNodeDrags.clear()
        traverseAncestors(DeponderDragTraverseKey) { ancestor ->
            val dragNode = ancestor as? DeponderDragNode ?: return@traverseAncestors true
            if (!areOnSameLayoutNode(dragNode, this)) {
                return@traverseAncestors false
            }
            dragNode.bindToDownstreamSameNodePlanet(this)
            if (!outerSameNodeDrags.contains(dragNode)) {
                outerSameNodeDrags.add(dragNode)
            }
            true
        }
    }

    internal fun refreshBaseOriginFromRoot() {
        val rootCoords = rootNode?.rootCoordinates ?: return
        val coords = outerCoordinates ?: return
        if (!rootCoords.isAttached || !coords.isAttached) return
        val origin = rootCoords.localPositionOf(coords, Offset.Zero)
        val changed = !hasBaseOrigin || baseOriginInRootX != origin.x || baseOriginInRootY != origin.y
        baseOriginInRootX = origin.x
        baseOriginInRootY = origin.y
        hasBaseOrigin = true
        val st = attachedState ?: return
        if (boundBody == null) {
            boundBody = st.syncPlanetPlacement(
                node = this,
                initialCenterX = origin.x + measuredWidthPx * 0.5f,
                initialCenterY = origin.y + measuredHeightPx * 0.5f,
            )
        } else if (changed) {
            st.notifyFrameChanged()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val sizeChanged = measuredWidthPx != placeable.width || measuredHeightPx != placeable.height
        measuredWidthPx = placeable.width
        measuredHeightPx = placeable.height
        if (sizeChanged && boundBody != null) {
            attachedState?.updatePlanetMeasuredSize(planetId, placeable.width, placeable.height)
        }
        return layout(placeable.width, placeable.height) {
            bindUpstreamSameNodeDrags()
            // Read zOrder exclusively inside placement so bringToFront() causes zero re-measurements!
            placeable.placeWithLayer(0, 0, zIndex = zOrder) {
                val st = attachedState
                st?.frameTick
                val body = boundBody
                transformOrigin = TransformOrigin.Center
                if (st != null && body != null && hasBaseOrigin) {
                    val staticCenterX = baseOriginInRootX + measuredWidthPx * 0.5f
                    val staticCenterY = baseOriginInRootY + measuredHeightPx * 0.5f
                    translationX = body.x - staticCenterX
                    translationY = body.y - staticCenterY
                    scaleX = st.scale
                    scaleY = st.scale
                } else {
                    translationX = 0f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
        }
    }

    override fun onDetach() {
        attachedState?.unregisterPlanetNode(this)
        boundBody = null
        outerCoordinates = null
        hasBaseOrigin = false
        zOrder = 0f
        for (drag in outerSameNodeDrags) {
            drag.unbindSameNodePlanet(this)
        }
        outerSameNodeDrags.clear()
        rootNode = null
    }
}

private object DeponderPlanetInnerPointerElement : ModifierNodeElement<DeponderPlanetInnerPointerNode>() {
    override fun create(): DeponderPlanetInnerPointerNode = DeponderPlanetInnerPointerNode()

    override fun update(node: DeponderPlanetInnerPointerNode) = Unit

    override fun hashCode(): Int = 0x51A7E701

    override fun equals(other: Any?): Boolean = other === this
}

internal class DeponderPlanetInnerPointerNode : DelegatingNode(), PointerInputModifierNode {
    private val pointerDelegate = delegate(
        SuspendingPointerInputModifierNode(
            PointerInputEventHandler {
                val planet = findNearestAncestor(DeponderPlanetTraverseKey) as? DeponderPlanetNode
                    ?: return@PointerInputEventHandler
                runPlanetDragLoop(
                    planetProvider = { planet },
                    configProvider = {
                        val activeOuter = planet.outerSameNodeDrags.firstOrNull { it.enabled }
                        if (activeOuter != null) {
                            DragProbeConfig(enabled = true, bringToFrontOnPress = activeOuter.bringToFrontOnPress)
                        } else {
                            DragProbeConfig.Disabled
                        }
                    },
                    pointerCoordinatesProvider = {
                        if (isAttached) requireLayoutCoordinates() else null
                    },
                )
            },
        ),
    )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        val planet = findNearestAncestor(DeponderPlanetTraverseKey) as? DeponderPlanetNode ?: return
        if (planet.outerSameNodeDrags.none { it.enabled }) return
        pointerDelegate.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerDelegate.onCancelPointerInput()
    }
}

private data class DeponderRubberElement(
    val startId: String,
    val endId: String,
    val physics: RubberPhysics,
) : ModifierNodeElement<DeponderRubberNode>() {
    override fun create(): DeponderRubberNode = DeponderRubberNode(startId, endId, physics)

    override fun update(node: DeponderRubberNode) {
        node.update(startId, endId, physics)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "deponderRubber"
        properties["startId"] = startId
        properties["endId"] = endId
        properties["physics"] = physics
    }
}

internal class DeponderRubberNode(
    var startId: String,
    var endId: String,
    var physics: RubberPhysics,
) : Modifier.Node(),
    LayoutModifierNode,
    LayoutAwareModifierNode,
    TraversableNode {

    override val traverseKey: Any
        get() = DeponderRubberTraverseKey

    var rootNode: DeponderRootNode? = null
        private set

    val attachedState: DeponderState?
        get() = rootNode?.state

    var boundPose: RubberPose? = null
        private set

    var outerCoordinates: LayoutCoordinates? = null
        private set

    var innerAnchorCoordinates: LayoutCoordinates? = null
        internal set

    var baseOriginInRootX: Float = 0f
        private set
    var baseOriginInRootY: Float = 0f
        private set
    var hasBaseOrigin: Boolean = false
        private set

    var measuredWidthPx: Int = 0
        private set
    var measuredHeightPx: Int = 0
        private set

    internal var explicitNonStretchedWidthPx: Int = -1
    internal val directStretchNodes = ArrayList<DeponderStretchNode>(1)
    internal val childStretchNodes = ArrayList<DeponderStretchNode>(1)

    internal fun isDirectStretchEnabled(): Boolean = directStretchNodes.any { it.enabled }

    internal fun computeEffectiveContainerScale(pose: RubberPose): Float {
        if (!pose.isActive || pose.length <= 0f || pose.scale <= 0f) return 0f
        if (isDirectStretchEnabled()) return pose.scale

        val nonStretchedW = if (explicitNonStretchedWidthPx >= 0) {
            explicitNonStretchedWidthPx
        } else {
            var maxInnerStretchW = -1
            for (i in childStretchNodes.indices) {
                val n = childStretchNodes[i]
                if (n.enabled && n.measuredWidthPx > maxInnerStretchW) {
                    maxInnerStretchW = n.measuredWidthPx
                }
            }
            if (maxInnerStretchW < 0) {
                measuredWidthPx
            } else if (measuredWidthPx > maxInnerStretchW) {
                measuredWidthPx
            } else {
                0
            }
        }

        if (nonStretchedW > 0 && pose.length < nonStretchedW * pose.scale) {
            return (pose.length / nonStretchedW.toFloat()).coerceAtLeast(0f)
        }
        return pose.scale
    }

    override fun onAttach() {
        check(findNearestAncestor(DeponderPlanetTraverseKey) == null) {
            "Modifier.deponderRubber('$startId', '$endId') cannot be nested inside a deponderPlanet."
        }
        check(findNearestAncestor(DeponderRubberTraverseKey) == null) {
            "Modifier.deponderRubber('$startId', '$endId') cannot be nested inside another deponderRubber."
        }
        val foundRoot = findNearestAncestor(DeponderRootTraverseKey) as? DeponderRootNode
            ?: throw IllegalStateException(
                "Modifier.deponderRubber('$startId', '$endId') must be placed inside a Modifier.deponderRoot container.",
            )
        rootNode = foundRoot
        boundPose = foundRoot.state.registerRubberNode(this)
    }

    fun update(newStartId: String, newEndId: String, newPhysics: RubberPhysics) {
        val oldStart = startId
        val oldEnd = endId
        val oldPhysics = physics
        if (oldStart == newStartId && oldEnd == newEndId && oldPhysics == newPhysics) return
        val oldKey = EdgeKey(oldStart, oldEnd)
        startId = newStartId
        endId = newEndId
        physics = newPhysics
        val st = attachedState ?: return
        boundPose = st.updateRubberRegistration(
            node = this,
            oldKey = oldKey,
            newStartId = newStartId,
            newEndId = newEndId,
            newPhysics = newPhysics,
        )
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        outerCoordinates = coordinates
        bindUpstreamSameNodeStretches()
        refreshBaseOriginFromRoot()
    }

    private fun bindUpstreamSameNodeStretches() {
        traverseAncestors(DeponderStretchTraverseKey) { ancestor ->
            val stretchNode = ancestor as? DeponderStretchNode ?: return@traverseAncestors true
            if (!areOnSameLayoutNode(stretchNode, this)) {
                return@traverseAncestors false
            }
            stretchNode.bindToSameNodeRubber(this)
            if (!directStretchNodes.contains(stretchNode)) {
                directStretchNodes.add(stretchNode)
            }
            childStretchNodes.remove(stretchNode)
            true
        }
    }

    internal fun refreshBaseOriginFromRoot() {
        val rootCoords = rootNode?.rootCoordinates ?: return
        val coords = outerCoordinates ?: return
        if (!rootCoords.isAttached || !coords.isAttached) return
        val origin = rootCoords.localPositionOf(coords, Offset.Zero)
        val changed = !hasBaseOrigin || baseOriginInRootX != origin.x || baseOriginInRootY != origin.y
        baseOriginInRootX = origin.x
        baseOriginInRootY = origin.y
        hasBaseOrigin = true
        if (changed) {
            attachedState?.notifyFrameChanged()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Measure rubber content with unconstrained width/height (unless explicitly fixed on this node)
        // so inner `fillMaxWidth()` stretched lines collapse to 1px and never inflate sibling badge widths,
        // and wide labels never prematurely wrap inside narrow root viewports.
        val childConstraints = Constraints(
            minWidth = if (constraints.hasFixedWidth) constraints.minWidth else 0,
            maxWidth = if (constraints.hasFixedWidth) constraints.maxWidth else Constraints.Infinity,
            minHeight = if (constraints.hasFixedHeight) constraints.minHeight else 0,
            maxHeight = if (constraints.hasFixedHeight) constraints.maxHeight else Constraints.Infinity,
        )
        val placeable = measurable.measure(childConstraints)
        measuredWidthPx = placeable.width
        measuredHeightPx = placeable.height
        val reportedWidth = if (constraints.hasFixedWidth) {
            constraints.maxWidth
        } else {
            placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        }
        val reportedHeight = if (constraints.hasFixedHeight) {
            constraints.maxHeight
        } else {
            placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        }
        return layout(reportedWidth, reportedHeight) {
            bindUpstreamSameNodeStretches()
            placeable.placeWithLayer(0, 0, zIndex = -1f) {
                val st = attachedState
                st?.frameTick
                val pose = boundPose
                transformOrigin = TransformOrigin.Center
                if (st == null || pose == null || !pose.isActive || pose.length <= 0f || !hasBaseOrigin) {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    translationX = 0f
                    translationY = 0f
                    rotationZ = 0f
                } else {
                    alpha = 1f
                    val staticCenterX = baseOriginInRootX + measuredWidthPx * 0.5f
                    val staticCenterY = baseOriginInRootY + measuredHeightPx * 0.5f
                    translationX = pose.centerX - staticCenterX
                    translationY = pose.centerY - staticCenterY
                    rotationZ = pose.rotationDeg
                    if (isDirectStretchEnabled()) {
                        scaleX = computeStretchScaleX(
                            lengthPx = pose.length,
                            widthPx = measuredWidthPx,
                            parentScale = 1f,
                        )
                        scaleY = pose.scale
                    } else {
                        val effectiveScale = computeEffectiveContainerScale(pose)
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                    }
                }
            }
        }
    }

    override fun onDetach() {
        attachedState?.unregisterRubberNode(this)
        boundPose = null
        outerCoordinates = null
        innerAnchorCoordinates = null
        hasBaseOrigin = false
        explicitNonStretchedWidthPx = -1
        for (s in directStretchNodes) {
            s.unbindRubber(this)
        }
        for (s in childStretchNodes) {
            s.unbindRubber(this)
        }
        directStretchNodes.clear()
        childStretchNodes.clear()
        rootNode = null
    }
}

private object DeponderRubberInnerAnchorElement : ModifierNodeElement<DeponderRubberInnerAnchorNode>() {
    override fun create(): DeponderRubberInnerAnchorNode = DeponderRubberInnerAnchorNode()

    override fun update(node: DeponderRubberInnerAnchorNode) = Unit

    override fun hashCode(): Int = 0x52B8F802

    override fun equals(other: Any?): Boolean = other === this
}

internal class DeponderRubberInnerAnchorNode : Modifier.Node(), LayoutAwareModifierNode {
    private var boundRubber: DeponderRubberNode? = null

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val rubber = findNearestAncestor(DeponderRubberTraverseKey) as? DeponderRubberNode
        boundRubber = rubber
        rubber?.innerAnchorCoordinates = coordinates
    }

    override fun onDetach() {
        val rubber = boundRubber
        if (rubber?.innerAnchorCoordinates != null) {
            rubber.innerAnchorCoordinates = null
        }
        boundRubber = null
    }
}

private data class DeponderStretchElement(
    val enabled: Boolean,
) : ModifierNodeElement<DeponderStretchNode>() {
    override fun create(): DeponderStretchNode = DeponderStretchNode(enabled)

    override fun update(node: DeponderStretchNode) {
        node.update(enabled)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "deponderStretch"
        properties["enabled"] = enabled
    }
}

internal class DeponderStretchNode(
    var enabled: Boolean,
) : Modifier.Node(),
    LayoutModifierNode,
    LayoutAwareModifierNode,
    TraversableNode {

    override val traverseKey: Any
        get() = DeponderStretchTraverseKey

    private var hostRubberNode: DeponderRubberNode? = null
    private var isSameNodeAsRubber: Boolean = false
    private var outerCoordinates: LayoutCoordinates? = null
    private var baseLeftInRubber: Float = 0f

    internal var measuredWidthPx: Int = 0
        private set

    override fun onAttach() {
        val ancestorRubber = findNearestAncestor(DeponderRubberTraverseKey) as? DeponderRubberNode
        hostRubberNode = ancestorRubber
        isSameNodeAsRubber = false
    }

    internal fun bindToSameNodeRubber(rubber: DeponderRubberNode) {
        hostRubberNode = rubber
        isSameNodeAsRubber = true
    }

    internal fun unbindRubber(rubber: DeponderRubberNode) {
        if (hostRubberNode === rubber) {
            hostRubberNode = null
            isSameNodeAsRubber = false
        }
    }

    fun update(newEnabled: Boolean) {
        if (enabled != newEnabled) {
            enabled = newEnabled
            hostRubberNode?.attachedState?.notifyFrameChanged()
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        outerCoordinates = coordinates
        syncRubberMembershipAndOrigin(requireBound = false)
    }

    private fun syncRubberMembershipAndOrigin(requireBound: Boolean) {
        val rubber = hostRubberNode ?: (findNearestAncestor(DeponderRubberTraverseKey) as? DeponderRubberNode)
        if (rubber == null) {
            if (requireBound && enabled) {
                throw IllegalStateException(
                    "Modifier.deponderStretch must be applied directly to or nested inside a Modifier.deponderRubber.",
                )
            }
            return
        }
        hostRubberNode = rubber
        val sameNode = areOnSameLayoutNode(this, rubber)
        isSameNodeAsRubber = sameNode
        if (sameNode) {
            rubber.childStretchNodes.remove(this)
            if (!rubber.directStretchNodes.contains(this)) {
                rubber.directStretchNodes.add(this)
            }
        } else {
            rubber.directStretchNodes.remove(this)
            if (!rubber.childStretchNodes.contains(this)) {
                rubber.childStretchNodes.add(this)
            }
            val anchor = rubber.innerAnchorCoordinates
            val coords = outerCoordinates
            if (anchor != null && coords != null && anchor.isAttached && coords.isAttached) {
                baseLeftInRubber = anchor.localPositionOf(coords, Offset.Zero).x
            }
        }
    }

    private fun hasActiveStretchAncestorInsideRubber(rubber: DeponderRubberNode): Boolean {
        if (rubber.isDirectStretchEnabled()) return true
        var ancestor = findNearestAncestor(DeponderStretchTraverseKey) as? DeponderStretchNode
        while (ancestor != null) {
            if (ancestor.hostRubberNode !== rubber) break
            if (ancestor.enabled) return true
            ancestor = ancestor.findNearestAncestor(DeponderStretchTraverseKey) as? DeponderStretchNode
        }
        return false
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val adjustedConstraints = if (enabled && constraints.minWidth == 0 && !constraints.hasBoundedWidth) {
            constraints.copy(minWidth = 1)
        } else {
            constraints
        }
        val placeable = measurable.measure(adjustedConstraints)
        measuredWidthPx = placeable.width
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val rubber = hostRubberNode
                val st = rubber?.attachedState
                st?.frameTick
                val pose = rubber?.boundPose
                transformOrigin = TransformOrigin.Center
                if (isSameNodeAsRubber ||
                    !enabled ||
                    rubber == null ||
                    pose == null ||
                    !pose.isActive ||
                    pose.length <= 0f ||
                    hasActiveStretchAncestorInsideRubber(rubber)
                ) {
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                } else {
                    val anchor = rubber.innerAnchorCoordinates
                    val coords = outerCoordinates
                    if (anchor != null && coords != null && anchor.isAttached && coords.isAttached) {
                        baseLeftInRubber = anchor.localPositionOf(coords, Offset.Zero).x
                    }
                    val parentScale = rubber.computeEffectiveContainerScale(pose)
                    scaleX = computeStretchScaleX(
                        lengthPx = pose.length,
                        widthPx = measuredWidthPx,
                        parentScale = parentScale,
                    )
                    scaleY = 1f
                    translationX = computeChildStretchCenterAlignTx(
                        rubberWidthPx = rubber.measuredWidthPx,
                        childLeftInRubberPx = baseLeftInRubber,
                        childWidthPx = measuredWidthPx,
                    )
                    translationY = 0f
                }
            }
            syncRubberMembershipAndOrigin(requireBound = true)
        }
    }

    override fun onDetach() {
        hostRubberNode?.directStretchNodes?.remove(this)
        hostRubberNode?.childStretchNodes?.remove(this)
        hostRubberNode = null
        isSameNodeAsRubber = false
        outerCoordinates = null
        baseLeftInRubber = 0f
    }
}

private data class DeponderDragElement(
    val enabled: Boolean,
    val bringToFrontOnPress: Boolean,
) : ModifierNodeElement<DeponderDragNode>() {
    override fun create(): DeponderDragNode = DeponderDragNode(enabled, bringToFrontOnPress)

    override fun update(node: DeponderDragNode) {
        node.enabled = enabled
        node.bringToFrontOnPress = bringToFrontOnPress
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "deponderDrag"
        properties["enabled"] = enabled
        properties["bringToFrontOnPress"] = bringToFrontOnPress
    }
}

internal class DeponderDragNode(
    var enabled: Boolean,
    var bringToFrontOnPress: Boolean,
) : DelegatingNode(),
    LayoutModifierNode,
    PointerInputModifierNode,
    TraversableNode {

    override val traverseKey: Any
        get() = DeponderDragTraverseKey

    private var targetPlanetNode: DeponderPlanetNode? = null
    private var delegatesToPlanetInnerLayer: Boolean = false

    private val pointerDelegate = delegate(
        SuspendingPointerInputModifierNode(
            PointerInputEventHandler {
                runPlanetDragLoop(
                    planetProvider = { targetPlanetNode },
                    configProvider = {
                        if (!delegatesToPlanetInnerLayer && enabled) {
                            DragProbeConfig(enabled = true, bringToFrontOnPress = bringToFrontOnPress)
                        } else {
                            DragProbeConfig.Disabled
                        }
                    },
                    pointerCoordinatesProvider = {
                        if (isAttached) requireLayoutCoordinates() else null
                    },
                )
            },
        ),
    )

    override fun onAttach() {
        val ancestorPlanet = findNearestAncestor(DeponderPlanetTraverseKey) as? DeponderPlanetNode
        if (ancestorPlanet != null) {
            targetPlanetNode = ancestorPlanet
            delegatesToPlanetInnerLayer = false
        } else {
            targetPlanetNode = null
            delegatesToPlanetInnerLayer = false
        }
    }

    internal fun bindToDownstreamSameNodePlanet(planet: DeponderPlanetNode) {
        targetPlanetNode = planet
        delegatesToPlanetInnerLayer = true
    }

    internal fun unbindSameNodePlanet(planet: DeponderPlanetNode) {
        if (targetPlanetNode === planet) {
            targetPlanetNode = null
            delegatesToPlanetInnerLayer = false
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            if (targetPlanetNode == null) {
                val ancestorPlanet = findNearestAncestor(DeponderPlanetTraverseKey) as? DeponderPlanetNode
                if (ancestorPlanet != null) {
                    targetPlanetNode = ancestorPlanet
                    delegatesToPlanetInnerLayer = false
                } else if (enabled) {
                    throw IllegalStateException(
                        "Modifier.deponderDrag must be applied directly to or nested inside a Modifier.deponderPlanet.",
                    )
                }
            }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (delegatesToPlanetInnerLayer || !enabled) return
        pointerDelegate.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerDelegate.onCancelPointerInput()
    }

    override fun onDetach() {
        pointerDelegate.onCancelPointerInput()
        targetPlanetNode?.outerSameNodeDrags?.remove(this)
        targetPlanetNode = null
        delegatesToPlanetInnerLayer = false
    }
}

internal data class DragProbeConfig(
    val enabled: Boolean,
    val bringToFrontOnPress: Boolean,
) {
    companion object {
        val Disabled: DragProbeConfig = DragProbeConfig(enabled = false, bringToFrontOnPress = false)
    }
}

/**
 * Pure `androidx.compose.ui.input.pointer` collaborative touch probe + static `deponderRoot` space solver:
 * - Observes `PointerEventPass.Initial` (top-down) so downstream `.clickable {}` (which consumes `down` in
 *   `PointerEventPass.Main`) never blocks planet dragging, while taps below `touchSlop` remain completely
 *   unconsumed so `.clickable {}` ripples and `onClick` callbacks fire normally.
 * - Once movement crosses `touchSlop`, consumes pointer changes in `PointerEventPass.Initial` so downstream
 *   or upstream `.clickable {}` cleanly cancels its pending tap without firing `onClick` on release.
 * - Projects pointer positions directly into static `deponderRoot` coordinates via `localPositionOf`
 *   so dynamic `placeWithLayer` transforms never cause coordinate feedback jitter.
 * - Seamlessly hands off `activePointerId` across multi-finger presses/lifts by re-anchoring `grabOffset`.
 */
private suspend fun PointerInputScope.runPlanetDragLoop(
    planetProvider: () -> DeponderPlanetNode?,
    configProvider: () -> DragProbeConfig,
    pointerCoordinatesProvider: () -> LayoutCoordinates?,
) {
    val currentCtx = coroutineContext
    awaitPointerEventScope {
        while (currentCtx.isActive) {
            var downChange: PointerInputChange? = null
            while (downChange == null && currentCtx.isActive) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val cfg = configProvider()
                if (!cfg.enabled) continue
                downChange = event.changes.firstOrNull { !it.isConsumed && it.pressed && !it.previousPressed }
            }
            val firstDown = downChange ?: break
            val cfgAtDown = configProvider()
            if (!cfgAtDown.enabled) continue

            val planetAtDown = planetProvider() ?: continue
            val rootCoordsAtDown = planetAtDown.rootNode?.rootCoordinates ?: continue
            val ptrCoordsAtDown = pointerCoordinatesProvider() ?: continue
            if (!rootCoordsAtDown.isAttached || !ptrCoordsAtDown.isAttached) continue

            val activePlanetId = planetAtDown.planetId
            val state = planetAtDown.attachedState ?: continue

            if (cfgAtDown.bringToFrontOnPress) {
                state.bringToFront(activePlanetId)
            }

            var activePointerId: PointerId = firstDown.id
            var initialRootPointer = rootCoordsAtDown.localPositionOf(ptrCoordsAtDown, firstDown.position)
            var lastRootPointer = initialRootPointer
            var grabOffsetX = 0f
            var grabOffsetY = 0f
            var isDragging = false
            val touchSlop = viewConfiguration.touchSlop

            try {
                while (currentCtx.isActive) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val cfg = configProvider()
                    if (!cfg.enabled) break

                    val planet = planetProvider() ?: break
                    val rootCoords = planet.rootNode?.rootCoordinates ?: break
                    val ptrCoords = pointerCoordinatesProvider() ?: break
                    if (!rootCoords.isAttached || !ptrCoords.isAttached) break

                    val changes = event.changes
                    val pointerChange = changes.firstOrNull { it.id == activePointerId }

                    if (pointerChange == null || !pointerChange.pressed) {
                        val fallback = changes.firstOrNull { it.pressed && !it.isConsumed }
                        if (fallback == null) {
                            if (isDragging) {
                                pointerChange?.consume()
                                isDragging = false
                                state.setPressed(planet.planetId, false)
                            }
                            break
                        }
                        if (isDragging) {
                            pointerChange?.consume()
                            fallback.consume()
                        }
                        activePointerId = fallback.id
                        val newRootPos = rootCoords.localPositionOf(ptrCoords, fallback.position)
                        lastRootPointer = newRootPos
                        if (!isDragging) {
                            initialRootPointer = newRootPos
                        } else {
                            val body = state.world.bodyOf(planet.planetId)
                            if (body != null) {
                                grabOffsetX = body.x - newRootPos.x
                                grabOffsetY = body.y - newRootPos.y
                            }
                        }
                        continue
                    }

                    if (pointerChange.isConsumed) {
                        if (isDragging) {
                            isDragging = false
                            state.setPressed(planet.planetId, false)
                        }
                        break
                    }

                    val curRootPos = rootCoords.localPositionOf(ptrCoords, pointerChange.position)
                    if (!isDragging) {
                        val totalDelta = curRootPos - initialRootPointer
                        val dist = totalDelta.getDistance()
                        if (dist >= touchSlop) {
                            isDragging = true
                            pointerChange.consume()
                            state.setPressed(planet.planetId, true)
                            val body = state.world.bodyOf(planet.planetId)
                            if (body != null) {
                                val slopAdjustedRootPos = if (dist > 0f) {
                                    initialRootPointer + (totalDelta / dist) * touchSlop
                                } else {
                                    curRootPos
                                }
                                grabOffsetX = body.x - slopAdjustedRootPos.x
                                grabOffsetY = body.y - slopAdjustedRootPos.y
                                state.moveTo(
                                    id = planet.planetId,
                                    xPx = curRootPos.x + grabOffsetX,
                                    yPx = curRootPos.y + grabOffsetY,
                                )
                            }
                        }
                    } else {
                        pointerChange.consume()
                        if (curRootPos != lastRootPointer) {
                            state.moveTo(
                                id = planet.planetId,
                                xPx = curRootPos.x + grabOffsetX,
                                yPx = curRootPos.y + grabOffsetY,
                            )
                        }
                    }
                    lastRootPointer = curRootPos
                }
            } finally {
                if (isDragging) {
                    val currentPlanetId = planetProvider()?.planetId ?: activePlanetId
                    state.setPressed(currentPlanetId, false)
                }
            }
        }
    }
}

/**
 * Pure math helper for safe stretch scale computation with zero-width / zero-scale / zero-length guard.
 */
internal fun computeStretchScaleX(
    lengthPx: Float,
    widthPx: Int,
    parentScale: Float,
): Float {
    if (widthPx <= 0 || parentScale <= 0f || lengthPx <= 0f) return 0f
    return lengthPx / (widthPx.toFloat() * parentScale)
}

/**
 * Computes horizontal center-alignment translation so a non-centered inner stretched child inside a
 * [deponderRubber] pivots cleanly around the rubber's horizontal center (`rubberWidthPx * 0.5f`).
 */
internal fun computeChildStretchCenterAlignTx(
    rubberWidthPx: Int,
    childLeftInRubberPx: Float,
    childWidthPx: Int,
): Float = rubberWidthPx * 0.5f - (childLeftInRubberPx + childWidthPx * 0.5f)
