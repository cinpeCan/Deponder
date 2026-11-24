import SwiftUI

internal enum DeponderCoordinateSpace {
    static let root = "com.cinpe.deponder.root"
}

private struct DeponderStateEnvironmentKey: EnvironmentKey {
    static let defaultValue: DeponderState? = nil
}

private struct DeponderPlanetIdEnvironmentKey: EnvironmentKey {
    static let defaultValue: String? = nil
}

private struct DeponderRubberTokenEnvironmentKey: EnvironmentKey {
    static let defaultValue: UUID? = nil
}

private struct DeponderInsideStretchEnvironmentKey: EnvironmentKey {
    static let defaultValue: Bool = false
}

extension EnvironmentValues {
    public var deponderState: DeponderState? {
        get { self[DeponderStateEnvironmentKey.self] }
        set { self[DeponderStateEnvironmentKey.self] = newValue }
    }

    internal var deponderPlanetId: String? {
        get { self[DeponderPlanetIdEnvironmentKey.self] }
        set { self[DeponderPlanetIdEnvironmentKey.self] = newValue }
    }

    internal var deponderRubberToken: UUID? {
        get { self[DeponderRubberTokenEnvironmentKey.self] }
        set { self[DeponderRubberTokenEnvironmentKey.self] = newValue }
    }

    internal var deponderInsideStretch: Bool {
        get { self[DeponderInsideStretchEnvironmentKey.self] }
        set { self[DeponderInsideStretchEnvironmentKey.self] = newValue }
    }
}

private struct DeponderPlanetIdPreferenceKey: PreferenceKey {
    static let defaultValue: String? = nil
    static func reduce(value: inout String?, nextValue: () -> String?) {
        value = nextValue() ?? value
    }
}

private struct DeponderRubberDirectStretchPreferenceKey: PreferenceKey {
    static let defaultValue: Bool = false
    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

private struct DeponderRootModifier: ViewModifier {
    let state: DeponderState
    @State private var hostToken = UUID()

    func body(content: Content) -> some View {
        content
            .environment(\.deponderState, state)
            .coordinateSpace(name: DeponderCoordinateSpace.root)
            .onGeometryChange(for: CGSize.self) { proxy in
                proxy.size
            } action: { newSize in
                state.updateViewportSize(
                    width: Float(newSize.width),
                    height: Float(newSize.height)
                )
            }
            .onAppear {
                state.attachHost(hostToken)
            }
            .onDisappear {
                state.detachHost(hostToken)
            }
    }
}

private struct DeponderPlanetModifier: ViewModifier {
    @Environment(\.deponderState) private var state
    @Environment(\.deponderPlanetId) private var parentPlanetId

    let id: String
    let physics: PlanetPhysics

    @State private var planetToken = UUID()
    @State private var baseFrame: CGRect = .zero

    func body(content: Content) -> some View {
        let _ = assertValidPlacement()
        let slot = state?.planetSlot(token: planetToken)
        let visualOffset = slot.map { CGSize(width: $0.offsetX, height: $0.offsetY) } ?? .zero
        let visualScale = slot?.scale ?? CGFloat(1.0)
        let zOrder = slot?.zOrder ?? 0

        return content
            .environment(\.deponderPlanetId, id)
            .scaleEffect(visualScale, anchor: .center)
            .offset(visualOffset)
            .onGeometryChange(for: CGRect.self) { proxy in
                proxy.frame(in: .named(DeponderCoordinateSpace.root))
            } action: { newFrame in
                baseFrame = newFrame
                syncPlanet(frame: newFrame)
            }
            .onChange(of: id) { _, _ in
                if baseFrame != .zero {
                    syncPlanet(frame: baseFrame)
                }
            }
            .onChange(of: physics) { _, _ in
                if baseFrame != .zero {
                    syncPlanet(frame: baseFrame)
                }
            }
            .onAppear {
                if baseFrame != .zero {
                    syncPlanet(frame: baseFrame)
                }
            }
            .onDisappear {
                state?.unregisterPlanet(token: planetToken)
            }
            .zIndex(zOrder)
            .preference(key: DeponderPlanetIdPreferenceKey.self, value: id)
    }

    private func assertValidPlacement() {
        precondition(state != nil, "deponderPlanet must be placed inside a container modified with deponderRoot.")
        precondition(parentPlanetId == nil, "Nested deponderPlanet inside another deponderPlanet ('\(parentPlanetId ?? "")') is not supported.")
    }

