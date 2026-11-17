@file:JvmName("DeponderViews")

package com.cinpe.deponder

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.cinpe.deponder.core.DeponderRubberInsets
import com.cinpe.deponder.core.EdgeKey
import com.cinpe.deponder.core.PlanetBody
import com.cinpe.deponder.core.PlanetPhysics
import com.cinpe.deponder.core.RootPhysics
import com.cinpe.deponder.core.RubberPhysics
import com.cinpe.deponder.core.RubberPose
import kotlin.math.hypot

/**
 * Establishes a non-intrusive physics simulation coordinate space on an unconstrained Android [ViewGroup]
 * (such as `FrameLayout`, `ConstraintLayout`, or `CoordinatorLayout`).
 *
 * Never mutates `clipChildren`, `clipToPadding`, child indices, or business view tags (`View.getTag()`).
 */
@JvmOverloads
public fun ViewGroup.deponderRoot(
    lifecycleOwner: LifecycleOwner? = null,
    physics: RootPhysics = RootPhysics(),
): DeponderController {
    val existing = getTag(R.id.deponder_root_controller_tag) as? DeponderController
    if (existing != null && !existing.isCancelled) {
        existing.updatePhysics(physics)
        existing.attachPendingDirectChildren()
        return existing
    }
    val controller = DeponderController(
        rootView = this,
        explicitLifecycleOwner = lifecycleOwner,
        initialPhysics = physics,
    )
    setTag(R.id.deponder_root_controller_tag, controller)
    controller.attachPendingDirectChildren()
    return controller
}

/**
 * Marks a direct child [View] of a [deponderRoot] container as a point-mass physics node (`Planet`).
 *
 * Throws [IllegalStateException] when attached if `view.parent` is not a [deponderRoot] container,
 * if `this` view is already bound as a [deponderRubber], or if [id] collides with another active planet.
 */
@JvmOverloads
public fun View.deponderPlanet(
    id: String,
    physics: PlanetPhysics = PlanetPhysics(),
) {
    check(getTag(R.id.deponder_rubber_binding_tag) == null) {
        "View cannot be bound as both deponderPlanet('$id') and deponderRubber simultaneously."
    }
    val existing = getTag(R.id.deponder_planet_binding_tag) as? PlanetViewBinding
    if (existing != null) {
        existing.update(id, physics)
        return
    }
    val binding = PlanetViewBinding(view = this, planetId = id, physics = physics)
    setTag(R.id.deponder_planet_binding_tag, binding)
    binding.install()
}

/**
 * Removes any active [deponderPlanet] binding from this [View] and resets its render matrix to identity,
 * while cleanly uninstalling any attached [deponderDrag] probes inside its subtree.
 */
public fun View.clearDeponderPlanet() {
    val existing = getTag(R.id.deponder_planet_binding_tag) as? PlanetViewBinding ?: return
    existing.uninstall()
    setTag(R.id.deponder_planet_binding_tag, null)
    clearDragProbesInSubtree(this)
}

/**
 * Marks a direct child [View] of a [deponderRoot] container as an undirected spring link (`Rubber`)
 * connecting [startId] and [endId].
 *
 * Applies [insets] ([DeponderRubberInsets.Default] contour clearance by default, or
 * [DeponderRubberInsets.None] for continuous center-to-center spans; defaults to [DeponderRubberView.insets]
 * and [DeponderRubberView.physics] when called on a pre-configured [DeponderRubberView]) on hardware
 * `RenderNode` properties with zero `requestLayout()` overhead, expanding outer bounds when inner
 * [deponderStretch] children are present so host containers with default `clipChildren = true` never
 * truncate stretched lines.
 */
@JvmOverloads
public fun View.deponderRubber(
    startId: String,
    endId: String,
    physics: RubberPhysics = if (this is DeponderRubberView) this.physics else RubberPhysics(),
    insets: DeponderRubberInsets = if (this is DeponderRubberView) this.insets else DeponderRubberInsets.Default,
) {
    check(getTag(R.id.deponder_planet_binding_tag) == null) {
        "View cannot be bound as both deponderRubber('$startId', '$endId') and deponderPlanet simultaneously."
    }
    if (this is DeponderRubberView) {
        syncPhysicsFromBinding(physics)
        syncInsetsFromBinding(insets)
    }
    val existing = getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
    if (existing != null) {
        existing.update(startId, endId, physics, insets)
        return
    }
    val binding = RubberViewBinding(
        view = this,
        startId = startId,
        endId = endId,
        physics = physics,
        insets = insets,
    )
    setTag(R.id.deponder_rubber_binding_tag, binding)
    binding.install()
}

/**
 * Removes any active [deponderRubber] binding from this [View] and resets its render matrix to identity,
 * clearing programmatic [deponderStretch] markers inside its subtree without altering `View.getTag()`.
 */
public fun View.clearDeponderRubber() {
    val existing = getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding ?: return
    existing.uninstall()
    setTag(R.id.deponder_rubber_binding_tag, null)
    clearProgrammaticStretchTagsInSubtree(this)
}

