import Foundation
import Observation
import QuartzCore
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

@MainActor
@Observable
internal final class PlanetSlot {
    var offsetX: CGFloat = 0
    var offsetY: CGFloat = 0
    var scale: CGFloat = 1
    var zOrder: Double = 0

    func update(offsetX: CGFloat, offsetY: CGFloat, scale: CGFloat, zOrder: Double) {
        if abs(self.offsetX - offsetX) > 1e-4 { self.offsetX = offsetX }
        if abs(self.offsetY - offsetY) > 1e-4 { self.offsetY = offsetY }
        if abs(self.scale - scale) > 1e-4 { self.scale = scale }
        if self.zOrder != zOrder { self.zOrder = zOrder }
    }
}

@MainActor
@Observable
internal final class RubberSlot {
    var visible: Bool = false
    var centerX: CGFloat = 0
    var centerY: CGFloat = 0
    var spanLength: Float = 0
    var rotationDeg: Double = 0
    var effectiveScale: Float = 1
    var poseScale: Float = 1
    var measuredWidth: Float = 0

    func update(
        visible: Bool,
        centerX: CGFloat,
        centerY: CGFloat,
        spanLength: Float,
        rotationDeg: Double,
        effectiveScale: Float,
        poseScale: Float,
        measuredWidth: Float
    ) {
        if self.visible != visible { self.visible = visible }
        if abs(self.centerX - centerX) > 1e-4 { self.centerX = centerX }
        if abs(self.centerY - centerY) > 1e-4 { self.centerY = centerY }
        if abs(self.spanLength - spanLength) > 1e-4 { self.spanLength = spanLength }
        if abs(self.rotationDeg - rotationDeg) > 1e-4 { self.rotationDeg = rotationDeg }
        if abs(self.effectiveScale - effectiveScale) > 1e-4 { self.effectiveScale = effectiveScale }
        if abs(self.poseScale - poseScale) > 1e-4 { self.poseScale = poseScale }
        if abs(self.measuredWidth - measuredWidth) > 1e-4 { self.measuredWidth = measuredWidth }
    }
}

@MainActor
@Observable
public final class DeponderState {

    internal final class PlanetRecord {
        let token: UUID
        let slot: PlanetSlot
        var id: String
        var physics: PlanetPhysics
        var baseCenterX: Float = 0
        var baseCenterY: Float = 0
        var hasBaseOrigin: Bool = false
        var measuredWidth: Float = 0
        var measuredHeight: Float = 0

        init(token: UUID, slot: PlanetSlot, id: String, physics: PlanetPhysics) {
            self.token = token
            self.slot = slot
            self.id = id
            self.physics = physics
        }
    }

    internal final class RubberRecord {
        let token: UUID
        let slot: RubberSlot
        var startId: String
        var endId: String
        var edgeKey: EdgeKey
        var physics: RubberPhysics
        var insets: DeponderRubberInsets
        var measuredWidth: Float = 0
        var measuredHeight: Float = 0
        var explicitNonStretchedWidth: Float = -1
        var explicitAdaptiveContentWidth: Float = -1
        var stretchChildWidths: [UUID: Float] = [:]
        var isDirectStretch: Bool = false

        init(
            token: UUID,
            slot: RubberSlot,
            startId: String,
            endId: String,
            physics: RubberPhysics,
            insets: DeponderRubberInsets
        ) {
            self.token = token
            self.slot = slot
            self.startId = startId
            self.endId = endId
            self.edgeKey = EdgeKey(a: startId, b: endId)
            self.physics = physics
            self.insets = insets
        }

        var hasStretchChildren: Bool {
            !stretchChildWidths.isEmpty
        }

        func resolvedNonStretchedWidth() -> Float {
            if explicitNonStretchedWidth >= 0 {
                return explicitNonStretchedWidth
            }
            let maxInnerStretchW = stretchChildWidths.values.max() ?? -1
            if maxInnerStretchW < 0 {
                return measuredWidth
            } else if measuredWidth > maxInnerStretchW {
                return measuredWidth
            } else {
                return 0
            }
        }

