import Observation
import os
import SwiftUI
import XCTest
@testable import Deponder
import DeponderCore
#if canImport(AppKit)
import AppKit
#endif

final class DeponderAppleTests: XCTestCase {

    func testCoreSwiftExport_presetsEdgeKeysAndStepSyncWorkCleanly() {
        let k1 = EdgeKey(a: "1", b: "23")
        let k2 = EdgeKey(a: "12", b: "3")
        XCTAssertNotEqual(k1.encoded, k2.encoded)

        let forward = EdgeKey(a: "a", b: "A")
        let reverse = EdgeKey(a: "A", b: "a")
        XCTAssertEqual(forward.encoded, reverse.encoded)

        let world = SimulationWorld(physics: .crisp, density: 1.0)
        world.updateViewport(widthPx: 900, heightPx: 700)

        let planets: [PlanetSpec] = [
            PlanetPhysics(pinned: true).toPlanetSpec(id: "root", density: 1.0),
            PlanetPhysics().toPlanetSpec(id: "a", density: 1.0),
            PlanetPhysics().toPlanetSpec(id: "b", density: 1.0),
            PlanetPhysics().toPlanetSpec(id: "c", density: 1.0),
        ]
        let edges: [RubberSpec] = [
            RubberPhysics.fixed(naturalLengthDp: 100).toRubberSpec(startId: "root", endId: "a", density: 1.0),
            RubberPhysics.fixed(naturalLengthDp: 100).toRubberSpec(startId: "root", endId: "b", density: 1.0),
            RubberPhysics.fixed(naturalLengthDp: 100).toRubberSpec(startId: "a", endId: "c", density: 1.0),
        ]

        world.syncTopology(
            planets: planets,
            edges: edges,
            defaultInitialX: 450,
            defaultInitialY: 350
        )

        var active = true
        var guardCount = 0
        while active && guardCount < 2400 {
            active = world.stepSync(dtSeconds: 0.016, scale: 1.0)
            guardCount += 1
        }
        XCTAssertFalse(active, "Crisp stepSync simulation must settle within 2400 frames")
        XCTAssertEqual(world.bodies.count, 4)
        XCTAssertEqual(world.rubbers.count, 3)

        // Verify scale transform contract on PlanetBody & RubberPose
        world.setBodySize(id: "root", widthPx: 40, heightPx: 40, scale: 1.0)
        world.setBodySize(id: "a", widthPx: 40, heightPx: 40, scale: 1.0)
        let poseAt1 = world.rubberOf(key: EdgeKey(a: "root", b: "a"))!
        XCTAssertTrue(poseAt1.isActive)
        XCTAssertEqual(poseAt1.scale, 1.0, accuracy: 1e-4)
        let insetAt1 = poseAt1.resolvedStartInsetPx(insets: .contour(gapDp: 4), density: 1.0)
        XCTAssertGreaterThan(insetAt1, 24)

        world.refreshRubberPoses(scale: 1.5)
        let poseAt15 = world.rubberOf(key: EdgeKey(a: "root", b: "a"))!
        XCTAssertEqual(poseAt15.scale, 1.5, accuracy: 1e-4)
        XCTAssertEqual(poseAt15.centerDistance, poseAt1.centerDistance, accuracy: 1e-4)
        let insetAt15 = poseAt15.resolvedStartInsetPx(insets: .contour(gapDp: 4), density: 1.0)
        XCTAssertEqual(insetAt15, insetAt1 * 1.5, accuracy: 1e-2)
    }

