# Deponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

一个 Android 原生仿物理特性的关系图谱动画 SDK（基于 Kotlin 与 Coroutines/Flow 构建，同时 100% 保持 Java 源码兼容）。

在 Deponder 仿物理特性的动画中，对象的位移是根据施加到每一帧的受力分析与时间积分计算的。

#### 开发者可以通过设置以下值来改变动画的效果

- 行星（`planetOption`）的质量、引力（或斥力）的辐射范围、引力（或斥力）的系数
- 橡皮（`rubberOption`）的自然长度、弹性系数
- 环境（`rootOption`）的空气摩擦、四壁作用力的辐射范围、四壁作用力的系数
- 缩放（`scale`），对所有散点（`planetOption`）和连线（`rubberOption`）生效


### 动画特点

- 基于 Android 原生 `Choreographer` 帧同步驱动硬件加速属性变换（`translationX/Y`、`scaleX/Y`、`rotation`），无 `invalidate()` 重绘开销
- 动画过程中不改变子 `View` 的布局边界，不触发 `onMeasure`、`onLayout`、`onDraw`
- 依托 `ViewGroup` 原生矩阵触摸分发，平移与缩放后的 planet 可直接响应点击与拖拽手势
- 不占用 `View.startAnimation()` 与 `ViewGroup.setTouchDelegate()`，业务层可自由叠加自定义动画与触摸逻辑


### 依赖

Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("io.github.cinpecan:deponder:0.4.0")
}
```

Groovy (`build.gradle`):

```groovy
dependencies {
    implementation "io.github.cinpecan:deponder:0.4.0"
}
```


### 开始使用（Kotlin DSL）

Deponder 内置了简洁的 Kotlin DSL，无需编写子类即可构建控制器：

```kotlin
// 可选：自定义空间环境参数（也可直接将 rootGroupView 传给 deponder(...)）
val rootOption = simpleRoot {
    itemView = rootGroupView
    initScale = 1f
    maxScale = 1.5f
    minScale = 0.5f
    mRootDensity = 0.0006f
    mInternalPressure = 300f
}

// 通过 lambda 构建控制器，无需编写子类
val control = deponder<NodeItem, LinkItem>(lifecycleOwner, rootOption) {
    planetId { it.id }
    rubberPairId { Pair(it.fromId, it.toId) }

    createPlanet { parent, node ->
        simplePlanet {
            itemView = inflateNodeView(parent, node)
            id = node.id
            quality = 2.293f
            mInternalPressure = 220f
            elasticityCoefficient = 1.33f
        }
    }

    bindPlanet { holder, node ->
        updateNodeView(holder.itemView(), node)
        holder.copy { id = node.id }
    }

    createRubber { parent, link ->
        simpleRubber {
            itemView = inflateEdgeView(parent)
            sId = link.fromId
            eId = link.toId
            elasticityCoefficient = 1.68f
            naturalLength = 300
        }
    }

    // 可选：自定义内容比对（默认使用 Objects.equals）
    areContentsTheSameByP { oldItem, newItem -> oldItem == newItem }
}

// 随时提交或更新数据集合与缩放比例
control.submitPlanet(nodeList)
control.submitRubber(linkList)
control.submitScale(1f)
```


### 开始使用（Java）

原有 Java 调用方式（`new Deponder<>(...)`、`SimplePlanet.builder()`、`SimpleRubber.builder()`、`SimpleRoot.builder()`、`DeponderHelper`）完全保持不变：

1. 继承 `Deponder<P, R>` 创建控制器对象：
```java
SimpleRootOption rootOption = SimpleRoot.builder()
        .itemView(rootGroupView)
        // 初始化缩放值（非必要，默认 1f）
        .initScale(1f)
        // 最大缩放值（非必要，默认 1.5f）
        .maxScale(1.5f)
        // 最小缩放值（非必要，默认 0.5f）
        .minScale(0.5f)
        // 空气阻尼（非必要，默认 0.0006f）
        .mRootDensity(0.0006f)
        // 环境四壁对行星作用力的影响范围（非必要，默认 300）
        .mInternalPressure(300f)
        // 环境四壁对行星作用力的弹性系数（非必要，默认 1.44f）
        .elasticityCoefficientStart(1.44f)
        .elasticityCoefficientTop(1.44f)
        .elasticityCoefficientEnd(1.44f)
        .elasticityCoefficientBot(1.44f)
        .build();