        func resolvedAdaptiveContentWidth() -> Float {
            if explicitAdaptiveContentWidth >= 0 {
                return explicitAdaptiveContentWidth
            }
            if isDirectStretch {
                return 0
            }
            return resolvedNonStretchedWidth()
        }

        func currentSpanLength(pose: RubberPose, density: Float) -> Float {
            guard pose.isActive && pose.centerDistance > 1e-4 else {
                return 0
            }
            return pose.resolvedLength(insets: insets, density: density)
        }

        func effectiveContainerScale(pose: RubberPose, density: Float) -> Float {
            let spanLen = currentSpanLength(pose: pose, density: density)
            guard spanLen > 0 && pose.scale > 0 else {
                return 0
            }
            if isDirectStretch {
                return pose.scale
            }
            let nonStretchedW = resolvedNonStretchedWidth()
            if nonStretchedW > 0 && spanLen < nonStretchedW * pose.scale {
                return max(0, spanLen / nonStretchedW)
            }
            return pose.scale
        }
    }

    @ObservationIgnored
    public let world: SimulationWorld

    public private(set) var physics: RootPhysics
    public private(set) var scale: Float
    public private(set) var isAwake: Bool = false

    internal let density: Float = 1.0

    @ObservationIgnored
    private var hostToken: UUID?

    @ObservationIgnored
    private var energyAwake: Bool = false

    @ObservationIgnored
    private var pendingUnfold: Bool = false

    @ObservationIgnored
    private var viewportWidth: Float = 0

    @ObservationIgnored
    private var viewportHeight: Float = 0

    @ObservationIgnored
    private var planetSlotsByToken: [UUID: PlanetSlot] = [:]

    @ObservationIgnored
    private var planetRecordsByToken: [UUID: PlanetRecord] = [:]

    @ObservationIgnored
    private var activePlanetsById: [String: UUID] = [:]

    @ObservationIgnored
    private var rubberSlotsByToken: [UUID: RubberSlot] = [:]

    @ObservationIgnored
    private var rubberRecordsByToken: [UUID: RubberRecord] = [:]

    @ObservationIgnored
    private var activeRubbersByKey: [String: UUID] = [:]

    @ObservationIgnored
    private var zOrderByPlanetId: [String: Double] = [:]

    @ObservationIgnored
    private var nextZOrder: Double = 1

    @ObservationIgnored
    private var displayLinkDriver: DisplayLinkDriver?

    @ObservationIgnored
    private var fallbackLoopTask: Task<Void, Never>?

    @ObservationIgnored
    private var asyncStepTask: Task<Void, Never>?

    @ObservationIgnored
    private var isAsyncStepInFlight: Bool = false

    public init(physics: RootPhysics = .default) {
        self.physics = physics
        self.scale = Self.clamp(physics.initScale, min: physics.minScale, max: physics.maxScale)
        self.world = SimulationWorld(environment: physics.toEnvironmentSpec(density: 1.0))
    }

    deinit {
        fallbackLoopTask?.cancel()
        asyncStepTask?.cancel()
    }

    private static func clamp(_ value: Float, min minValue: Float, max maxValue: Float) -> Float {
        Swift.max(minValue, Swift.min(maxValue, value))
    }

    private static func rubberBucketKey(_ edgeKey: EdgeKey) -> String {
        edgeKey.encoded
    }

    public func updatePhysics(_ newPhysics: RootPhysics) {
        let clampedScale = Self.clamp(scale, min: newPhysics.minScale, max: newPhysics.maxScale)
        physics = newPhysics
        scale = clampedScale
        world.updateEnvironment(
            spec: newPhysics.toEnvironmentSpec(density: density),
            reheatSimulation: false
        )
        world.refreshRubberPoses(scale: clampedScale)
        completeReduction(energized: !world.bodies.isEmpty ? true : nil, syncSlots: true)
    }