/**
 * Opts a [deponderRubber] view (or an inner descendant view inside a [deponderRubber] subtree) into
 * horizontal endpoint-distance stretching.
 *
 * Programmatic calls take precedence over XML `android:tag="@string/deponder_stretch"`.
 */
@JvmOverloads
public fun View.deponderStretch(
    enabled: Boolean = true,
) {
    setTag(R.id.deponder_stretch_state_tag, enabled)
    if (!enabled) {
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
    }

    if (enabled && findEnclosingPlanetBinding(this) != null) {
        throw IllegalStateException(
            "deponderStretch() must be applied directly to or nested inside a deponderRubber view, not a deponderPlanet.",
        )
    }
    val enclosingRubber = findEnclosingRubberBinding(this)
    if (enclosingRubber != null) {
        enclosingRubber.syncAdaptiveLengthAndRefreshPose()
    } else if (enabled) {
        findEnclosingRootController(this)?.markOrphanValidationDirty()
    }
}

/**
 * Attaches a non-intrusive collaborative drag probe to a [deponderPlanet] view or an inner handle view
 * inside a [deponderPlanet] subtree.
 *
 * - Preserves `OnClickListener`, `OnLongClickListener`, and Material ripple animations via `scaledTouchSlop` gating.
 * - Immediately calls `requestDisallowInterceptTouchEvent(true)` on `ACTION_DOWN` so outer scroll/pager
 *   containers never hijack planet drags before `scaledTouchSlop`.
 * - Projects multi-pointer touch coordinates through `View.matrix` into static [deponderRoot] pixel space
 *   for API 23+ multi-finger safety without screen-space feedback drift.
 * - Elevates visual Z-order (`elevation + translationZ`) above all sibling planets when [bringToFrontOnPress] is `true`.
 */
@JvmOverloads
public fun View.deponderDrag(
    enabled: Boolean = true,
    bringToFrontOnPress: Boolean = true,
) {
    val existing = getTag(R.id.deponder_drag_binding_tag) as? DragProbeBinding
    if (existing != null) {
        existing.update(enabled, bringToFrontOnPress)
        if (enabled && findEnclosingPlanetBinding(this) == null) {
            findEnclosingRootController(this)?.markOrphanValidationDirty()
        }
        return
    }
    val binding = DragProbeBinding(
        sourceView = this,
        enabled = enabled,
        bringToFrontOnPress = bringToFrontOnPress,
    )
    setTag(R.id.deponder_drag_binding_tag, binding)
    binding.install()
    if (enabled && findEnclosingPlanetBinding(this) == null) {
        findEnclosingRootController(this)?.markOrphanValidationDirty()
    }
}

internal class PlanetViewBinding(
    val view: View,
    var planetId: String,
    var physics: PlanetPhysics,
) {
    var attachedController: DeponderController? = null
        private set

    var hasElevatedZ: Boolean = false
    var elevatedTranslationZ: Float = 0f

    private val layoutListener = View.OnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
        val ctrl = attachedController ?: return@OnLayoutChangeListener
        val changed = l != oldL || t != oldT || r != oldR || b != oldB
        if (ctrl.world.bodyOf(planetId) == null && hasLaidOutBounds()) {
            ctrl.seedOrUpdatePlanetBody(this)
        } else if (changed) {
            val w = (r - l).coerceAtLeast(0)
            val h = (b - t).coerceAtLeast(0)
            ctrl.updatePlanetMeasuredSize(planetId, w, h)
        }
    }

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val ctrl = requireDirectParentRootController(
                childView = view,
                operatorDescription = "deponderPlanet('$planetId')",
            )
            attachToRootController(ctrl)
        }

        override fun onViewDetachedFromWindow(v: View) {
            detachFromControllerAndReset()
        }
    }

    fun install() {
        view.addOnLayoutChangeListener(layoutListener)
        view.addOnAttachStateChangeListener(attachListener)
        if (view.isAttachedToWindow) {
            val ctrl = requireDirectParentRootController(
                childView = view,
                operatorDescription = "deponderPlanet('$planetId')",
            )
            attachToRootController(ctrl)
        }
    }

    fun attachToRootController(controller: DeponderController) {
        if (attachedController === controller) return
        attachedController?.unregisterPlanetBinding(this)
        attachedController = controller
        controller.registerPlanetBinding(this)
    }

    fun update(newId: String, newPhysics: PlanetPhysics) {
        val oldId = planetId
        val oldPhysics = physics
        if (oldId == newId && oldPhysics == newPhysics) return
        planetId = newId
        physics = newPhysics
        attachedController?.rebindPlanetIdOrPhysics(this, oldId = oldId, newId = newId)
    }

    fun hasLaidOutBounds(): Boolean =
        view.isLaidOut || view.width > 0 || view.height > 0 || view.right > view.left || view.bottom > view.top

    fun staticCenterX(): Float = view.left + view.width * 0.5f

    fun staticCenterY(): Float = view.top + view.height * 0.5f

    fun applyBodyPose(body: PlanetBody, scale: Float) {
        val w = view.width
        val h = view.height
        view.pivotX = w * 0.5f
        view.pivotY = h * 0.5f
        view.translationX = body.x - (view.left + w * 0.5f)
        view.translationY = body.y - (view.top + h * 0.5f)
        view.scaleX = scale
        view.scaleY = scale
        if (hasElevatedZ) {
            view.translationZ = elevatedTranslationZ
        }
    }

    fun onRootCancelled() {
        attachedController = null
        resetViewTransforms()
    }

    private fun detachFromControllerAndReset() {
        val ctrl = attachedController
        attachedController = null
        ctrl?.unregisterPlanetBinding(this)
        resetViewTransforms()
    }

    fun uninstall() {
        view.removeOnLayoutChangeListener(layoutListener)
        view.removeOnAttachStateChangeListener(attachListener)
        detachFromControllerAndReset()
    }

    private fun resetViewTransforms() {
        hasElevatedZ = false
        elevatedTranslationZ = 0f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationZ = 0f
    }
}