    func testRubberGeometryAndStretchMathParity() {
        XCTAssertEqual(DeponderRubberStyle.default.labelBackgroundColor, Color.clear)

        let insetNormal = DeponderRubberGeometryMath.computeArrowStemInset(
            effArrowWidth: 10,
            effArrowHeight: 13.5,
            lineThickness: 2,
            isOpaqueLine: true
        )
        XCTAssertGreaterThan(insetNormal, 7)
        XCTAssertLessThan(insetNormal, 10)

        let insetThick = DeponderRubberGeometryMath.computeArrowStemInset(
            effArrowWidth: 10,
            effArrowHeight: 13.5,
            lineThickness: 12,
            isOpaqueLine: true
        )
        XCTAssertEqual(insetThick, 10, accuracy: 1e-4)

        let insetTranslucent = DeponderRubberGeometryMath.computeArrowStemInset(
            effArrowWidth: 10,
            effArrowHeight: 13.5,
            lineThickness: 2,
            isOpaqueLine: false
        )
        XCTAssertEqual(insetTranslucent, 10, accuracy: 1e-4)

        XCTAssertEqual(
            DeponderRubberGeometryMath.computeArrowScale(
                localSpan: 10,
                arrowLength: 10,
                hasStart: true,
                hasEnd: true
            ),
            0.5,
            accuracy: 1e-4
        )

        XCTAssertFalse(DeponderRubberGeometryMath.isStartArrowEnabled(arrowMode: .both, insets: .none))
        XCTAssertFalse(DeponderRubberGeometryMath.isEndArrowEnabled(arrowMode: .both, insets: .none))
        XCTAssertFalse(DeponderRubberGeometryMath.isStartArrowEnabled(arrowMode: .both, insets: .endOnly()))
        XCTAssertTrue(DeponderRubberGeometryMath.isEndArrowEnabled(arrowMode: .both, insets: .endOnly()))
        XCTAssertFalse(DeponderRubberGeometryMath.isStartArrowEnabled(arrowMode: .both, insets: .fixed(startDp: 0, endDp: 24)))
        XCTAssertTrue(DeponderRubberGeometryMath.isEndArrowEnabled(arrowMode: .both, insets: .fixed(startDp: 0, endDp: 24)))
        XCTAssertTrue(DeponderRubberGeometryMath.isEndArrowEnabled(arrowMode: .end, insets: .default))
        XCTAssertEqual(DeponderRubberInsets.Mode.contour, .CONTOUR)

        XCTAssertEqual(DeponderStretchMath.computeStretchScaleX(lengthPx: 180, widthPx: 0, parentScale: 1), 0)
        XCTAssertEqual(DeponderStretchMath.computeStretchScaleX(lengthPx: 180, widthPx: 10, parentScale: 0), 0)
        XCTAssertEqual(DeponderStretchMath.computeStretchScaleX(lengthPx: 180, widthPx: 10, parentScale: 1.2), 15, accuracy: 1e-4)
        XCTAssertEqual(
            DeponderStretchMath.computeChildStretchCenterAlignTx(
                rubberWidthPx: 100,
                childLeftInRubberPx: 0,
                childWidthPx: 20
            ),
            40,
            accuracy: 1e-4
        )
    }

    @MainActor
    func testDeponderState_lifecycleWakeGateScalingDragMoveAndUnfold() {
        let state = DeponderState(
            physics: RootPhysics.make(
                initScale: 1.0,
                maxScale: 1.8,
                minScale: 0.6,
                airDamping: 8.0,
                wallRepulsionDp: 0
            )
        )
        let hostToken = UUID()
        state.attachHost(hostToken)
        state.updateViewportSize(width: 800, height: 600)
        XCTAssertFalse(state.isAwake)

        // Defer unfold prior to planet mount
        state.unfold()

        let tokenA = UUID()
        let tokenB = UUID()
        state.syncPlanet(
            token: tokenA,
            id: "a",
            physics: PlanetPhysics.make(repulsionRadiusDp: 0, pinned: true),
            baseCenterX: 100,
            baseCenterY: 300,
            width: 40,
            height: 40
        )
        state.syncPlanet(
            token: tokenB,
            id: "b",
            physics: PlanetPhysics.make(repulsionRadiusDp: 0, pinned: false),
            baseCenterX: 260,
            baseCenterY: 300,
            width: 40,
            height: 40
        )

        let rubberToken = UUID()
        state.syncRubber(
            token: rubberToken,
            startId: "a",
            endId: "b",
            physics: RubberPhysics.fixed(naturalLengthDp: 100, elasticity: 12),
            insets: .default,
            width: 40,
            height: 20,
            explicitNonStretchedWidth: 40,
            explicitAdaptiveContentWidth: 0
        )
        XCTAssertTrue(state.isAwake)

        // Detach preserves energy; reattach resumes
        state.detachHost(hostToken)
        XCTAssertFalse(state.isAwake)
        state.attachHost(hostToken)
        XCTAssertTrue(state.isAwake)

        // Settle cleanly to 0% CPU sleep
        var active = true
        var guardCount = 0
        while active && guardCount < 800 {
            active = state.stepFrame(dtSeconds: 0.016)
            guardCount += 1
        }
        XCTAssertFalse(active, "Critically damped simulation should settle within 800 frames")
        XCTAssertFalse(state.isAwake)

        // Scale clamping
        state.submitScale(5.0)
        XCTAssertEqual(state.scale, 1.8, accuracy: 1e-4)
        state.submitScale(0.1)
        XCTAssertEqual(state.scale, 0.6, accuracy: 1e-4)

        // Move and drag
        state.moveTo(id: "a", x: 250, y: 320)
        XCTAssertEqual(state.world.bodyOf(id: "a")!.x, 250, accuracy: 1e-4)
        state.dragBy(id: "a", dx: 30, dy: -20)
        XCTAssertEqual(state.world.bodyOf(id: "a")!.x, 280, accuracy: 1e-4)
        XCTAssertEqual(state.world.bodyOf(id: "a")!.y, 300, accuracy: 1e-4)

        // Z-order monotonicity
        let z0 = state.zOrder(for: "a")
        state.bringToFront(id: "a")
        XCTAssertGreaterThan(state.zOrder(for: "a"), z0)

        // Settled elasticity no-op keeps loop asleep; changed elasticity wakes it
        var settleGuard = 0
        while state.stepFrame(dtSeconds: 0.016) && settleGuard < 800 {
            settleGuard += 1
        }
        XCTAssertFalse(state.isAwake)
        state.submitRubberElasticity(12)
        XCTAssertFalse(state.isAwake)
        state.submitRubberElasticity(16)
        XCTAssertTrue(state.isAwake)

        // planetVisualOffset tracks exact root body.x/y relative to baseCenter without dx/dy overshoot
        state.moveTo(id: "a", x: 145, y: 265)
        let offsetA = state.planetVisualOffset(id: "a")
        XCTAssertEqual(offsetA.width, 45, accuracy: 1e-4)
        XCTAssertEqual(offsetA.height, -35, accuracy: 1e-4)
    }