    public func submitScale(_ newScale: Float) {
        precondition(newScale.isFinite, "scale must be finite, but was \(newScale)")
        let clamped = Self.clamp(newScale, min: physics.minScale, max: physics.maxScale)
        if clamped == scale {
            return
        }
        scale = clamped
        world.refreshRubberPoses(scale: clamped)
        completeReduction(energized: !world.bodies.isEmpty ? true : nil, syncSlots: true)
    }

    public func submitRubberElasticity(_ elasticity: Float) {
        precondition(elasticity.isFinite, "elasticity must be finite, but was \(elasticity)")
        let changed = world.rubbers.contains { abs($0.spec.elasticity - elasticity) > 1e-6 }
        world.updateAllRubbersElasticity(elasticity: elasticity)
        if changed {
            completeReduction(energized: true, syncSlots: true)
        }
    }

    @available(
        *,
        deprecated,
        message: "No-op since 0.6.1: simulated annealing has been retired. State and gesture mutations wake the loop automatically; invoke unfold() to untangle topology."
    )
    public func reheat() {
        // Intentional no-op since 0.6.1.
    }

    public func unfold() {
        if world.bodies.isEmpty || viewportWidth <= 0 || viewportHeight <= 0 {
            pendingUnfold = true
            return
        }
        pendingUnfold = false
        world.unfold()
        completeReduction(energized: true, syncSlots: true)
    }

    public func setPinned(id: String, pinned: Bool) {
        world.setBodyPinned(id: id, pinned: pinned)
        if let token = activePlanetsById[id], let record = planetRecordsByToken[token] {
            record.physics = record.physics.withPinned(pinned: pinned)
        }
        completeReduction(energized: !pinned && world.bodyOf(id: id) != nil ? true : nil, syncSlots: true)
    }

    public func setPressed(id: String, pressed: Bool) {
        world.setBodyPressed(id: id, pressed: pressed)
        completeReduction(energized: world.bodyOf(id: id) != nil ? true : nil, syncSlots: true)
    }

    public func dragBy(id: String, dx: Float, dy: Float) {
        precondition(dx.isFinite && dy.isFinite, "dragBy delta must be finite, but were (\(dx), \(dy))")
        if dx == 0 && dy == 0 {
            return
        }
        world.offsetBodyPosition(id: id, dx: dx, dy: dy, scale: scale)
        completeReduction(energized: true, syncSlots: true)
    }

    public func moveTo(id: String, x: Float, y: Float) {
        precondition(x.isFinite && y.isFinite, "moveTo coordinates must be finite, but were (\(x), \(y))")
        world.setBodyPosition(id: id, x: x, y: y, scale: scale)
        completeReduction(energized: true, syncSlots: true)
    }

    public func bringToFront(id: String) {
        nextZOrder += 1
        zOrderByPlanetId[id] = nextZOrder
        syncVisualSlots()
    }

    public func zOrder(for id: String) -> Double {
        zOrderByPlanetId[id] ?? 0
    }

    @discardableResult
    public func stepFrame(dtSeconds: Float) -> Bool {
        if pendingUnfold && !world.bodies.isEmpty && viewportWidth > 0 && viewportHeight > 0 {
            pendingUnfold = false
            world.unfold()
        }
        let keepRunning = world.stepSync(dtSeconds: dtSeconds, scale: scale)
        completeReduction(energized: keepRunning ? nil : false, syncSlots: true)
        return keepRunning
    }

    internal var isHostAttached: Bool {
        hostToken != nil
    }

    internal func planetSlot(token: UUID) -> PlanetSlot {
        if let existing = planetSlotsByToken[token] {
            return existing
        }
        let slot = PlanetSlot()
        planetSlotsByToken[token] = slot
        return slot
    }

    internal func rubberSlot(token: UUID) -> RubberSlot {
        if let existing = rubberSlotsByToken[token] {
            return existing
        }
        let slot = RubberSlot()
        rubberSlotsByToken[token] = slot
        return slot
    }