internal class RubberViewBinding(
    val view: View,
    var startId: String,
    var endId: String,
    var physics: RubberPhysics,
    var insets: DeponderRubberInsets,
) {
    var attachedController: DeponderController? = null
        private set

    private val cachedStretchXmlToken: String = view.resources.getString(R.string.deponder_stretch)

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        syncAdaptiveLengthAndRefreshPose()
    }

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val ctrl = requireDirectParentRootController(
                childView = view,
                operatorDescription = "deponderRubber('$startId', '$endId')",
            )
            attachToRootController(ctrl)
        }

        override fun onViewDetachedFromWindow(v: View) {
            detachFromControllerAndReset()
        }
    }

    fun install() {
        view.addOnLayoutChangeListener(layoutListener)
        view.addOnAttachStateChangeListener(attachListener)
        if (view.isAttachedToWindow) {
            val ctrl = requireDirectParentRootController(
                childView = view,
                operatorDescription = "deponderRubber('$startId', '$endId')",
            )
            attachToRootController(ctrl)
        }
    }

    fun attachToRootController(controller: DeponderController) {
        if (attachedController === controller) return
        attachedController?.unregisterRubberBinding(this)
        attachedController = controller
        controller.registerRubberBinding(this)
    }

    fun update(
        newStartId: String,
        newEndId: String,
        newPhysics: RubberPhysics,
        newInsets: DeponderRubberInsets,
    ) {
        val oldKey = EdgeKey(startId, endId)
        val topologyOrPhysicsChanged = startId != newStartId || endId != newEndId || physics != newPhysics
        val insetsChanged = insets != newInsets
        if (!topologyOrPhysicsChanged && !insetsChanged) return
        startId = newStartId
        endId = newEndId
        physics = newPhysics
        insets = newInsets
        val ctrl = attachedController ?: return
        if (topologyOrPhysicsChanged) {
            ctrl.rebindRubberEndpointsOrPhysics(this, oldKey)
        } else {
            refreshPoseFromController()
        }
    }

    fun updateInsetsOnly(newInsets: DeponderRubberInsets) {
        if (insets != newInsets) {
            insets = newInsets
            refreshPoseFromController()
        }
    }

    fun updatePhysicsOnly(newPhysics: RubberPhysics) {
        if (physics != newPhysics) {
            physics = newPhysics
            attachedController?.rebindRubberEndpointsOrPhysics(this, EdgeKey(startId, endId))
        }
    }

    internal fun currentAdaptiveContentWidthPx(): Float {
        if (view is DeponderRubberView) {
            return view.adaptiveContentWidthPx().toFloat()
        }
        val stretchXmlToken = attachedController?.xmlStretchToken ?: cachedStretchXmlToken
        return computeNonStretchedWidthPx(view, stretchXmlToken).toFloat()
    }

    internal fun syncAdaptiveLengthAndRefreshPose() {
        val ctrl = attachedController ?: return
        ctrl.syncRubberAdaptiveLength(this)
        refreshPoseFromController()
    }

    fun refreshPoseFromController() {
        val ctrl = attachedController ?: return
        val pose = ctrl.world.rubberOf(EdgeKey(startId, endId))
        applyRubberPose(pose)
    }

    private fun currentDensityPxPerDp(): Float =
        attachedController?.density ?: view.resources.displayMetrics.density.coerceAtLeast(1e-3f)

    private fun computeUnderLayerTranslationZ(): Float {
        val minPlanetZ = attachedController?.minPlanetVisualZOrZero() ?: 0f
        return minPlanetZ - view.elevation - 1f
    }

    private fun hideRubberLayer(underTranslationZ: Float) {
        view.alpha = 0f
        view.scaleX = 0f
        view.scaleY = 0f
        view.translationX = 0f
        view.translationY = 0f
        view.rotation = 0f
        view.translationZ = underTranslationZ
        if (view is DeponderRubberView) {
            view.applyDefaultRubberPose(pose = null, spanLengthPx = 0f, effectiveScale = 0f, outerScaleX = 0f)
        }
    }

    fun applyRubberPose(pose: RubberPose?) {
        val underTranslationZ = computeUnderLayerTranslationZ()
        if (pose == null || !pose.isActive || pose.centerDistance <= 1e-4f) {
            hideRubberLayer(underTranslationZ)
            return
        }

        val density = currentDensityPxPerDp()
        val spanLen = pose.resolvedLength(insets, density)
        if (spanLen <= 0f) {
            hideRubberLayer(underTranslationZ)
            return
        }

        val w = view.width
        val h = view.height
        val cx = pose.resolvedCenterX(insets, density)
        val cy = pose.resolvedCenterY(insets, density)

        // Built-in DeponderRubberView: expand outer RenderNode across `spanLen` so default
        // `rootView.clipChildren = true` never clips stretched lines or endpoint arrowheads!
        if (view is DeponderRubberView) {
            val nonStretchedW = view.nonStretchedContentWidthPx()
            val effectiveScale = computeRubberEffectiveScale(
                lengthPx = spanLen,
                nonStretchedWidthPx = nonStretchedW,
                poseScale = pose.scale,
            )
            if (effectiveScale <= 0f) {
                hideRubberLayer(underTranslationZ)
                return
            }
            val outerScaleX = if (w > 0) spanLen / w.toFloat() else 0f
            view.alpha = 1f
            view.translationZ = underTranslationZ
            view.pivotX = w * 0.5f
            view.pivotY = h * 0.5f
            view.translationX = cx - (view.left + w * 0.5f)
            view.translationY = cy - (view.top + h * 0.5f)
            view.rotation = pose.rotationDeg
            view.scaleX = outerScaleX
            view.scaleY = effectiveScale
            view.applyDefaultRubberPose(
                pose = pose,
                spanLengthPx = spanLen,
                effectiveScale = effectiveScale,
                outerScaleX = outerScaleX,
            )
            return
        }

        val stretchXmlToken = attachedController?.xmlStretchToken ?: cachedStretchXmlToken
        if (isViewStretchEnabled(view, stretchXmlToken)) {
            view.alpha = 1f
            view.translationZ = underTranslationZ
            view.pivotX = w * 0.5f
            view.pivotY = h * 0.5f
            view.translationX = cx - (view.left + w * 0.5f)
            view.translationY = cy - (view.top + h * 0.5f)
            view.rotation = pose.rotationDeg
            view.scaleX = computeViewStretchScaleX(
                lengthPx = spanLen,
                widthPx = w,
                parentScale = 1f,
            )
            view.scaleY = pose.scale
            if (view is ViewGroup) {
                resetCompoundRubberDescendants(view)
            }
            return
        }

        val nonStretchedW = computeNonStretchedWidthPx(view, stretchXmlToken)
        val effectiveScale = computeRubberEffectiveScale(
            lengthPx = spanLen,
            nonStretchedWidthPx = nonStretchedW,
            poseScale = pose.scale,
        )
        if (effectiveScale <= 0f) {
            hideRubberLayer(underTranslationZ)
            return
        }

        view.alpha = 1f
        view.translationZ = underTranslationZ
        view.pivotX = w * 0.5f
        view.pivotY = h * 0.5f
        view.translationX = cx - (view.left + w * 0.5f)
        view.translationY = cy - (view.top + h * 0.5f)
        view.rotation = pose.rotationDeg

        if (view is ViewGroup && w > 0 && containsStretchedDescendantOrSelf(view, stretchXmlToken)) {
            // Compound custom ViewGroup rubber: expand outer RenderNode across `spanLen`
            // so `rootView.clipChildren = true` never truncates inner stretched lines, while
            // counter-scaling non-stretched sibling labels/badges to exact `(effectiveScale, effectiveScale)`!
            val outerScaleX = spanLen / w.toFloat()
            val invScaleX = if (outerScaleX > 1e-6f) effectiveScale / outerScaleX else 0f
            view.scaleX = outerScaleX
            view.scaleY = effectiveScale
            applyCompoundRubberDescendantsTopDown(
                group = view,
                rubberRoot = view,
                rubberWidthPx = w,
                invScaleX = invScaleX,
                stretchXmlToken = stretchXmlToken,
            )
        } else {
            view.scaleX = effectiveScale
            view.scaleY = effectiveScale
            if (view is ViewGroup) {
                resetCompoundRubberDescendants(view)
            }
        }
    }

    fun onRootCancelled() {
        attachedController = null
        resetViewTransforms()
    }

    private fun detachFromControllerAndReset() {
        val ctrl = attachedController
        attachedController = null
        ctrl?.unregisterRubberBinding(this)
        resetViewTransforms()
    }

    fun uninstall() {
        view.removeOnLayoutChangeListener(layoutListener)
        view.removeOnAttachStateChangeListener(attachListener)
        detachFromControllerAndReset()
    }

    private fun resetViewTransforms() {
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationZ = 0f
        if (view is DeponderRubberView) {
            view.resetDefaultRubberPose()
        } else if (view is ViewGroup) {
            resetCompoundRubberDescendants(view)
        }
    }
}