    @MainActor
    func testCompoundRubberBadgeShrinkScale() {
        let state = DeponderState(physics: .crisp)
        state.attachHost(UUID())
        state.updateViewportSize(width: 400, height: 300)

        state.syncPlanet(
            token: UUID(),
            id: "a",
            physics: PlanetPhysics(pinned: true),
            baseCenterX: 100,
            baseCenterY: 150,
            width: 20,
            height: 20
        )
        state.syncPlanet(
            token: UUID(),
            id: "b",
            physics: PlanetPhysics(pinned: true),
            baseCenterX: 160,
            baseCenterY: 150,
            width: 20,
            height: 20
        )

        let rubberToken = UUID()
        state.syncRubber(
            token: rubberToken,
            startId: "a",
            endId: "b",
            physics: RubberPhysics(),
            insets: .contour(gapDp: 0),
            width: 80,
            height: 18
        )
        state.registerRubberStretchChild(
            rubberToken: rubberToken,
            childToken: UUID(),
            width: 20
        )

        let record = state.rubberRecord(for: EdgeKey(a: "a", b: "b"))!
        let pose = state.rubberPose(for: EdgeKey(a: "a", b: "b"))!
        // Center distance 60pt - 10pt - 10pt = 40pt visible span; 80pt badge shrinks to 40/80 = 0.5x
        XCTAssertEqual(record.currentSpanLength(pose: pose, density: 1.0), 40, accuracy: 1e-3)
        XCTAssertEqual(record.effectiveContainerScale(pose: pose, density: 1.0), 0.5, accuracy: 1e-3)
    }