    internal func attachHost(_ token: UUID) {
        hostToken = token
        completeReduction(energized: nil, syncSlots: true)
    }

    internal func detachHost(_ token: UUID) {
        guard hostToken == token else { return }
        var releasedAny = false
        for body in world.bodies where body.isPressed {
            world.setBodyPressed(id: body.spec.id, pressed: false)
            releasedAny = true
        }
        if releasedAny && !world.bodies.isEmpty {
            energyAwake = true
        }
        hostToken = nil
        syncLoopState()
    }

    internal func updateViewportSize(width: Float, height: Float) {
        let safeWidth = Swift.max(0, width)
        let safeHeight = Swift.max(0, height)
        if abs(safeWidth - viewportWidth) < 0.01 && abs(safeHeight - viewportHeight) < 0.01 {
            return
        }
        viewportWidth = safeWidth
        viewportHeight = safeHeight
        world.updateViewport(widthPx: safeWidth, heightPx: safeHeight)
        if pendingUnfold && !world.bodies.isEmpty && safeWidth > 0 && safeHeight > 0 {
            pendingUnfold = false
            world.unfold()
        }
        completeReduction(energized: !world.bodies.isEmpty ? true : nil, syncSlots: true)
    }

    internal func syncPlanet(
        token: UUID,
        id: String,
        physics: PlanetPhysics,
        baseCenterX: Float,
        baseCenterY: Float,
        width: Float,
        height: Float
    ) {
        let existing = planetRecordsByToken[token]
        if let existing, existing.id != id {
            if activePlanetsById[existing.id] == token {
                activePlanetsById.removeValue(forKey: existing.id)
                _ = world.removePlanet(id: existing.id, scale: scale)
            }
        }

        if let conflict = activePlanetsById[id], conflict != token {
            preconditionFailure("Duplicate deponderPlanet id '\(id)' under the same deponderRoot.")
        }
        activePlanetsById[id] = token

        let slot = planetSlot(token: token)
        let record = existing ?? PlanetRecord(token: token, slot: slot, id: id, physics: physics)
        let isNewPlacement = existing == nil || existing?.id != id || !record.hasBaseOrigin
        record.id = id
        record.physics = physics
        record.baseCenterX = baseCenterX
        record.baseCenterY = baseCenterY
        record.hasBaseOrigin = true
        record.measuredWidth = width
        record.measuredHeight = height
        planetRecordsByToken[token] = record

        let existingBody = world.bodyOf(id: id)
        let seedX = isNewPlacement ? baseCenterX : (existingBody?.x ?? baseCenterX)
        let seedY = isNewPlacement ? baseCenterY : (existingBody?.y ?? baseCenterY)

        world.upsertPlanet(
            spec: physics.toPlanetSpec(id: id, density: density),
            initialX: seedX,
            initialY: seedY,
            scale: scale
        )
        world.setBodySize(id: id, widthPx: width, heightPx: height, scale: scale)
        if pendingUnfold && viewportWidth > 0 && viewportHeight > 0 {
            pendingUnfold = false
            world.unfold()
        }
        completeReduction(energized: true, syncSlots: true)
    }

    internal func unregisterPlanet(token: UUID) {
        if let slot = planetSlotsByToken.removeValue(forKey: token) {
            slot.update(offsetX: 0, offsetY: 0, scale: 1, zOrder: 0)
        }
        guard let record = planetRecordsByToken.removeValue(forKey: token) else { return }
        if activePlanetsById[record.id] == token {
            activePlanetsById.removeValue(forKey: record.id)
            _ = world.removePlanet(id: record.id, scale: scale)
        }
        completeReduction(energized: !world.bodies.isEmpty, syncSlots: true)
    }

    internal func planetRecord(for id: String) -> PlanetRecord? {
        guard let token = activePlanetsById[id] else { return nil }
        return planetRecordsByToken[token]
    }