internal fun assertNoStretchInPlanetSubtree(view: View, xmlStretchToken: String) {
    if (view.getTag(R.id.deponder_rubber_binding_tag) != null) return
    if (isViewStretchEnabled(view, xmlStretchToken)) {
        throw IllegalStateException(
            "deponderStretch() / @string/deponder_stretch must be applied directly to or nested inside a deponderRubber view.",
        )
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            assertNoStretchInPlanetSubtree(view.getChildAt(i), xmlStretchToken)
        }
    }
}

internal fun assertNoOrphanProgrammaticStretchInSubtree(view: View) {
    if (view.getTag(R.id.deponder_rubber_binding_tag) != null) return
    if ((view.getTag(R.id.deponder_stretch_state_tag) as? Boolean) == true) {
        throw IllegalStateException(
            "deponderStretch() must be applied directly to or nested inside a deponderRubber view.",
        )
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            assertNoOrphanProgrammaticStretchInSubtree(view.getChildAt(i))
        }
    }
}

private fun clearProgrammaticStretchTagsInSubtree(view: View) {
    if (view.getTag(R.id.deponder_stretch_state_tag) != null) {
        view.setTag(R.id.deponder_stretch_state_tag, null)
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            clearProgrammaticStretchTagsInSubtree(view.getChildAt(i))
        }
    }
}