DeponderControl<NodeItem, LinkItem> deponder = new Deponder<NodeItem, LinkItem>(lifecycleOwner, rootOption) {
    @Override
    public String planetId(NodeItem item) {
        return item.getId();
    }

    @Override
    public Pair<String, String> rubberPairId(LinkItem link) {
        return Pair.create(link.getFromId(), link.getToId());
    }

    @NonNull
    @Override
    public SimplePlanet createPlanet(@NonNull ViewGroup parent, NodeItem item) {
        return SimplePlanet.builder()
                // 在 rootGroupView 下由 Deponder 控制的子 View（必要）
                .itemView(inflateNodeView(parent, item))
                // 唯一标识，与 planetId(item) 一致
                .id(item.getId())
                // 质量（非必要，默认 2.293f）
                .quality(2.293f)
                // 行星间相互作用力的影响范围（非必要，默认 220）
                .mInternalPressure(220f)
                // 行星间相互作用力的弹性系数（非必要，默认 1.33f）
                .elasticityCoefficient(1.33f)
                .build();
    }

    @NonNull
    @Override
    public SimpleRubber createRubber(@NonNull ViewGroup parent, LinkItem link) {
        return SimpleRubber.builder()
                // 两个散点对象的 id 值，组合应唯一（必要）
                .sId(link.getFromId())
                .eId(link.getToId())
                // 期望连线对象展示的 View（必要）
                .itemView(inflateEdgeView(parent))
                // 弹簧的弹性系数（非必要，默认 1.68f）
                .elasticityCoefficient(1.68f)
                // 弹簧的自然长度（非必要，默认 300）
                .naturalLength(300)
                .build();
    }
};
```
> 也可以使用 `new Deponder<NodeItem, LinkItem>(lifecycleOwner, rootGroupView) { ... }` 直接采用默认空间配置。

2. 提交散点集合、连线集合以及缩放比例（后续可随时再次提交新集合）：
```java
deponder.submitPlanet(listPlanet);
deponder.submitRubber(listRubber);
deponder.submitScale(1f); // 非必须，默认为 initScale（1f）
```

3. 拖拽手势监听（受 `Deponder` 管理的 planet 已自动绑定，也可单独调用）：
```java
DeponderHelper.bindDefTouchPlanet(planetA);
```


### 更进一步

- `elasticityCoefficient` 弹性系数：为正数时表现的是斥力，如果需要引力可以设为负数。
- `PlanetOption` 中 `mInternalPressure`（力的影响范围）设置得足够大，可以使 planet 均匀地分布在空间中。
- 橡皮（rubber）的伸缩动画是将弹簧包装的 `View`（或 `ViewGroup`）进行拉伸；如果你的 rubber 内有需要保持宽高比的子 `View`（如 `TextView`、`ImageView` 等形变会影响观感），可以在该子 `View` 中添加 tag `"UN_RUBBER_RUBBER"`（`@string/un_rubber` / `DeponderHelper.TAG_OF_UN_RUBBER`）以保持宽高比恒定：
  ```java
  view.setTag("UN_RUBBER_RUBBER");
  ```
  或在子 View 的 XML 布局中添加：
  ```xml
  <View
      ...
      android:tag="UN_RUBBER_RUBBER"
      ... />
  ```
- 如果希望动画尽快趋于静态，可以尝试增大弹性系数并降低 planet 质量（`quality`）。
- Deponder 并不关心你使用的是何种布局容器，全部适用。


### 0.4.0 更新说明

- **Kotlin 与协程架构**：全面升级为 Kotlin 与 Coroutines 实现，内置 Kotlin DSL；外部接入接口保持不变，原有业务代码可直接平滑升级（环境要求：`minSdk 21`、`JVM 11`）。
- **轻量级依赖**：移除 RxJava3、Guava 等第三方传递依赖，仅保留 AndroidX Lifecycle / Annotation 与 Kotlin Coroutines。


### 效果示例

![Image](https://s4.ax1x.com/2022/02/25/bAEZwj.gif)

## NOTICE

    Copyright (c) 2022-present, Cinpecan and Deponder Contributors.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    Deponder baseUrl: https://github.com/cinpeCan/Deponder

    Deponder Subcomponents:

    The following components are provided under the Apache License 2.0. See project links for details.

    org.jetbrains.kotlinx:kotlinx-coroutines-android from https://github.com/Kotlin/kotlinx.coroutines Apache-2.0 License
    androidx.lifecycle / androidx.annotation from https://source.android.com/ Apache-2.0 License
