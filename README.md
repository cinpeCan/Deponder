# DemoDeponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

一个Android原生的动画SDK

目的是利用原生优势,获得尽量高的性能(skip onMeasure,onLayout,onDraw)和尽量低的侵入性(no override view):

- 游动的view,仅第一帧经过onMeasure,onLayout,onDraw绘制, 其后view的真实位置不变
- 通过矩阵计算(占用view的animal接口)进行移动,缩放,旋转,翻转(不再触发onMeasure,onLayout,onDraw), 
  因此开发者仍然可以使用它们对view进行叠加调整.
- touch事件(占用view的touchListener接口)逆矩阵偏移到view真实位置上处理.
- 对view的left,top,right,bottom,translationX,translationY等接口均不主动占用.

#### 游动动画模仿物理特性
开发者可以通过设置 
- 散点(planetOption)的质量,引力(或斥力)的辐射范围,引力(或斥力)的系数
- 连线(rubberOption)的自然长度,弹性系数
- 环境(rootOption)的空气摩擦,四壁约束力(斥力)的辐射范围,四壁约束力(斥力)的系数
- 缩放(scale),对所有散点(planetOption)和连线(rubberOption)生效, 但对环境(rootOption)的视图和空气摩擦不生效(会对辐射范围和斥力生效).


以控制动画效果.

### 简单使用


1. 创建一个Deponder对象:
```java
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(LifecycleOwner/*生命周期持有者*/, [YOUR GROUPVIEW]);
```
2. 申明一些 散点(planetOption) 对象.
```java
PlanetOption planetA=SimplePlanet.builder()
            //在[YOUR GROUPVIEW]下的子view,且希望这个view由Deponder控制.
            .itemView([YOUR CHILD VIEW])
            //如果不打算用hashCode作为唯一标识,可以自行修改,例如使用UUID.
            .id([YOUR CHILD VIEW].hashCode())
            .build();
```
也可以申明更多...然后放入一个集合.
```java
PlanetOption planetB...
...
List<PlanetOption> listPlanet=new ArrayList();
listPlanet.add(planetA);
listPlanet.add(planetB);
...
```

3. 申明 连线(rubberOption) 对象.
```java
RubberOption rubberA=SimpleRubber.builder()
                //两个散点对象的id值.  他们的组合应是唯一的.
                .sId(planetA.id())
                .eId(planetB.id())
                .itemView([期望连线对象展示的view,例如是一条线段(矩形)])
                .build();
```
也可以申明更多...同样放入一个集合.
```java
RubberOption rubberB...
...
List<PlanetOption> listRubber=new ArrayList();
listRubber.add(rubberA);
...
```
4. 最后提交它们(第一次初始化的条件)当然,提交空的list也在预期中,之后你也可以随时提交新的view,旧的view将失去动画.
```java
deponder.submitPlanet(listPlanet);
deponder.submitRubber(listRubber);
```
提交希望的缩放比例(这不是初始化必须,因为默认初始值已经为1)当Planet数量有较大差距时,对于保持良好观感可能非常有用, 之后你也可以随时提交新的缩放比例.
```java
//deponder.submitScale(1);
```
好了,它们开始动起来了.

### 依赖

Example for Gradle:

```groovy
repositories {
  mavenCentral()
}

implementation 'io.github.cinpecan:deponder:0.2.1@aar'
```

or for Maven:

```xml
<dependency>
    <groupId>io.github.cinpecan</groupId>
    <artifactId>deponder</artifactId>
    <version>0.2.1</version>
</dependency>
```

### 更进一步

```java
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(this, SimpleRootOption.builder()
                //初始化缩放值
                .initScale()
                //最大缩放值
                .maxScale()
                //最小缩放值
                .minScale()
                //空气阻尼
                .mRootDensity()
                //斥力的辐射范围
                .mInternalPressure()
                //斥力系数
                .elasticityCoefficient()
                .build());
```

```java
PlanetOption planetA=SimplePlanet.builder()
            ...
            //质量
            .quality()
            //斥力(引力)辐射的范围
            .mInternalPressure()
            //斥力(引力)系数
            .elasticityCoefficient()
            .build();
```
            
```java
RubberOption rubberA=SimpleRubber.builder()
                ...
                //弹性系数
                .elasticityCoefficient()
                //弹簧的自然长度
                .naturalLength()
                .build();
```

- 动画并不改变view的实际位置.占用且仅占用animal接口.
- 占用planet的touchListen接口并不是必须的,后续版本将删除对该接口的占用,改为在上层viewGroup自动偏移touch事件,你可以像平时一样使用touchListen.
<!-- - 目前,可以在Rubber下需要保持宽高比不变的子view(例如TextView,ImageView)中,添加"UN_RUBBER_RUBBER"(@string/un_rubber) -->
<!--   ```  -->
<!--   addtag("UN_RUBBER_RUBBER")  -->
<!--   ```  -->
<!--   或在子view的xml布局中添加  -->
<!--   ```  -->
<!--   android:tag="UN_RUBBER_RUBBER"  -->
<!--   ```  -->
<!--   以保持宽高比恒定.  -->

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