    private func syncPlanet(frame: CGRect) {
        guard let state else { return }
        state.syncPlanet(
            token: planetToken,
            id: id,
            physics: physics,
            baseCenterX: Float(frame.midX),
            baseCenterY: Float(frame.midY),
            width: Float(frame.width),
            height: Float(frame.height)
        )
    }
}

private struct DeponderDragModifier: ViewModifier {
    @Environment(\.deponderState) private var state
    @Environment(\.deponderPlanetId) private var envPlanetId

    let enabled: Bool
    let bringToFrontOnPress: Bool
    let touchSlop: CGFloat

    @State private var childPlanetId: String?
    @State private var isDragging: Bool = false
    @State private var grabOffsetX: Float = 0
    @State private var grabOffsetY: Float = 0

    private var targetPlanetId: String? {
        envPlanetId ?? childPlanetId
    }

    func body(content: Content) -> some View {
        content
            .onPreferenceChange(DeponderPlanetIdPreferenceKey.self) { detectedId in
                childPlanetId = detectedId
            }
            .simultaneousGesture(
                DragGesture(
                    minimumDistance: touchSlop,
                    coordinateSpace: .named(DeponderCoordinateSpace.root)
                )
                .onChanged { value in
                    guard enabled, let state, let targetId = targetPlanetId else { return }
                    if !isDragging {
                        isDragging = true
                        if bringToFrontOnPress {
                            state.bringToFront(id: targetId)
                        }
                        state.setPressed(id: targetId, pressed: true)

                        let totalDeltaX = value.location.x - value.startLocation.x
                        let totalDeltaY = value.location.y - value.startLocation.y
                        let dist = hypot(totalDeltaX, totalDeltaY)
                        let slopAdjustedX: CGFloat
                        let slopAdjustedY: CGFloat
                        if dist > 0 {
                            let step = min(dist, touchSlop)
                            slopAdjustedX = value.startLocation.x + (totalDeltaX / dist) * step
                            slopAdjustedY = value.startLocation.y + (totalDeltaY / dist) * step
                        } else {
                            slopAdjustedX = value.location.x
                            slopAdjustedY = value.location.y
                        }
                        if let body = state.world.bodyOf(id: targetId) {
                            grabOffsetX = body.x - Float(slopAdjustedX)
                            grabOffsetY = body.y - Float(slopAdjustedY)
                        } else {
                            grabOffsetX = 0
                            grabOffsetY = 0
                        }
                    }
                    state.moveTo(
                        id: targetId,
                        x: Float(value.location.x) + grabOffsetX,
                        y: Float(value.location.y) + grabOffsetY
                    )
                }
                .onEnded { _ in
                    guard let state else {
                        isDragging = false
                        grabOffsetX = 0
                        grabOffsetY = 0
                        return
                    }
                    if isDragging, let targetId = targetPlanetId {
                        state.setPressed(id: targetId, pressed: false)
                    }
                    isDragging = false
                    grabOffsetX = 0
                    grabOffsetY = 0
                },
                including: enabled ? .all : .subviews
            )
            .onDisappear {
                if isDragging, let state, let targetId = targetPlanetId {
                    state.setPressed(id: targetId, pressed: false)
                }
                isDragging = false
                grabOffsetX = 0
                grabOffsetY = 0
            }
    }
}

private struct DeponderRubberModifier: ViewModifier {
    @Environment(\.deponderState) private var state
    @Environment(\.deponderPlanetId) private var parentPlanetId
    @Environment(\.deponderRubberToken) private var parentRubberToken

    let startId: String
    let endId: String
    let physics: RubberPhysics
    let insets: DeponderRubberInsets
    let explicitToken: UUID?
    let explicitNonStretchedWidth: Float
    let explicitAdaptiveContentWidth: Float

    @State private var fallbackToken = UUID()
    @State private var baseFrame: CGRect = .zero
    @State private var isDirectStretch: Bool = false

    private var activeToken: UUID {
        explicitToken ?? fallbackToken
    }

