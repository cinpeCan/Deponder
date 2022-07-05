# DemoDeponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

一个Android原生仿物理特性的关系图谱动画SDK 

在Deponder仿物理特性的动画中，对象的位移是根据施加到每一帧的受力分析与时间积分计算的。

#### 开发者可以通过设置以下值来改变动画的效果

- 行星(planetOption)的质量,引力(或斥力)的辐射范围,引力(或斥力)的系数
- 橡皮(rubberOption)的自然长度,弹性系数
- 环境(rootOption)的空气摩擦,四壁作用力的辐射范围,四壁作用力的系数
- 缩放(scale),对所有散点(planetOption)和连线(rubberOption)生效


### 动画特点

- 通过矩阵计算和改变View位置,避免同时进行移动,缩放,旋转,翻转时多次触发invalidate
- 动画中，planet的实际位置不发生改变，不会主动触发onMeasure,onLayout,onDraw
- 虽然planet的实际位置完全不变，但touch的事件将被自动偏移到视觉中点击的planet上处理
- 占用planet的animation接口


### 依赖

Example for Gradle:

```groovy
repositories {
  mavenCentral()
}
```
```groovy
dependencies {
  implementation "io.github.cinpecan:deponder:0.2.7@aar"
}
```



### 开始使用


1. 创建一个Deponder对象:
```java
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(LifecycleOwner, [YOUR GROUPVIEW]);
```

>或者 自定义环境设定

```java
RootOption rootOption =SimpleRootOption.builder()
            .itemView([YOUR GROUPVIEW])
            //初始化缩放值(非必要，默认1f)
            .initScale(...)
            //最大缩放值(非必要，默认1.5f)
            .maxScale(...)
            //最小缩放值(非必要，默认0.5f)
            .minScale(...)
            //空气阻尼(非必要，默认0.0006f)
            .mRootDensity(...)
            //环境四壁对行星作用力的影响范围(非必要，默认300)
            .mInternalPressure(...)
            //环境四壁对行星作用力的弹性系数(非必要，默认1.44f)
            .elasticityCoefficientStart(...)
            .elasticityCoefficientTop(...)
            .elasticityCoefficientEnd(...)
            .elasticityCoefficientBot(...)
            .build());
            
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(LifecycleOwner, rootOption);
```

2. 申明一些 行星(planetOption) 对象.
```java
PlanetOption planetA=SimplePlanet.builder()
            //在[YOUR GROUPVIEW]下的[YOUR CHILD VIEW],且希望这个view由Deponder控制.(必要)
            .itemView([YOUR CHILD VIEW])
            //唯一标识(非必要，默认String.valueOf([YOUR CHILD VIEW].hashCode())
            .id(...)
            //质量(非必要，默认2.293f)
            .quality(...)
            //行星间相互作用力的影响范围(非必要，默认220)
            .mInternalPressure(...)
            //行星间相互作用力的弹性系数(非必要，默认1.33f)
            .elasticityCoefficient(...)
            .build();
```
也可以申明更多...然后放入一个集合.
```java
PlanetOption planetB = SimplePlanet.builder().(...).build();
...
List<PlanetOption> listPlanet=new ArrayList<>();
listPlanet.add(planetA);
listPlanet.add(planetB);
...
```

3. 申明 连线(rubberOption) 对象.
```java
RubberOption rubberA=SimpleRubber.builder()
                //两个散点对象的id值.  他们的组合应是唯一的.(必要)
                .sId(planetA.id())
                .eId(planetB.id())
                .itemView([期望连线对象展示的view,例如是一条线段(一般为矩形有背景颜色且宽高大于0的View)])
                //弹簧的弹性系数(非必要，默认1.68f)
                .elasticityCoefficient(...)
                //弹簧的自然长度(非必要，默认300)
                .naturalLength(...)
                .build();
```
也可以申明更多...同样放入一个集合.
```java
RubberOption rubberB = SimpleRubber.builder().(...).build();
...
List<RubberOption> listRubber=new ArrayList<>();
listRubber.add(rubberA);
...
```
4. 最后提交它们,之后你也可以随时提交新的planet集合或rubber集合.
```java
deponder.submitPlanet(listPlanet);(第一次启动动画时必须)
deponder.submitRubber(listRubber);(第一次启动动画时必须)
```
提交希望的缩放比例,当Planet数量过多或过少时,可能非常有用,之后你也可以随时提交新的缩放比例.
```java
deponder.submitScale(1);(非必须,默认为1f)
```
>好了,它们开始动起来了.

如果你想要拖拽它们，你可以自定义手势监听，也可以使用Deponder提供的简单拖拽手势监听。
```java
DeponderHelper.bindDefTouchPlanet(planetA);
DeponderHelper.bindDefTouchPlanet(planetB);
```

### 更进一步

- elasticityCoefficient弹性系数，为正数时表现的是斥力，如果你需要引力可以设为负数。

- PlanetOption中mInternalPressure(力的影响范围)设置得足够大，可以使planet完全均匀得分布在空间中。

- 橡皮(rubber)的伸缩动画是将弹簧包装的View(或ViewGroup)进行拉伸,如果你的rubble有需要保持宽高的子View(如TextView,ImageView等形变会影响观感), 
  可以在该子view中,添加tag:"UN_RUBBER_RUBBER"(@string/un_rubber),以保持它的宽高比恒定。
  例如：
  ```
  view.addtag("UN_RUBBER_RUBBER")
  ```
  或在子view的xml布局中添加
  ```
  <View
  ...
  android:tag="UN_RUBBER_RUBBER"
  ...
  />
  
  ```
  
- 如果希望动画尽快趋于静态,可以尝试增大弹性系数和降低planet质量。
  
- Deponder并不关心你使用的是何种布局，全部适用。


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
    
    Deponder baseUrl:https://github.com/cinpeCan/DemoDeponder
    
    Deponder Subcomponents:
    
    The following components are provided under the Apache License. See project link for details.
    The text of each license is the standard Apache 2.0 license.
    
    io.reactivex.rxjava3:rxandroid from https://github.com/ReactiveX/RxJava Apache-2.0 License
    com.google.guava from https://github.com/google/guava Apache-2.0 License
    com.google.auto.value from https://github.com/google/auto Apache-2.0 License