    @MainActor
    func testFineGrainedObservation_isolatedPinnedPlanetIsNotInvalidatedWhenOtherPlanetMoves() {
        let state = DeponderState(
            physics: RootPhysics.make(
                airDamping: 4.0,
                wallRepulsionDp: 0
            )
        )
        state.attachHost(UUID())
        state.updateViewportSize(width: 800, height: 600)

        let tokenA = UUID()
        let tokenB = UUID()
        let tokenIsolated = UUID()
        let rubberToken = UUID()

        state.syncPlanet(
            token: tokenA,
            id: "a",
            physics: PlanetPhysics.make(repulsionRadiusDp: 0, pinned: true),
            baseCenterX: 100,
            baseCenterY: 300,
            width: 30,
            height: 30
        )
        state.syncPlanet(
            token: tokenB,
            id: "b",
            physics: PlanetPhysics.make(repulsionRadiusDp: 0, pinned: false),
            baseCenterX: 320,
            baseCenterY: 300,
            width: 30,
            height: 30
        )
        state.syncPlanet(
            token: tokenIsolated,
            id: "isolated",
            physics: PlanetPhysics.make(repulsionRadiusDp: 0, pinned: true),
            baseCenterX: 650,
            baseCenterY: 300,
            width: 30,
            height: 30
        )
        state.syncRubber(
            token: rubberToken,
            startId: "a",
            endId: "b",
            physics: RubberPhysics.fixed(naturalLengthDp: 100, elasticity: 10),
            insets: .default,
            width: 40,
            height: 16
        )

        let isolatedSlot = state.planetSlot(token: tokenIsolated)
        let movingSlot = state.planetSlot(token: tokenB)
        let abRubberSlot = state.rubberSlot(token: rubberToken)

        let isolatedInvalidations = OSAllocatedUnfairLock(initialState: 0)
        let movingInvalidations = OSAllocatedUnfairLock(initialState: 0)
        let rubberInvalidations = OSAllocatedUnfairLock(initialState: 0)

        withObservationTracking {
            _ = isolatedSlot.offsetX
            _ = isolatedSlot.offsetY
            _ = isolatedSlot.scale
            _ = isolatedSlot.zOrder
        } onChange: {
            isolatedInvalidations.withLock { $0 += 1 }
        }

        withObservationTracking {
            _ = movingSlot.offsetX
            _ = movingSlot.offsetY
        } onChange: {
            movingInvalidations.withLock { $0 += 1 }
        }

        withObservationTracking {
            _ = abRubberSlot.spanLength
            _ = abRubberSlot.centerX
        } onChange: {
            rubberInvalidations.withLock { $0 += 1 }
        }

        _ = state.stepFrame(dtSeconds: 0.016)

        XCTAssertEqual(isolatedInvalidations.withLock { $0 }, 0, "Stationary pinned node must not be invalidated when another node moves")
        XCTAssertEqual(movingInvalidations.withLock { $0 }, 1, "Moved planet slot must fire exactly one invalidation")
        XCTAssertEqual(rubberInvalidations.withLock { $0 }, 1, "Connected rubber slot must fire exactly one invalidation")
    }

    #if canImport(AppKit)
    @MainActor
    func testSwiftUIHostingLayout_mountsAndIntegratesFiveOperators() {
        let state = DeponderState(physics: .crisp)
        let rootView = ZStack {
            DeponderDefaultRubber(
                startId: "p1",
                endId: "p2",
                label: "links",
                arrowMode: .both,
                physics: RubberPhysics.fixed(naturalLengthDp: 120),
                insets: .contour(gapDp: 0)
            )
            Rectangle()
                .frame(width: 24, height: 4)
                .deponderStretch()
                .deponderRubber(startId: "p1", endId: "p3", insets: .contour(gapDp: 0))
            ZStack {
                Rectangle()
                    .frame(width: 20, height: 2)
                    .deponderStretch()
                Text("badge")
            }
            .deponderRubber(startId: "p2", endId: "p3", insets: .contour(gapDp: 0))

            Text("P1")
                .frame(width: 40, height: 40)
                .deponderPlanet(id: "p1", physics: PlanetPhysics(pinned: true))
                .deponderDrag()
            Text("P2")
                .frame(width: 40, height: 40)
                .deponderDrag()
                .deponderPlanet(id: "p2", physics: PlanetPhysics(pinned: false))
            Text("P3")
                .frame(width: 40, height: 40)
                .deponderPlanet(id: "p3", physics: PlanetPhysics(pinned: false))
                .deponderDrag()
        }
        .frame(width: 400, height: 300)
        .deponderRoot(state)

        let hostingView = NSHostingView(rootView: rootView)
        hostingView.frame = NSRect(x: 0, y: 0, width: 400, height: 300)
        hostingView.layoutSubtreeIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.05))

        XCTAssertNotNil(state.world.bodyOf(id: "p1"))
        XCTAssertNotNil(state.world.bodyOf(id: "p2"))
        XCTAssertNotNil(state.world.bodyOf(id: "p3"))
        XCTAssertNotNil(state.world.rubberOf(key: EdgeKey(a: "p1", b: "p2")))
        XCTAssertNotNil(state.world.rubberOf(key: EdgeKey(a: "p1", b: "p3")))
        XCTAssertNotNil(state.world.rubberOf(key: EdgeKey(a: "p2", b: "p3")))
    }
    #endif
}