    func body(content: Content) -> some View {
        let token = activeToken
        let _ = assertValidPlacement()
        let slot = state?.rubberSlot(token: token)
        let transform = computeRubberTransform(slot: slot, baseFrame: baseFrame)

        return content
            .coordinateSpace(name: token)
            .environment(\.deponderRubberToken, token)
            .onPreferenceChange(DeponderRubberDirectStretchPreferenceKey.self) { direct in
                if isDirectStretch != direct {
                    isDirectStretch = direct
                    if baseFrame != .zero {
                        syncRubber(frame: baseFrame, directStretch: direct)
                    }
                }
            }
            .scaleEffect(
                x: transform.scaleX,
                y: transform.scaleY,
                anchor: .center
            )
            .rotationEffect(.degrees(transform.rotationDeg), anchor: .center)
            .offset(x: transform.offsetX, y: transform.offsetY)
            .opacity(transform.visible ? 1 : 0)
            .allowsHitTesting(transform.visible)
            .zIndex(-1)
            .onGeometryChange(for: CGRect.self) { proxy in
                proxy.frame(in: .named(DeponderCoordinateSpace.root))
            } action: { newFrame in
                baseFrame = newFrame
                syncRubber(frame: newFrame, directStretch: isDirectStretch)
            }
            .onChange(of: startId) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onChange(of: endId) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onChange(of: physics) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onChange(of: insets) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onChange(of: explicitNonStretchedWidth) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onChange(of: explicitAdaptiveContentWidth) { _, _ in
                if baseFrame != .zero { syncRubber(frame: baseFrame, directStretch: isDirectStretch) }
            }
            .onAppear {
                if baseFrame != .zero {
                    syncRubber(frame: baseFrame, directStretch: isDirectStretch)
                }
            }
            .onDisappear {
                state?.unregisterRubber(token: token)
            }
    }

    private func assertValidPlacement() {
        precondition(state != nil, "deponderRubber must be placed inside a container modified with deponderRoot.")
        precondition(parentPlanetId == nil, "deponderRubber('\(startId)', '\(endId)') cannot be nested inside a deponderPlanet.")
        precondition(parentRubberToken == nil, "deponderRubber('\(startId)', '\(endId)') cannot be nested inside another deponderRubber.")
    }

    private func syncRubber(frame: CGRect, directStretch: Bool) {
        guard let state else { return }
        state.syncRubber(
            token: activeToken,
            startId: startId,
            endId: endId,
            physics: physics,
            insets: insets,
            width: Float(frame.width),
            height: Float(frame.height),
            explicitNonStretchedWidth: explicitNonStretchedWidth,
            explicitAdaptiveContentWidth: explicitAdaptiveContentWidth,
            isDirectStretch: directStretch
        )
    }

    private func computeRubberTransform(
        slot: RubberSlot?,
        baseFrame: CGRect
    ) -> (visible: Bool, offsetX: CGFloat, offsetY: CGFloat, rotationDeg: Double, scaleX: CGFloat, scaleY: CGFloat) {
        guard let slot, slot.visible, baseFrame != .zero, slot.spanLength > 0, slot.effectiveScale > 0 else {
            return (false, 0, 0, 0, 0, 0)
        }

        let scaleX: CGFloat
        let scaleY: CGFloat
        if isDirectStretch {
            scaleX = CGFloat(
                DeponderStretchMath.computeStretchScaleX(
                    lengthPx: slot.spanLength,
                    widthPx: Float(baseFrame.width),
                    parentScale: 1.0
                )
            )
            scaleY = CGFloat(slot.poseScale)
        } else {
            scaleX = CGFloat(slot.effectiveScale)
            scaleY = CGFloat(slot.effectiveScale)
        }

        return (
            visible: true,
            offsetX: slot.centerX - baseFrame.midX,
            offsetY: slot.centerY - baseFrame.midY,
            rotationDeg: slot.rotationDeg,
            scaleX: scaleX,
            scaleY: scaleY
        )
    }
}

private struct DeponderStretchModifier: ViewModifier {
    @Environment(\.deponderState) private var state
    @Environment(\.deponderRubberToken) private var rubberToken
    @Environment(\.deponderInsideStretch) private var insideStretch

    let enabled: Bool

    @State private var stretchToken = UUID()
    @State private var childFrameInRubber: CGRect = .zero

