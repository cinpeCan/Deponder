import SwiftUI

public struct DeponderRubberStyle: Equatable, Hashable, Sendable {
    public let lineColor: Color
    public let lineThickness: CGFloat
    public let arrowSize: CGFloat
    public let labelColor: Color
    public let labelTextSize: CGFloat
    public let labelBackgroundColor: Color

    public static let defaultLineColor = Color(red: 0.45, green: 0.49, blue: 0.58)
    public static let defaultLabelColor = Color.primary
    public static let defaultLabelBackgroundColor = Color.clear

    public static let `default` = DeponderRubberStyle()

    public init(
        lineColor: Color = DeponderRubberStyle.defaultLineColor,
        lineThickness: CGFloat = CGFloat(DeponderDefaults.Rubber.shared.DEFAULT_THICKNESS_DP),
        arrowSize: CGFloat = CGFloat(DeponderDefaults.Rubber.shared.DEFAULT_ARROW_SIZE_DP),
        labelColor: Color = DeponderRubberStyle.defaultLabelColor,
        labelTextSize: CGFloat = CGFloat(DeponderDefaults.Rubber.shared.DEFAULT_LABEL_TEXT_SIZE_SP),
        labelBackgroundColor: Color = DeponderRubberStyle.defaultLabelBackgroundColor
    ) {
        precondition(lineThickness.isFinite && lineThickness > 0, "lineThickness must be > 0, got \(lineThickness)")
        precondition(arrowSize.isFinite && arrowSize >= 0, "arrowSize must be >= 0, got \(arrowSize)")
        precondition(labelTextSize.isFinite && labelTextSize > 0, "labelTextSize must be > 0, got \(labelTextSize)")

        self.lineColor = lineColor
        self.lineThickness = lineThickness
        self.arrowSize = arrowSize
        self.labelColor = labelColor
        self.labelTextSize = labelTextSize
        self.labelBackgroundColor = labelBackgroundColor
    }
}

public enum DeponderRubberGeometryMath {
    public static let arrowHeightRatio: Float = 1.35
    public static let arrowStemInsetRatio: Float = 0.75

    public static func isStartArrowEnabled(
        arrowMode: DeponderArrowMode,
        insets: DeponderRubberInsets
    ) -> Bool {
        (arrowMode == .START || arrowMode == .BOTH) && insets.hasStartSafeArea
    }

    public static func isEndArrowEnabled(
        arrowMode: DeponderArrowMode,
        insets: DeponderRubberInsets
    ) -> Bool {
        (arrowMode == .END || arrowMode == .BOTH) && insets.hasEndSafeArea
    }

    public static func computeArrowScale(
        localSpan: Float,
        arrowLength: Float,
        hasStart: Bool,
        hasEnd: Bool
    ) -> Float {
        guard arrowLength > 0, localSpan > 0 else { return 0 }
        let minSpan = (hasStart ? arrowLength : 0) + (hasEnd ? arrowLength : 0)
        if minSpan > 0 && localSpan < minSpan {
            return max(0, min(1, localSpan / minSpan))
        }
        return 1
    }

    public static func computeArrowStemInset(
        effArrowWidth: Float,
        effArrowHeight: Float,
        lineThickness: Float,
        isOpaqueLine: Bool = true
    ) -> Float {
        guard effArrowWidth > 0 else { return 0 }
        if !isOpaqueLine { return effArrowWidth }
        let heightAtInset = effArrowHeight * arrowStemInsetRatio
        if lineThickness <= heightAtInset * 0.9 {
            return effArrowWidth * arrowStemInsetRatio
        }
        return effArrowWidth
    }
}

public struct DeponderDefaultRubber: View {
    @Environment(\.deponderState) private var state
    @Environment(\.self) private var environment

    public let startId: String
    public let endId: String
    public let label: String
    public let arrowMode: DeponderArrowMode
    public let physics: RubberPhysics
    public let insets: DeponderRubberInsets
    public let style: DeponderRubberStyle

    @State private var rubberToken = UUID()
    @State private var measuredPillSize: CGSize = .zero

    public init(
        startId: String,
        endId: String,
        label: String = "",
        arrowMode: DeponderArrowMode = .NONE,
        physics: RubberPhysics = .default,
        insets: DeponderRubberInsets = .default,
        style: DeponderRubberStyle = .default
    ) {
        self.startId = startId
        self.endId = endId
        self.label = label
        self.arrowMode = arrowMode
        self.physics = physics
        self.insets = insets
        self.style = style
    }

