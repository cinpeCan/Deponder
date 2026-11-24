// swift-tools-version: 6.0

import Foundation
import PackageDescription

private let useLocalBinary = ProcessInfo.processInfo.environment["DEPONDER_LOCAL_BINARY"] == "1"

private let bridgeBinaryTarget: Target = useLocalBinary
    ? .binaryTarget(
        name: "DeponderCoreBridge",
        path: "deponder-core/build/XCFrameworks/release/DeponderCoreBridge.xcframework"
    )
    : .binaryTarget(
        name: "DeponderCoreBridge",
        url: "https://github.com/cinpeCan/Deponder/releases/download/v0.7.0/DeponderCoreBridge.xcframework.zip",
        checksum: "9345d903744187a9bd5fb94722ea863d0a46feda30b5ac336f46d9cdc5528b57"
    )

let package = Package(
    name: "Deponder",
    platforms: [
        .iOS(.v18),
        .macOS(.v15),
    ],
    products: [
        .library(
            name: "DeponderCore",
            targets: ["DeponderCore"]
        ),
        .library(
            name: "Deponder",
            targets: ["Deponder"]
        ),
    ],
    targets: [
        bridgeBinaryTarget,
        .target(
            name: "KotlinRuntime",
            path: "apple/Generated/KotlinRuntime"
        ),
        .target(
            name: "SharedBridge_DeponderCore",
            path: "apple/Generated/SharedBridge_DeponderCore"
        ),
        .target(
            name: "KotlinRuntimeSupportBridge",
            path: "apple/Generated/KotlinRuntimeSupportBridge"
        ),
        .target(
            name: "KotlinCoroutineSupportBridge",
            path: "apple/Generated/KotlinCoroutineSupportBridge"
        ),
        .target(
            name: "KotlinRuntimeSupport",
            dependencies: [
                "KotlinRuntimeSupportBridge",
                "KotlinRuntime",
                "DeponderCoreBridge",
            ],
            path: "apple/Generated/KotlinRuntimeSupport",
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ]
        ),
        .target(
            name: "KotlinCoroutineSupport",
            dependencies: [
                "KotlinRuntimeSupport",
                "KotlinCoroutineSupportBridge",
                "KotlinRuntime",
                "DeponderCoreBridge",
            ],
            path: "apple/Generated/KotlinCoroutineSupport",
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ]
        ),
        .target(
            name: "ExportedKotlinPackages",
            dependencies: [
                "KotlinRuntime",
                "DeponderCoreBridge",
            ],
            path: "apple/Generated/ExportedKotlinPackages",
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ]
        ),
        .target(
            name: "DeponderCore",
            dependencies: [
                "ExportedKotlinPackages",
                "KotlinRuntimeSupport",
                "KotlinCoroutineSupport",
                "SharedBridge_DeponderCore",
                "KotlinRuntime",
                "DeponderCoreBridge",
            ],
            path: "apple/Generated/DeponderCore",
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ],
            linkerSettings: [
                .linkedLibrary("c++"),
                .linkedFramework("Foundation"),
            ]
        ),
        .target(
            name: "Deponder",
            dependencies: [
                "DeponderCore",
            ],
            path: "apple/Sources/Deponder",
            swiftSettings: [
                .swiftLanguageMode(.v6),
            ]
        ),
        .testTarget(
            name: "DeponderTests",
            dependencies: [
                "Deponder",
                "DeponderCore",
            ],
            path: "apple/Tests/DeponderTests",
            swiftSettings: [
                .swiftLanguageMode(.v6),
            ]
        ),
    ]
)