    @ViewBuilder
    func body(content: Content) -> some View {
        if let rubberToken {
            if insideStretch {
                content
            } else {
                let placement = computeChildStretchPlacement(rubberToken: rubberToken)
                content
                    .environment(\.deponderInsideStretch, true)
                    .scaleEffect(x: placement.scaleX, y: 1.0, anchor: .center)
                    .offset(x: placement.alignTx, y: 0)
                    .onGeometryChange(for: CGRect.self) { proxy in
                        proxy.frame(in: .named(rubberToken))
                    } action: { newFrame in
                        childFrameInRubber = newFrame
                        syncRegistration()
                    }
                    .onChange(of: enabled) { _, _ in
                        syncRegistration()
                    }
                    .onDisappear {
                        state?.unregisterRubberStretchChild(
                            rubberToken: rubberToken,
                            childToken: stretchToken
                        )
                    }
            }
        } else {
            // Applied directly upstream to `.deponderRubber(...)`: delegate as direct stretch when enabled.
            content.preference(key: DeponderRubberDirectStretchPreferenceKey.self, value: enabled)
        }
    }

    private func syncRegistration() {
        guard let state, let rubberToken else { return }
        if enabled {
            state.registerRubberStretchChild(
                rubberToken: rubberToken,
                childToken: stretchToken,
                width: Float(childFrameInRubber.width)
            )
        } else {
            state.unregisterRubberStretchChild(
                rubberToken: rubberToken,
                childToken: stretchToken
            )
        }
    }

    private func computeChildStretchPlacement(rubberToken: UUID) -> (scaleX: CGFloat, alignTx: CGFloat) {
        guard enabled, let state else { return (1, 0) }
        guard childFrameInRubber.width > 0 else { return (0, 0) }
        let slot = state.rubberSlot(token: rubberToken)
        guard slot.visible else { return (0, 0) }

        let scaleX = DeponderStretchMath.computeStretchScaleX(
            lengthPx: slot.spanLength,
            widthPx: Float(childFrameInRubber.width),
            parentScale: slot.effectiveScale
        )
        let alignTx = DeponderStretchMath.computeChildStretchCenterAlignTx(
            rubberWidthPx: slot.measuredWidth,
            childLeftInRubberPx: Float(childFrameInRubber.minX),
            childWidthPx: Float(childFrameInRubber.width)
        )
        return (CGFloat(scaleX), CGFloat(alignTx))
    }
}

public enum DeponderStretchMath {
    public static func computeStretchScaleX(
        lengthPx: Float,
        widthPx: Float,
        parentScale: Float
    ) -> Float {
        guard widthPx > 0, parentScale > 1e-4, lengthPx > 0 else { return 0 }
        return lengthPx / (widthPx * parentScale)
    }

    public static func computeChildStretchCenterAlignTx(
        rubberWidthPx: Float,
        childLeftInRubberPx: Float,
        childWidthPx: Float
    ) -> Float {
        (rubberWidthPx * 0.5) - (childLeftInRubberPx + childWidthPx * 0.5)
    }
}

extension View {
    public func deponderRoot(_ state: DeponderState) -> some View {
        modifier(DeponderRootModifier(state: state))
    }

    public func deponderPlanet(
        id: String,
        physics: PlanetPhysics = .default
    ) -> some View {
        modifier(DeponderPlanetModifier(id: id, physics: physics))
    }

    public func deponderDrag(
        enabled: Bool = true,
        bringToFrontOnPress: Bool = true,
        touchSlop: CGFloat = 8
    ) -> some View {
        modifier(
            DeponderDragModifier(
                enabled: enabled,
                bringToFrontOnPress: bringToFrontOnPress,
                touchSlop: touchSlop
            )
        )
    }

    public func deponderRubber(
        startId: String,
        endId: String,
        physics: RubberPhysics = .default,
        insets: DeponderRubberInsets = .default
    ) -> some View {
        modifier(
            DeponderRubberModifier(
                startId: startId,
                endId: endId,
                physics: physics,
                insets: insets,
                explicitToken: nil,
                explicitNonStretchedWidth: -1,
                explicitAdaptiveContentWidth: -1
            )
        )
    }

    public func deponderStretch(enabled: Bool = true) -> some View {
        modifier(DeponderStretchModifier(enabled: enabled))
    }
}

internal extension View {
    func deponderRubberInternal(
        token: UUID,
        startId: String,
        endId: String,
        physics: RubberPhysics,
        insets: DeponderRubberInsets,
        explicitNonStretchedWidth: Float,
        explicitAdaptiveContentWidth: Float
    ) -> some View {
        modifier(
            DeponderRubberModifier(
                startId: startId,
                endId: endId,
                physics: physics,
                insets: insets,
                explicitToken: token,
                explicitNonStretchedWidth: explicitNonStretchedWidth,
                explicitAdaptiveContentWidth: explicitAdaptiveContentWidth
            )
        )
    }
}
