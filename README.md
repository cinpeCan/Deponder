# Deponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

<p align="start">
  <a href="README_zh.md"><b>中文说明</b></a>
</p>

A **Multiplatform Physics Interaction SDK** for **Android Views** and **Compose Multiplatform** (Android, iOS, Desktop, Web/Wasm).

Decouple force-directed physics from layout containers via lightweight Compose `Modifier`s and Android `View` extensions. Add attraction-repulsion fields, spring links, and smooth dragging directly to standard UI components without wrapper layouts or view-tree mutations — ideal for knowledge graphs, topology views, interactive tag clouds, and floating cards.

![Deponder demo](https://s4.ax1x.com/2022/02/25/bAEZwj.gif)

## Key Features

- **Unified Cross-Stack API**: Identical 5-part mental model and `dp`-based physics configuration across Compose Multiplatform and Android Views, with full Java interop.
- **Low-Intrusive & Gesture-Safe**: Preserves standard `.clickable {}`, `OnClickListener`, and ripple feedback via `touchSlop` gating and root-space coordinate projection; elevates dragged items via `zIndex` / `translationZ` without reordering child views or mutating `clipChildren` / `clipToBounds`.
- **Automatic Contour Clearance & Built-in Spring Links**: Trims link endpoints against node bounds automatically so neither built-in nor custom link views get buried beneath cards; includes ready-to-use `DeponderDefaultRubber` (Compose) and `DeponderRubberView` (Android View / XML) with arrowheads and floating labels.
- **Frame-Rate-Independent Physics Kernel (`deponder-core`)**:
  - **Consistent Motion Across 60Hz–240Hz**: Adaptive `1/120s` sub-stepping keeps simulation speed consistent across display refresh rates, paired with deterministic symmetry breaking for stacked or collinear nodes.
  - **Idle 0% CPU & 3-Tier Hybrid Quadtree**: Dual velocity/acceleration sleep thresholds drop CPU usage to 0% at rest with instant drag wakeup, scaling automatically across node counts (`< 64` direct array, `64–255` single-thread quadtree, `256+` parallel snapshot evaluation on `Dispatchers.Default`).
  - **Layer-Only Frame Updates**: Drives displacement and deformation via Compose `Modifier.Node` graphics layers and Android `View` layer transforms without triggering per-frame relayout or recomposition.
- **Minimal Transitive Footprint**: `deponder-core` depends only on `kotlin-stdlib` and `kotlinx-coroutines-core`; `deponder-compose` depends strictly on `deponder-core`, `compose-runtime`, and `compose-ui` (**zero `compose-foundation` or Material dependency**); `deponder` exposes only `deponder-core` and `lifecycle-common`.

## Core Concepts at a Glance

| Role | Compose API | Android View API | Purpose |
| :--- | :--- | :--- | :--- |
| **Root Field** | `Modifier.deponderRoot` | `viewGroup.deponderRoot` | Establishes the physics coordinate space and frame loop on a container |
| **Node (Planet)** | `Modifier.deponderPlanet` | `view.deponderPlanet` | Registers a UI component as a physics body with mass, repulsion radius, or pinning |
| **Spring Link (Rubber)** | `Modifier.deponderRubber` | `view.deponderRubber` | Connects two node IDs with configurable spring attraction (or use built-in default rubbers) |
| **Link Transform (Stretch)** | `Modifier.deponderStretch` | `view.deponderStretch` | Stretches and rotates a custom link component dynamically between two nodes |
| **Drag Gesture (Drag)** | `Modifier.deponderDrag` | `view.deponderDrag` | Adds opt-in, click-safe physics dragging to a node |

## Artifacts & Setup

| Artifact | Target UI Stack | Platforms |
| :--- | :--- | :--- |
| `io.github.cinpecan:deponder-compose:0.5.0` | Jetpack Compose & Compose Multiplatform | Android, iOS, Desktop (JVM 17), Web (`wasmJs`) |
| `io.github.cinpecan:deponder:0.5.0` | Traditional Android `View` / `ViewGroup` | Android (`minSdk 23`, JVM 17) |
| `io.github.cinpecan:deponder-core:0.5.0` | UI-agnostic KMP physics kernel (custom `Canvas` / headless graphs) | Android, iOS, Desktop (JVM 17), Web (`wasmJs`) |
| `io.github.cinpecan:deponder-bom:0.5.0` | Version alignment BOM (optional for hybrid projects) | All |

**Compose Multiplatform (`commonMain`) or Pure Jetpack Compose:**

```kotlin
dependencies {
    implementation("io.github.cinpecan:deponder-compose:0.5.0")
}
```

**Traditional Android View:**

```kotlin
dependencies {
    implementation("io.github.cinpecan:deponder:0.5.0")
}
```

**Hybrid Android View + Compose Project (with BOM):**

```kotlin
dependencies {
    implementation(platform("io.github.cinpecan:deponder-bom:0.5.0"))
    implementation("io.github.cinpecan:deponder")
    implementation("io.github.cinpecan:deponder-compose")
}
```

## Quick Start: Compose Multiplatform

> Host `Modifier.deponderRoot` on an unconstrained container such as `Box`. In Compose, `deponderPlanet` and `deponderRubber` can be declared at any static nesting depth inside `deponderRoot`.

```kotlin
val state = rememberDeponderState(
    RootPhysics.fromPreset(DeponderDefaults.Presets.Balanced)
)

Box(modifier = Modifier.fillMaxSize().deponderRoot(state)) {
    // 1. Ready-to-use default spring link (for custom link UIs, combine Modifier.deponderRubber + Modifier.deponderStretch)
    links.forEach { link ->
        DeponderDefaultRubber(
            startId = link.fromId,
            endId = link.toId,
            label = link.relation,
            arrowMode = DeponderArrowMode.BOTH,
            physics = RubberPhysics(naturalLengthDp = 120f),
        )
    }

    // 2. Any standard Composable — layer-only updates with zero recomposition during physics frames
    nodes.forEach { node ->
        NodeCardComposable(
            node = node,
            modifier = Modifier
                .deponderPlanet(
                    id = node.id,
                    physics = PlanetPhysics(repulsionRadiusDp = 90f, pinned = node.isAnchor),
                )
                .deponderDrag()
                .clickable { onNodeClicked(node) },
        )
    }
}

// Runtime control: state.submitScale(1.2f), state.submitRubberElasticity(2.4f), state.setPinned(id, true)
```

## Quick Start: Android View (Kotlin & Java)

> Host `deponderRoot` on an unconstrained `ViewGroup` such as `FrameLayout`, `ConstraintLayout`, or `CoordinatorLayout` (`deponderPlanet` and `deponderRubber` views must be **direct children** of `deponderRoot`).

```kotlin
val controller = rootFrameLayout.deponderRoot(
    lifecycleOwner = this,
    physics = RootPhysics.fromPreset(DeponderDefaults.Presets.Balanced),
)

// Ready-to-use default spring View (also supports XML <com.cinpe.deponder.DeponderRubberView>;
// for custom compound link Views, combine view.deponderRubber(...) with view.deponderStretch() or @string/deponder_stretch)
val rubberView = DeponderRubberView(this)
    .withLabel("depends on")
    .withArrowMode(DeponderArrowMode.END)
rootFrameLayout.addView(rubberView)
rubberView.deponderRubber("core", "ui", RubberPhysics(naturalLengthDp = 120f))

// Configure force fields and dragging on direct-child Views while keeping setOnClickListener intact
cardCore.deponderPlanet("core", PlanetPhysics(pinned = true))
cardUi.deponderPlanet("ui")
cardUi.deponderDrag()
cardUi.setOnClickListener { showDetail("ui") }

// Runtime control: controller.submitScale(1.2f), controller.submitRubberElasticity(2.4f), controller.cancel()
```

> Java callers can use static & fluent helpers on `DeponderViews` (`deponderRoot`, `deponderPlanet`, `deponderRubber`, `deponderStretch`, `deponderDrag`) and `new DeponderRubberView(context).withLabel(...)`.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.

```text
Copyright (c) 2022-present, Cinpecan and Deponder Contributors.
Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
```