private fun clearDragProbesInSubtree(view: View) {
    val dragBinding = view.getTag(R.id.deponder_drag_binding_tag) as? DragProbeBinding
    if (dragBinding != null) {
        dragBinding.uninstall()
        view.setTag(R.id.deponder_drag_binding_tag, null)
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            clearDragProbesInSubtree(view.getChildAt(i))
        }
    }
}

internal fun cancelActiveDragProbesInSubtree(view: View) {
    val dragBinding = view.getTag(R.id.deponder_drag_binding_tag) as? DragProbeBinding
    dragBinding?.cancelActiveDrag()
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            cancelActiveDragProbesInSubtree(view.getChildAt(i))
        }
    }
}

internal fun assertNoOrphanDragInSubtree(view: View) {
    if (view.getTag(R.id.deponder_planet_binding_tag) != null) return
    val dragBinding = view.getTag(R.id.deponder_drag_binding_tag) as? DragProbeBinding
    if (dragBinding != null && dragBinding.enabled) {
        throw IllegalStateException(
            "deponderDrag() must be applied directly to or nested inside a deponderPlanet view.",
        )
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            assertNoOrphanDragInSubtree(view.getChildAt(i))
        }
    }
}

private fun findEnclosingRubberBinding(startView: View): RubberViewBinding? {
    var cur: View? = startView
    while (cur != null) {
        val binding = cur.getTag(R.id.deponder_rubber_binding_tag) as? RubberViewBinding
        if (binding != null) return binding
        cur = cur.parent as? View
    }
    return null
}

private fun findEnclosingPlanetBinding(startView: View): PlanetViewBinding? {
    var cur: View? = startView
    while (cur != null) {
        val binding = cur.getTag(R.id.deponder_planet_binding_tag) as? PlanetViewBinding
        if (binding != null) return binding
        cur = cur.parent as? View
    }
    return null
}

private fun findEnclosingRootController(startView: View): DeponderController? {
    var cur: View? = startView.parent as? View
    while (cur != null) {
        val controller = cur.getTag(R.id.deponder_root_controller_tag) as? DeponderController
        if (controller != null && !controller.isCancelled) return controller
        cur = cur.parent as? View
    }
    return null
}

private fun requireDirectParentRootController(
    childView: View,
    operatorDescription: String,
): DeponderController {
    val immediateParent = childView.parent as? ViewGroup
        ?: throw IllegalStateException(
            "$operatorDescription view has no parent ViewGroup.",
        )
    val directController = immediateParent.getTag(R.id.deponder_root_controller_tag) as? DeponderController
    if (directController != null && !directController.isCancelled) {
        return directController
    }

    var ancestor = immediateParent.parent
    while (ancestor is View) {
        val higherController = ancestor.getTag(R.id.deponder_root_controller_tag) as? DeponderController
        if (higherController != null && !higherController.isCancelled) {
            throw IllegalStateException(
                "$operatorDescription view must be a DIRECT child of deponderRoot " +
                    "(${ancestor.javaClass.simpleName}), but its immediate parent is ${immediateParent.javaClass.simpleName}.",
            )
        }
        ancestor = ancestor.parent
    }

    throw IllegalStateException(
        "$operatorDescription view must be a direct child of a ViewGroup initialized with deponderRoot().",
    )
}