    internal func syncRubber(
        token: UUID,
        startId: String,
        endId: String,
        physics: RubberPhysics,
        insets: DeponderRubberInsets,
        width: Float,
        height: Float,
        explicitNonStretchedWidth: Float = -1,
        explicitAdaptiveContentWidth: Float = -1,
        isDirectStretch: Bool = false
    ) {
        let newKey = EdgeKey(a: startId, b: endId)
        let newBucket = Self.rubberBucketKey(newKey)

        if let existing = rubberRecordsByToken[token] {
            let oldBucket = Self.rubberBucketKey(existing.edgeKey)
            if oldBucket != newBucket {
                if activeRubbersByKey[oldBucket] == token {
                    activeRubbersByKey.removeValue(forKey: oldBucket)
                    _ = world.removeRubber(key: existing.edgeKey, scale: scale)
                }
            }
        }

        if let conflict = activeRubbersByKey[newBucket], conflict != token {
            preconditionFailure(
                "Duplicate deponderRubber link between '\(startId)' and '\(endId)' (\(newBucket)) under the same deponderRoot."
            )
        }
        activeRubbersByKey[newBucket] = token

        let slot = rubberSlot(token: token)
        let record = rubberRecordsByToken[token]
            ?? RubberRecord(token: token, slot: slot, startId: startId, endId: endId, physics: physics, insets: insets)
        record.startId = startId
        record.endId = endId
        record.edgeKey = newKey
        record.physics = physics
        record.insets = insets
        record.measuredWidth = width
        record.measuredHeight = height
        record.explicitNonStretchedWidth = explicitNonStretchedWidth
        record.explicitAdaptiveContentWidth = explicitAdaptiveContentWidth
        record.isDirectStretch = isDirectStretch
        rubberRecordsByToken[token] = record

        let adaptiveContentWidth = record.resolvedAdaptiveContentWidth()
        _ = world.upsertRubber(
            spec: physics.toRubberSpec(
                startId: startId,
                endId: endId,
                density: density,
                contentWidthPx: adaptiveContentWidth
            ),
            scale: scale
        )
        completeReduction(energized: true, syncSlots: true)
    }

    internal func registerRubberStretchChild(
        rubberToken: UUID,
        childToken: UUID,
        width: Float
    ) {
        guard let record = rubberRecordsByToken[rubberToken] else { return }
        if record.stretchChildWidths[childToken] == width {
            return
        }
        record.stretchChildWidths[childToken] = width
        syncRubberAdaptiveLength(record: record)
        syncVisualSlots()
    }

    internal func unregisterRubberStretchChild(rubberToken: UUID, childToken: UUID) {
        guard let record = rubberRecordsByToken[rubberToken] else { return }
        if record.stretchChildWidths.removeValue(forKey: childToken) != nil {
            syncRubberAdaptiveLength(record: record)
            syncVisualSlots()
        }
    }

    private func syncRubberAdaptiveLength(record: RubberRecord) {
        guard record.physics.isAutoLength else { return }
        guard let pose = world.rubberOf(key: record.edgeKey) else { return }
        let targetLen = record.physics.resolveNaturalLengthPx(
            density: density,
            contentWidthPx: record.resolvedAdaptiveContentWidth()
        )
        if abs(pose.spec.naturalLengthPx - targetLen) > 0.05 {
            _ = world.upsertRubber(
                spec: record.physics.toRubberSpec(
                    startId: record.startId,
                    endId: record.endId,
                    density: density,
                    contentWidthPx: record.resolvedAdaptiveContentWidth()
                ),
                scale: scale
            )
            completeReduction(energized: true, syncSlots: false)
        }
    }

