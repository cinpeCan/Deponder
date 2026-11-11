# Deponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

<p align="start">
  <a href="README_zh.md">
    <b>中文说明</b>
  </a>
</p>

An Android physical-like relationship graph animation SDK, powered by Kotlin & Coroutines while maintaining 100% Java source compatibility.

In Deponder's physics-like animation, the displacement of an object is calculated as the integral of the force applied to each frame over time.

#### Set the following values to change the effect of the animation

- The mass of the planet (`planetOption`), the radiation range of the gravitational force (or repulsion), the coefficient of the gravitational (or repulsive) force
- The natural length of the rubber (`rubberOption`), the elastic coefficient
- The air friction coefficient of the environment (`rootOption`), the radiation range of the four-wall force, the coefficient of the four-wall force
- Scale (`scale`), effective for all planets (`planetOption`) and rubbers (`rubberOption`)


### Features

- Driven by Android `Choreographer` VSYNC and hardware-accelerated `View` properties (`translationX/Y`, `scaleX/Y`, `rotation`) without `invalidate()` redraw overhead.
- Preserves layout bounds during animation without triggering `onMeasure()`, `onLayout()`, or `onDraw()`.
- Native `ViewGroup` matrix touch routing dispatches click and drag gestures directly to translated/scaled planets.
- Leaves `View.startAnimation()` and `ViewGroup.setTouchDelegate()` free for custom animations and touch handling.


### Setting up the dependency

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


### Start using (Kotlin DSL)

Deponder provides a concise Kotlin DSL to build controllers without subclassing:

```kotlin
// Optional: custom environment settings (or pass rootView directly to deponder(...))
val rootOption = simpleRoot {
    itemView = rootGroupView
    initScale = 1f
    maxScale = 1.5f
    minScale = 0.5f
    mRootDensity = 0.0006f
    mInternalPressure = 300f
}

// Create a Deponder controller with lambdas — no subclassing required
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

    // Optional diff callback (defaults to Objects.equals)
    areContentsTheSameByP { oldItem, newItem -> oldItem == newItem }
}

// Submit data at any time
control.submitPlanet(nodeList)
control.submitRubber(linkList)
control.submitScale(1f)
```


### Start using (Java)

All Java APIs (`new Deponder<>(...)`, `SimplePlanet.builder()`, `SimpleRubber.builder()`, `SimpleRoot.builder()`, `DeponderHelper`) remain fully supported:

1. Create a `Deponder` object by subclassing `Deponder<P, R>`:
```java
SimpleRootOption rootOption = SimpleRoot.builder()
        .itemView(rootGroupView)
        // Initial scaling value (optional, default 1f)
        .initScale(1f)
        // Maximum zoom value (optional, default 1.5f)
        .maxScale(1.5f)
        // Minimum zoom value (optional, default 0.5f)
        .minScale(0.5f)
        // Air damping (optional, default 0.0006f)
        .mRootDensity(0.0006f)
        // Influence range of the environment walls on planetary force (optional, default 300)
        .mInternalPressure(300f)
        // Elastic coefficient of the four walls (optional, default 1.44f)
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
                // Child View to be controlled by Deponder (required)
                .itemView(inflateNodeView(parent, item))
                // Unique identifier matching planetId(item)
                .id(item.getId())
                // Mass (optional, default 2.293f)
                .quality(2.293f)
                // Influence range of interaction force between planets (optional, default 220)
                .mInternalPressure(220f)
                // Elastic coefficient between planets (optional, default 1.33f)
                .elasticityCoefficient(1.33f)
                .build();
    }

    @NonNull
    @Override
    public SimpleRubber createRubber(@NonNull ViewGroup parent, LinkItem link) {
        return SimpleRubber.builder()
                // Endpoint planet IDs; their combination must be unique (required)
                .sId(link.getFromId())
                .eId(link.getToId())
                // View displayed for the connection (required)
                .itemView(inflateEdgeView(parent))
                // Elastic coefficient of the rubber (optional, default 1.68f)
                .elasticityCoefficient(1.68f)
                // Natural length of the rubber (optional, default 300)
                .naturalLength(300)
                .build();
    }
};
```
> You can also use `new Deponder<NodeItem, LinkItem>(lifecycleOwner, rootGroupView) { ... }` to use default environment parameters.

2. Submit planets, rubbers, and optional scale at any time:
```java
deponder.submitPlanet(listPlanet);
deponder.submitRubber(listRubber);
deponder.submitScale(1f); // Optional, defaults to initScale (1f)
```

3. Planet drag gesture listener (automatically bound for managed planets, or usable standalone):
```java
DeponderHelper.bindDefTouchPlanet(planetA);
```


### More

- `elasticityCoefficient`: when positive, it acts as repulsion; set it to negative if you want attraction/gravity.
- Setting `mInternalPressure` (range of influence of the force) in `PlanetOption` large enough makes planets distribute evenly across the space.
- The rubber's telescopic animation stretches the wrapped `View` (or `ViewGroup`). If your rubber has child `View`s that must maintain their aspect ratio (such as `TextView` or `ImageView`), add the tag `"UN_RUBBER_RUBBER"` (`@string/un_rubber` / `DeponderHelper.TAG_OF_UN_RUBBER`) on that child `View`:
  ```java
  view.setTag("UN_RUBBER_RUBBER");
  ```
  or in XML:
  ```xml
  <View
      ...
      android:tag="UN_RUBBER_RUBBER"
      ... />
  ```
- If you want the animation to settle into equilibrium faster, try increasing the elastic coefficient and reducing the planet `quality` (mass).
- Deponder works with any `ViewGroup` layout container.


### Release Notes (0.4.0)

- **Kotlin & Coroutines**: Rewritten in Kotlin with Coroutines and a built-in Kotlin DSL; public integration APIs remain unchanged for seamless upgrades (`minSdk 21`, `JVM 11`).
- **Lightweight dependencies**: Removed transitive dependencies on RxJava3 and Guava, retaining only AndroidX Lifecycle / Annotation and Kotlin Coroutines.


### Effect example

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