private fun isViewStretchEnabled(view: View, xmlStretchToken: String): Boolean {
    val explicitFlag = view.getTag(R.id.deponder_stretch_state_tag) as? Boolean
    if (explicitFlag != null) return explicitFlag
    return view.tag == xmlStretchToken
}

private fun containsStretchedDescendantOrSelf(view: View, stretchXmlToken: String): Boolean {
    if (isViewStretchEnabled(view, stretchXmlToken)) return true
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            if (containsStretchedDescendantOrSelf(view.getChildAt(i), stretchXmlToken)) {
                return true
            }
        }
    }
    return false
}

/**
 * Measures the widest non-stretched element inside a custom rubber so that when two planets move closer
 * than a custom badge or label's natural width, the rubber uniformly scales down to fit cleanly inside
 * `[S', E']` without being occluded under either planet, while ignoring stretched line children.
 */
internal fun computeNonStretchedWidthPx(view: View, stretchXmlToken: String): Int {
    if (view.visibility == View.GONE || isViewStretchEnabled(view, stretchXmlToken)) return 0
    if (view !is ViewGroup) return view.width
    var maxW = 0
    for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        if (child.visibility == View.GONE) continue
        val childW = if (containsStretchedDescendantOrSelf(child, stretchXmlToken)) {
            computeNonStretchedWidthPx(child, stretchXmlToken)
        } else {
            child.width
        }
        if (childW > maxW) maxW = childW
    }
    return maxW
}

internal fun computeRubberEffectiveScale(
    lengthPx: Float,
    nonStretchedWidthPx: Int,
    poseScale: Float,
): Float {
    if (lengthPx <= 0f || poseScale <= 0f) return 0f
    if (nonStretchedWidthPx > 0 && lengthPx < nonStretchedWidthPx * poseScale) {
        return (lengthPx / nonStretchedWidthPx.toFloat()).coerceAtLeast(0f)
    }
    return poseScale
}

/**
 * Applies top-down transforms inside a compound [rubberRoot] whose outer `RenderNode` already spans
 * the resolved link length (`outerScaleX = spanLen / rubberWidthPx`):
 * - Stretched children scale to fill `[0, rubberWidthPx]` (`scaleX = rubberWidthPx / childW`),
 *   mapping to `spanLen` on screen with ancestor pruning.
 * - Non-stretched sibling subtrees counter-scale by `invScaleX = effectiveScale / outerScaleX`,
 *   rendering at exact undistorted `(effectiveScale, effectiveScale)` on screen without clipping.
 */
private fun applyCompoundRubberDescendantsTopDown(
    group: ViewGroup,
    rubberRoot: ViewGroup,
    rubberWidthPx: Int,
    invScaleX: Float,
    stretchXmlToken: String,
) {
    for (i in 0 until group.childCount) {
        val child = group.getChildAt(i)
        val childW = child.width
        val childH = child.height
        if (isViewStretchEnabled(child, stretchXmlToken)) {
            val staticLeftInRubber = computeStaticLeftInAncestor(child, rubberRoot)
            child.pivotX = childW * 0.5f
            child.pivotY = childH * 0.5f
            child.scaleX = if (childW > 0 && rubberWidthPx > 0) {
                rubberWidthPx.toFloat() / childW.toFloat()
            } else {
                0f
            }
            child.scaleY = 1f
            child.translationX = rubberWidthPx * 0.5f - (staticLeftInRubber + childW * 0.5f)
            child.translationY = 0f
            // Ancestor pruning: any deeper descendants inside a stretched branch stay unscaled.
            if (child is ViewGroup) {
                resetCompoundRubberDescendants(child)
            }
        } else if (child is ViewGroup && containsStretchedDescendantOrSelf(child, stretchXmlToken)) {
            child.scaleX = 1f
            child.scaleY = 1f
            child.translationX = 0f
            child.translationY = 0f
            applyCompoundRubberDescendantsTopDown(
                group = child,
                rubberRoot = rubberRoot,
                rubberWidthPx = rubberWidthPx,
                invScaleX = invScaleX,
                stretchXmlToken = stretchXmlToken,
            )
        } else {
            val staticLeftInRubber = computeStaticLeftInAncestor(child, rubberRoot)
            val dxFromCenter = (staticLeftInRubber + childW * 0.5f) - rubberWidthPx * 0.5f
            child.pivotX = childW * 0.5f
            child.pivotY = childH * 0.5f
            child.scaleX = invScaleX
            child.scaleY = 1f
            child.translationX = dxFromCenter * (invScaleX - 1f)
            child.translationY = 0f
            if (child is ViewGroup) {
                resetCompoundRubberDescendants(child)
            }
        }
    }
}

private fun resetCompoundRubberDescendants(group: ViewGroup) {
    for (i in 0 until group.childCount) {
        val child = group.getChildAt(i)
        child.scaleX = 1f
        child.scaleY = 1f
        child.translationX = 0f
        child.translationY = 0f
        if (child is ViewGroup) {
            resetCompoundRubberDescendants(child)
        }
    }
}