    internal func unregisterRubber(token: UUID) {
        if let slot = rubberSlotsByToken.removeValue(forKey: token) {
            slot.update(
                visible: false,
                centerX: 0,
                centerY: 0,
                spanLength: 0,
                rotationDeg: 0,
                effectiveScale: 0,
                poseScale: 1,
                measuredWidth: 0
            )
        }
        guard let record = rubberRecordsByToken.removeValue(forKey: token) else { return }
        let bucket = Self.rubberBucketKey(record.edgeKey)
        if activeRubbersByKey[bucket] == token {
            activeRubbersByKey.removeValue(forKey: bucket)
            _ = world.removeRubber(key: record.edgeKey, scale: scale)
        }
        completeReduction(energized: !world.bodies.isEmpty ? true : nil, syncSlots: true)
    }

    internal func rubberRecord(for key: EdgeKey) -> RubberRecord? {
        guard let token = activeRubbersByKey[Self.rubberBucketKey(key)] else { return nil }
        return rubberRecordsByToken[token]
    }

    internal func rubberPose(for key: EdgeKey) -> RubberPose? {
        world.rubberOf(key: key)
    }

    internal func body(for id: String) -> PlanetBody? {
        world.bodyOf(id: id)
    }

    internal func planetVisualOffset(id: String) -> CGSize {
        guard let record = planetRecord(for: id) else {
            return .zero
        }
        return CGSize(width: record.slot.offsetX, height: record.slot.offsetY)
    }

    private func syncVisualSlots() {
        let currentScale = CGFloat(scale)
        for record in planetRecordsByToken.values {
            let zOrder = zOrderByPlanetId[record.id] ?? 0
            if record.hasBaseOrigin, let body = world.bodyOf(id: record.id) {
                record.slot.update(
                    offsetX: CGFloat(body.x) - CGFloat(record.baseCenterX),
                    offsetY: CGFloat(body.y) - CGFloat(record.baseCenterY),
                    scale: currentScale,
                    zOrder: zOrder
                )
            } else {
                record.slot.update(
                    offsetX: 0,
                    offsetY: 0,
                    scale: currentScale,
                    zOrder: zOrder
                )
            }
        }

        for record in rubberRecordsByToken.values {
            if let pose = world.rubberOf(key: record.edgeKey),
               pose.isActive,
               pose.centerDistance > 1e-4 {
                let spanLen = record.currentSpanLength(pose: pose, density: density)
                let effScale = record.effectiveContainerScale(pose: pose, density: density)
                let isVisible = spanLen > 0 && effScale > 0
                let targetX = CGFloat(pose.resolvedCenterX(insets: record.insets, density: density))
                let targetY = CGFloat(pose.resolvedCenterY(insets: record.insets, density: density))
                record.slot.update(
                    visible: isVisible,
                    centerX: targetX,
                    centerY: targetY,
                    spanLength: spanLen,
                    rotationDeg: Double(pose.rotationDeg),
                    effectiveScale: effScale,
                    poseScale: pose.scale,
                    measuredWidth: record.measuredWidth
                )
            } else {
                record.slot.update(
                    visible: false,
                    centerX: 0,
                    centerY: 0,
                    spanLength: 0,
                    rotationDeg: 0,
                    effectiveScale: 0,
                    poseScale: 1,
                    measuredWidth: record.measuredWidth
                )
            }
        }
    }

    private func completeReduction(energized: Bool?, syncSlots: Bool) {
        if let energized {
            energyAwake = energized
        }
        syncLoopState()
        if syncSlots {
            syncVisualSlots()
        }
    }

    private func syncLoopState() {
        let hasTopology = !world.bodies.isEmpty
        let nextAwake = hostToken != nil && hasTopology && energyAwake
        if isAwake != nextAwake {
            isAwake = nextAwake
        }
        if nextAwake {
            startFrameLoopIfNeeded()
        } else {
            stopFrameLoop()
        }
    }

