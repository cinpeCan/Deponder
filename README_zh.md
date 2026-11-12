# Deponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

<p align="start">
  <a href="README.md"><b>English</b></a>
</p>

面向 **Android 原生 View** 与 **Compose Multiplatform（Android / iOS / Desktop / Web Wasm）** 的**跨平台物理交互 SDK**。

通过轻量级 `Modifier` 与 `View` 扩展将物理能力与布局容器解耦，无需包裹专属父容器或改动视图树结构，即可让任意现有业务组件获得引力、斥力、弹簧连线与物理拖拽交互（适用于知识图谱、关系拓扑、浮动标签云与可交互卡片等场景）。

![Deponder 演示](https://s4.ax1x.com/2022/02/25/bAEZwj.gif)

## 核心特性

- **双端统一的接入模型**：在 Compose 与 Android View 上提供语义完全对称的 5 个接入接口与统一的 `dp` 物理参数体系，跨技术栈迁移零额外心智负担。
- **低侵入容器解耦与手势共存**：通过 `touchSlop` 门限与根容器坐标投影区分点击与拖拽，完整保留组件原有的 `.clickable {}`、`OnClickListener` 与水波纹反馈；拖拽置顶仅调整 `zIndex` / `translationZ`，不改变子视图层级顺序，也不修改宿主的 `clipChildren` / `clipToBounds`。
- **连线边缘自动避让与默认弹簧组件**：物理内核自动按两端节点的几何轮廓裁剪连线起止点，避免连线与箭头被节点卡片遮挡；内置开箱即用的 `DeponderDefaultRubber`（Compose）与 `DeponderRubberView`（Android View / XML），支持单双向箭头与线体上方透明悬浮标签。
- **帧率无关的高性能内核（`deponder-core`）**：
  - **跨屏恒速积分**：采用自适应 `1/120s` 子步积分，在 60Hz–240Hz 屏幕上保持一致的物理运动节奏，并对重叠或共线节点做确定性对称破缺。
  - **静止零开销与三级四叉树加速**：结合速度与加速度双阈值全局休眠（静止态 0% CPU 占用，微幅拖拽即时唤醒），按节点规模自动切换计算策略（64 以内直接遍历、64–255 单线程四叉树、256 及以上切入 `Dispatchers.Default` 并行快照计算）。
  - **仅图层级渲染更新**：通过 Compose `Modifier.Node` 图层变换与 View 硬件属性更新位移与形变，避免逐帧触发重新布局（Relayout）与重组（Recomposition）。
- **精简外部传递依赖**：`deponder-core` 仅依赖 `kotlin-stdlib` 与 `kotlinx-coroutines-core`；`deponder-compose` 仅依赖 `deponder-core`、`compose-runtime` 与 `compose-ui`（**零 `compose-foundation` 与 Material 依赖**）；`deponder` 仅暴露 `deponder-core` 与 `lifecycle-common`。

## 核心概念速览

| 接入能力 | Compose 接口 | Android View 接口 | 作用说明 |
| :--- | :--- | :--- | :--- |
| **物理场域（Root）** | `Modifier.deponderRoot` | `viewGroup.deponderRoot` | 在外层容器建立物理坐标系与帧循环调度 |
| **物理节点（Planet）** | `Modifier.deponderPlanet` | `view.deponderPlanet` | 将业务组件注册为具备质量、引斥力半径或锚定状态的节点 |
| **弹簧连线（Rubber）** | `Modifier.deponderRubber` | `view.deponderRubber` | 绑定两个节点 ID，建立胡克定律弹簧牵引关系（亦可直接使用内置默认连线组件） |
| **连线形变（Stretch）** | `Modifier.deponderStretch` | `view.deponderStretch` | 在自定义连线组件上跟随两端节点间距同步拉伸与旋转 |
| **拖拽交互（Drag）** | `Modifier.deponderDrag` | `view.deponderDrag` | 为节点按需启用与点击事件无冲突的物理拖拽手势 |

## 依赖配置

| Maven 构件 | 适用 UI 体系 | 支持平台 |
| :--- | :--- | :--- |
| `io.github.cinpecan:deponder-compose:0.5.0` | Jetpack Compose 与 Compose Multiplatform | Android、iOS、Desktop (JVM 17)、Web (`wasmJs`) |
| `io.github.cinpecan:deponder:0.5.0` | 传统 Android `View` / `ViewGroup` | Android（`minSdk 23`, JVM 17） |
| `io.github.cinpecan:deponder-core:0.5.0` | 纯 KMP 无 UI 物理计算内核（适合自绘 `Canvas` 或无头图布局） | Android、iOS、Desktop (JVM 17)、Web (`wasmJs`) |
| `io.github.cinpecan:deponder-bom:0.5.0` | 版本对齐清单（混合工程可选） | 全平台 |

**Compose Multiplatform（`commonMain`）或纯 Jetpack Compose 项目：**

```kotlin
dependencies {
    implementation("io.github.cinpecan:deponder-compose:0.5.0")
}
```

**传统 Android View 项目：**

```kotlin
dependencies {
    implementation("io.github.cinpecan:deponder:0.5.0")
}
```

**Android View + Compose 混合项目（共用同一份 `deponder-core` 内核）：**

```kotlin
dependencies {
    implementation(platform("io.github.cinpecan:deponder-bom:0.5.0"))
    implementation("io.github.cinpecan:deponder")
    implementation("io.github.cinpecan:deponder-compose")
}
```

## 快速上手：Compose Multiplatform

> 将 `Modifier.deponderRoot` 挂载在 `Box` 等无强排布约束的容器上。Compose 端支持任意静态嵌套深度（例如在底层 `Box` 声明 `DeponderDefaultRubber`，在上层容器内声明 `deponderPlanet`）。

```kotlin
val state = rememberDeponderState(
    RootPhysics.fromPreset(DeponderDefaults.Presets.Balanced)
)

Box(modifier = Modifier.fillMaxSize().deponderRoot(state)) {
    // 1. 开箱即用的默认弹簧连线（支持单/双向箭头与悬浮文字；自定义连线可组合 deponderRubber + deponderStretch）
    links.forEach { link ->
        DeponderDefaultRubber(
            startId = link.fromId,
            endId = link.toId,
            label = link.relation,
            arrowMode = DeponderArrowMode.BOTH,
            physics = RubberPhysics(naturalLengthDp = 120f),
        )
    }

    // 2. 任意现有业务 Composable —— 物理运动全程仅更新图形层，零重组开销
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

// 运行时动态控制：state.submitScale(1.2f)、state.submitRubberElasticity(2.4f)、state.setPinned(id, true)
```

## 快速上手：Android View（Kotlin & Java）

> 将 `deponderRoot` 挂载在 `FrameLayout`、`ConstraintLayout` 或 `CoordinatorLayout` 等容器上（Android View 端要求 `deponderPlanet` 与 `deponderRubber` 为 `deponderRoot` 的**直接子 View**）。

```kotlin
val controller = rootFrameLayout.deponderRoot(
    lifecycleOwner = this,
    physics = RootPhysics.fromPreset(DeponderDefaults.Presets.Balanced),
)

// 开箱即用的默认弹簧 View（亦支持 XML <com.cinpe.deponder.DeponderRubberView>；
// 自定义复合连线布局可组合 view.deponderRubber(...) 与 view.deponderStretch() / @string/deponder_stretch）
val rubberView = DeponderRubberView(this)
    .withLabel("依赖")
    .withArrowMode(DeponderArrowMode.END)
rootFrameLayout.addView(rubberView)
rubberView.deponderRubber("core", "ui", RubberPhysics(naturalLengthDp = 120f))

// 为现有直接子 View 配置物理力场与拖拽，完整保留原有 setOnClickListener
cardCore.deponderPlanet("core", PlanetPhysics(pinned = true))
cardUi.deponderPlanet("ui")
cardUi.deponderDrag()
cardUi.setOnClickListener { showDetail("ui") }

// 运行时动态控制：controller.submitScale(1.2f)、controller.submitRubberElasticity(2.4f)、controller.cancel()
```

> Java 调用方可通过 `DeponderViews.deponderRoot(...)`、`deponderPlanet(...)`、`deponderRubber(...)`、`deponderStretch(...)`、`deponderDrag(...)` 及 `new DeponderRubberView(context).withLabel(...)` 直接调用。

## 开源协议

本项目基于 Apache License 2.0 协议开源，完整条款见 [LICENSE](LICENSE)。

```text
Copyright (c) 2022-present, Cinpecan and Deponder Contributors.
Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
```