private fun computeStaticLeftInAncestor(child: View, ancestor: ViewGroup): Float {
    var sumLeft = 0f
    var cur: View = child
    while (cur !== ancestor) {
        sumLeft += cur.left
        val parent = cur.parent as? View ?: break
        sumLeft -= parent.scrollX
        cur = parent
    }
    return sumLeft
}

internal fun computeViewStretchScaleX(
    lengthPx: Float,
    widthPx: Int,
    parentScale: Float,
): Float {
    if (widthPx <= 0 || parentScale <= 0f || lengthPx <= 0f) return 0f
    return lengthPx / (widthPx.toFloat() * parentScale)
}

/**
 * Projects a pointer coordinate `(localX, localY)` from [sourceView] (which may be a transformed planet
 * or a nested handle view) through every ancestor `View.matrix` up to static [rootView] pixel space.
 *
 * Works across all pointer indices (`0..pointerCount-1`) down to Android API 23 (`minSdk = 23`).
 */
internal fun mapViewPointToRootStaticSpace(
    sourceView: View,
    rootView: ViewGroup,
    localX: Float,
    localY: Float,
    scratchPoint: FloatArray,
): Unit {
    var x = localX
    var y = localY
    var current: View = sourceView
    while (current !== rootView) {
        if (!current.matrix.isIdentity) {
            scratchPoint[0] = x
            scratchPoint[1] = y
            current.matrix.mapPoints(scratchPoint)
            x = scratchPoint[0]
            y = scratchPoint[1]
        }
        val parent = current.parent as? View ?: break
        x += current.left - parent.scrollX
        y += current.top - parent.scrollY
        current = parent
    }
    scratchPoint[0] = x
    scratchPoint[1] = y
}