    public var body: some View {
        let slot = state?.rubberSlot(token: rubberToken)
        let spanLen = slot?.spanLength ?? 0
        let active = slot?.visible == true && spanLen > 0

        let hasStartArrow = DeponderRubberGeometryMath.isStartArrowEnabled(arrowMode: arrowMode, insets: insets)
        let hasEndArrow = DeponderRubberGeometryMath.isEndArrowEnabled(arrowMode: arrowMode, insets: insets)
        let arrowLength = Float(style.arrowSize)
        let arrowHeight = arrowLength * DeponderRubberGeometryMath.arrowHeightRatio

        let effScale = slot?.effectiveScale ?? 1
        let localSpan = effScale > 1e-4 ? spanLen / effScale : 0

        let arrScale = DeponderRubberGeometryMath.computeArrowScale(
            localSpan: localSpan,
            arrowLength: arrowLength,
            hasStart: hasStartArrow,
            hasEnd: hasEndArrow
        )
        let effArrowWidth = arrowLength * arrScale
        let effArrowHeight = arrowHeight * arrScale
        let lineAlpha = style.lineColor.resolve(in: environment).opacity
        let isOpaqueLine = lineAlpha >= 0.999

        let sInset = hasStartArrow
            ? DeponderRubberGeometryMath.computeArrowStemInset(
                effArrowWidth: effArrowWidth,
                effArrowHeight: effArrowHeight,
                lineThickness: Float(style.lineThickness),
                isOpaqueLine: isOpaqueLine
            )
            : 0
        let eInset = hasEndArrow
            ? DeponderRubberGeometryMath.computeArrowStemInset(
                effArrowWidth: effArrowWidth,
                effArrowHeight: effArrowHeight,
                lineThickness: Float(style.lineThickness),
                isOpaqueLine: isOpaqueLine
            )
            : 0
        let stemLen = max(0, localSpan - sInset - eInset)

        let labelOffset = CGFloat(DeponderDefaults.Rubber.shared.DEFAULT_LABEL_OFFSET_DP)
        let arrowFootprint: CGFloat = (hasStartArrow ? style.arrowSize : 0) + (hasEndArrow ? style.arrowSize : 0)
        let containerWidth = max(max(CGFloat(4), arrowFootprint), measuredPillSize.width)
        let pillOffsetY = -(style.lineThickness * 0.5 + labelOffset + measuredPillSize.height * 0.5)
        let containerHeight = max(
            max(style.lineThickness, CGFloat(arrowHeight)),
            measuredPillSize.height > 0
                ? style.lineThickness + (labelOffset + measuredPillSize.height) * 2
                : style.lineThickness
        )

        return ZStack {
            if active {
                // Stem rectangle
                Rectangle()
                    .fill(style.lineColor)
                    .frame(width: max(0, CGFloat(stemLen)), height: style.lineThickness)
                    .offset(x: CGFloat((sInset - eInset) * 0.5))

                if hasStartArrow && effArrowWidth > 0 && effArrowHeight > 0 {
                    DeponderArrowTriangle(pointsRight: false)
                        .fill(style.lineColor)
                        .frame(width: CGFloat(effArrowWidth), height: CGFloat(effArrowHeight))
                        .offset(x: CGFloat(-localSpan * 0.5 + effArrowWidth * 0.5))
                }

                if hasEndArrow && effArrowWidth > 0 && effArrowHeight > 0 {
                    DeponderArrowTriangle(pointsRight: true)
                        .fill(style.lineColor)
                        .frame(width: CGFloat(effArrowWidth), height: CGFloat(effArrowHeight))
                        .offset(x: CGFloat(localSpan * 0.5 - effArrowWidth * 0.5))
                }
            }

            if !label.isEmpty {
                let hasPillBackground = style.labelBackgroundColor.resolve(in: environment).opacity > 0.001
                Text(label)
                    .font(.system(size: style.labelTextSize, weight: .medium))
                    .foregroundStyle(style.labelColor)
                    .lineLimit(1)
                    .padding(.horizontal, hasPillBackground ? 6 : 0)
                    .padding(.vertical, hasPillBackground ? 2 : 0)
                    .background {
                        if hasPillBackground {
                            Capsule().fill(style.labelBackgroundColor)
                        }
                    }
                    .onGeometryChange(for: CGSize.self) { proxy in
                        proxy.size
                    } action: { newSize in
                        measuredPillSize = newSize
                    }
                    .offset(y: pillOffsetY)
            }
        }
        .frame(width: containerWidth, height: containerHeight)
        .onChange(of: label) { _, newValue in
            if newValue.isEmpty {
                measuredPillSize = .zero
            }
        }
        .deponderRubberInternal(
            token: rubberToken,
            startId: startId,
            endId: endId,
            physics: physics,
            insets: insets,
            explicitNonStretchedWidth: Float(containerWidth),
            explicitAdaptiveContentWidth: label.isEmpty ? 0 : Float(measuredPillSize.width)
        )
    }
}

private struct DeponderArrowTriangle: Shape {
    let pointsRight: Bool

    func path(in rect: CGRect) -> Path {
        var path = Path()
        if pointsRight {
            path.move(to: CGPoint(x: rect.maxX, y: rect.midY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        } else {
            path.move(to: CGPoint(x: rect.minX, y: rect.midY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        }
        path.closeSubpath()
        return path
    }
}

#if DEBUG
#Preview("Deponder Force Graph") {
    struct PreviewGraph: View {
        @State private var state = DeponderState(physics: .balanced)

        var body: some View {
            ZStack {
                DeponderDefaultRubber(
                    startId: "core",
                    endId: "ui",
                    label: "drives",
                    arrowMode: .end
                )
                DeponderDefaultRubber(
                    startId: "core",
                    endId: "physics",
                    label: "120Hz",
                    arrowMode: .both
                )

                Text("Core")
                    .font(.headline)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.indigo.opacity(0.15), in: Capsule())
                    .deponderPlanet(id: "core", physics: PlanetPhysics(pinned: true))
                    .deponderDrag()

                Text("SwiftUI")
                    .font(.subheadline)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.teal.opacity(0.15), in: Capsule())
                    .deponderPlanet(id: "ui")
                    .deponderDrag()

                Text("Physics")
                    .font(.subheadline)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.orange.opacity(0.15), in: Capsule())
                    .deponderPlanet(id: "physics")
                    .deponderDrag()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .deponderRoot(state)
            .onAppear {
                state.unfold()
            }
        }
    }

    return PreviewGraph()
        .frame(width: 420, height: 320)
}
#endif