    private func startFrameLoopIfNeeded() {
        if displayLinkDriver != nil || fallbackLoopTask != nil {
            return
        }

        if let driver = DisplayLinkDriver.make(onTick: { [weak self] dtSeconds in
            guard let self, self.isAwake else { return false }
            self.driveFrameTick(dtSeconds: dtSeconds)
            return self.isAwake
        }) {
            displayLinkDriver = driver
            driver.start()
            return
        }

        fallbackLoopTask = Task { @MainActor [weak self] in
            var lastTime = ContinuousClock.now
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(16))
                guard let self, self.isAwake else { break }
                let now = ContinuousClock.now
                let elapsed = lastTime.duration(to: now)
                lastTime = now
                let dt = Float(elapsed.components.seconds) + Float(elapsed.components.attoseconds) / 1e18
                let safeDt = Swift.max(0.001, Swift.min(0.05, dt))
                self.driveFrameTick(dtSeconds: safeDt)
                if !self.isAwake {
                    break
                }
            }
        }
    }

    private func driveFrameTick(dtSeconds: Float) {
        if world.bodies.count < 256 {
            _ = stepFrame(dtSeconds: dtSeconds)
            return
        }
        guard !isAsyncStepInFlight else { return }
        isAsyncStepInFlight = true
        let worldBox = SendableSimulationWorldBox(world: world)
        let currentScale = scale
        asyncStepTask = Task { @MainActor [weak self] in
            guard let self else { return }
            defer {
                self.isAsyncStepInFlight = false
                self.asyncStepTask = nil
            }
            if self.pendingUnfold && !self.world.bodies.isEmpty && self.viewportWidth > 0 && self.viewportHeight > 0 {
                self.pendingUnfold = false
                self.world.unfold()
            }
            do {
                let keepRunning = try await worldBox.world.step(dtSeconds: dtSeconds, scale: currentScale)
                guard !Task.isCancelled else { return }
                self.completeReduction(energized: keepRunning ? nil : false, syncSlots: true)
            } catch {
                if !Task.isCancelled {
                    self.completeReduction(energized: false, syncSlots: true)
                }
            }
        }
    }

    private func stopFrameLoop() {
        displayLinkDriver?.stop()
        displayLinkDriver = nil
        fallbackLoopTask?.cancel()
        fallbackLoopTask = nil
        asyncStepTask?.cancel()
        asyncStepTask = nil
        isAsyncStepInFlight = false
    }
}

private struct SendableSimulationWorldBox: @unchecked Sendable {
    let world: SimulationWorld
}

@MainActor
private final class DisplayLinkDriver: NSObject {
    private let onTick: (Float) -> Bool
    private var displayLink: CADisplayLink?
    private var lastTimestamp: CFTimeInterval = 0

    private init(displayLink: CADisplayLink?, onTick: @escaping (Float) -> Bool) {
        self.onTick = onTick
        super.init()
        self.displayLink = displayLink
    }

    static func make(onTick: @escaping (Float) -> Bool) -> DisplayLinkDriver? {
        let driver = DisplayLinkDriver(displayLink: nil, onTick: onTick)
        #if os(iOS)
        let link = CADisplayLink(target: driver, selector: #selector(handleFrame(_:)))
        driver.displayLink = link
        return driver
        #elseif os(macOS)
        guard let screen = NSScreen.main else {
            return nil
        }
        let link = screen.displayLink(target: driver, selector: #selector(handleFrame(_:)))
        driver.displayLink = link
        return driver
        #else
        return nil
        #endif
    }

    func start() {
        lastTimestamp = 0
        displayLink?.add(to: .main, forMode: .common)
    }

    func stop() {
        displayLink?.invalidate()
        displayLink = nil
    }

    @objc private func handleFrame(_ link: CADisplayLink) {
        let timestamp = link.timestamp
        let dt: Float
        if lastTimestamp > 0 {
            let delta = timestamp - lastTimestamp
            dt = Float(max(0.001, min(0.05, delta)))
        } else {
            let duration = link.targetTimestamp - timestamp
            dt = duration > 0 ? Float(max(0.001, min(0.05, duration))) : (1.0 / 60.0)
        }
        lastTimestamp = timestamp
        if !onTick(dt) {
            stop()
        }
    }
}