internal class DragProbeBinding(
    private val sourceView: View,
    var enabled: Boolean,
    var bringToFrontOnPress: Boolean,
) : View.OnTouchListener {

    private val scratchPoint = FloatArray(2)
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var downRootX: Float = 0f
    private var downRootY: Float = 0f
    private var lastRootX: Float = 0f
    private var lastRootY: Float = 0f
    private var grabOffsetX: Float = 0f
    private var grabOffsetY: Float = 0f
    private var isDragging: Boolean = false
    private var activePlanetId: String? = null
    private var listenersAttached: Boolean = false

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit

        override fun onViewDetachedFromWindow(v: View) {
            cancelActiveDrag()
        }
    }

    private fun ensureListenersAttached() {
        if (!listenersAttached) {
            sourceView.addOnAttachStateChangeListener(attachListener)
            sourceView.setOnTouchListener(this)
            listenersAttached = true
        }
    }

    private fun detachListeners() {
        if (listenersAttached) {
            sourceView.removeOnAttachStateChangeListener(attachListener)
            sourceView.setOnTouchListener(null)
            listenersAttached = false
        }
    }

    fun install() {
        if (!enabled) return
        if (findEnclosingRubberBinding(sourceView) != null) {
            throw IllegalStateException(
                "deponderDrag() must be applied directly to or nested inside a deponderPlanet view, not a deponderRubber.",
            )
        }
        ensureListenersAttached()
    }

    fun update(newEnabled: Boolean, newBringToFrontOnPress: Boolean) {
        enabled = newEnabled
        bringToFrontOnPress = newBringToFrontOnPress
        if (!newEnabled) {
            cancelActiveDrag()
            detachListeners()
        } else {
            if (findEnclosingRubberBinding(sourceView) != null) {
                throw IllegalStateException(
                    "deponderDrag() must be applied directly to or nested inside a deponderPlanet view, not a deponderRubber.",
                )
            }
            ensureListenersAttached()
        }
    }

    fun uninstall() {
        cancelActiveDrag()
        detachListeners()
    }

    private fun cancelNativeViewPressState(v: View, sourceEvent: MotionEvent?) {
        if (sourceEvent != null && (v.isClickable || v.isLongClickable)) {
            val cancelEvent = MotionEvent.obtain(sourceEvent)
            cancelEvent.action = MotionEvent.ACTION_CANCEL
            v.onTouchEvent(cancelEvent)
            cancelEvent.recycle()
        }
        if (v.isPressed) {
            v.isPressed = false
        }
        v.cancelPendingInputEvents()
    }

    internal fun cancelActiveDrag() {
        if (isDragging) {
            if (sourceView.isPressed) {
                sourceView.isPressed = false
            }
            val planetBinding = findEnclosingPlanetBinding(sourceView)
            val ctrl = planetBinding?.attachedController
            val id = activePlanetId ?: planetBinding?.planetId
            if (ctrl != null && id != null) {
                ctrl.setPressed(id, false)
            }
        }
        isDragging = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
        activePlanetId = null
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled) return false
        val planetBinding = findEnclosingPlanetBinding(v) ?: return false
        val controller = planetBinding.attachedController ?: return false
        val rootView = controller.rootView
        val hasNativeClick = v.isClickable || v.isLongClickable

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                activePlanetId = planetBinding.planetId
                isDragging = false

                // Prevent outer ScrollView / ViewPager2 / RecyclerView containers from hijacking
                // planet drags before scaledTouchSlop is crossed.
                v.parent?.requestDisallowInterceptTouchEvent(true)

                if (bringToFrontOnPress) {
                    controller.bringToFront(planetBinding.planetId)
                }

                mapViewPointToRootStaticSpace(
                    sourceView = v,
                    rootView = rootView,
                    localX = event.getX(0),
                    localY = event.getY(0),
                    scratchPoint = scratchPoint,
                )
                downRootX = scratchPoint[0]
                downRootY = scratchPoint[1]
                lastRootX = downRootX
                lastRootY = downRootY

                // If `v` is natively clickable/longClickable, returning false lets View.onTouchEvent drive
                // Material ripples, pressed state, and click callbacks while still receiving ACTION_MOVE!
                return !hasNativeClick
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                return isDragging || !hasNativeClick
            }

            MotionEvent.ACTION_MOVE -> {
                val ptrIdx = event.findPointerIndex(activePointerId)
                if (ptrIdx < 0) return isDragging || !hasNativeClick

                mapViewPointToRootStaticSpace(
                    sourceView = v,
                    rootView = rootView,
                    localX = event.getX(ptrIdx),
                    localY = event.getY(ptrIdx),
                    scratchPoint = scratchPoint,
                )
                val curRootX = scratchPoint[0]
                val curRootY = scratchPoint[1]

                if (!isDragging) {
                    val totalDx = curRootX - downRootX
                    val totalDy = curRootY - downRootY
                    val dist = hypot(totalDx.toDouble(), totalDy.toDouble()).toFloat()
                    val touchSlop = ViewConfiguration.get(v.context).scaledTouchSlop.toFloat()
                    if (dist >= touchSlop) {
                        isDragging = true
                        activePlanetId = planetBinding.planetId
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        cancelNativeViewPressState(v, event)
                        controller.setPressed(planetBinding.planetId, true)
                        val body = controller.world.bodyOf(planetBinding.planetId)
                        if (body != null) {
                            val ratio = if (dist > 0f) touchSlop / dist else 0f
                            val slopRootX = downRootX + totalDx * ratio
                            val slopRootY = downRootY + totalDy * ratio
                            grabOffsetX = body.x - slopRootX
                            grabOffsetY = body.y - slopRootY
                            controller.moveTo(
                                id = planetBinding.planetId,
                                xPx = curRootX + grabOffsetX,
                                yPx = curRootY + grabOffsetY,
                            )
                        }
                        lastRootX = curRootX
                        lastRootY = curRootY
                        return true
                    }
                    lastRootX = curRootX
                    lastRootY = curRootY
                    return !hasNativeClick
                } else {
                    if (curRootX != lastRootX || curRootY != lastRootY) {
                        controller.moveTo(
                            id = planetBinding.planetId,
                            xPx = curRootX + grabOffsetX,
                            yPx = curRootY + grabOffsetY,
                        )
                        lastRootX = curRootX
                        lastRootY = curRootY
                    }
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val actionIndex = event.actionIndex
                val liftedId = event.getPointerId(actionIndex)
                if (liftedId == activePointerId) {
                    val newIndex = if (actionIndex == 0) 1 else 0
                    if (newIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newIndex)
                        mapViewPointToRootStaticSpace(
                            sourceView = v,
                            rootView = rootView,
                            localX = event.getX(newIndex),
                            localY = event.getY(newIndex),
                            scratchPoint = scratchPoint,
                        )
                        lastRootX = scratchPoint[0]
                        lastRootY = scratchPoint[1]
                        if (!isDragging) {
                            downRootX = lastRootX
                            downRootY = lastRootY
                        } else {
                            val body = controller.world.bodyOf(planetBinding.planetId)
                            if (body != null) {
                                grabOffsetX = body.x - lastRootX
                                grabOffsetY = body.y - lastRootY
                            }
                        }
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
                return isDragging || !hasNativeClick
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = isDragging
                if (wasDragging) {
                    if (v.isPressed) {
                        v.isPressed = false
                    }
                    val lockedId = activePlanetId
                    if (lockedId != null) {
                        controller.setPressed(lockedId, false)
                    }
                    if (planetBinding.planetId != lockedId) {
                        controller.setPressed(planetBinding.planetId, false)
                    }
                }
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                activePlanetId = null
                if (wasDragging) {
                    return true
                }
                if (!hasNativeClick) {
                    v.performClick()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDragging
                if (wasDragging) {
                    if (v.isPressed) {
                        v.isPressed = false
                    }
                    val lockedId = activePlanetId
                    if (lockedId != null) {
                        controller.setPressed(lockedId, false)
                    }
                    if (planetBinding.planetId != lockedId) {
                        controller.setPressed(planetBinding.planetId, false)
                    }
                }
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                activePlanetId = null
                return wasDragging || !hasNativeClick
            }

            else -> return isDragging || !hasNativeClick
        }
    }
}
