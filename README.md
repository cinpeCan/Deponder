# Deponder

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cinpecan/deponder.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cinpecan%22%20AND%20a:%22deponder%22)

<p align="start">
  <a href="README_zh.md">
    <b>中文说明</b>
  </a>
</p> 

An Android's physical-like relationship graph animation SDK.  

In Deponder's physics-like animation, the displacement of an object is calculated as the integral of the force applied to each frame over time.

#### Set the following values to change the effect of the animation

- The mass of the planet (planetOption), the radiation range of the gravitational force (or repulsion), the coefficient of the gravitational (or repulsive) force
- The natural length of the rubber (rubberOption), the elastic coefficient
- The air friction coefficient of the environment (rootOption), the radiation range of the four-wall force, the coefficient of the four-wall force
- Scale (scale), effective for all planets (planetOption) and rubber (rubberOption)


### Features

- Calculate and change the View position through a matrix to avoid multiple triggering of View.invalidate() when moving, scaling, rotating, and flipping at the same time.
- In the animation, the actual position of the planet does not change, and it will not actively trigger onMeasure(), onLayout(), onDraw().
- Although the actual position of the planet has not changed, the touch event will be automatically shifted to the clicked planet in the vision for processing.
- Occupy the animation() interface of planetOption


### Setting up the dependency

Example for Gradle:

```groovy
repositories {
  mavenCentral()
}
```
```groovy
dependencies {
  implementation "io.github.cinpecan:deponder:0.3.1@aar"
}
```


### Start using


1. Create a Deponder object:
```java
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(LifecycleOwner, [YOUR GROUPVIEW]);
```

>or custom environment settings

```java
RootOption rootOption =SimpleRootOption.builder()
            .itemView([YOUR GROUPVIEW])
            //Initialize the scaling value (not necessary, default 1f)
            .initScale(...)
            //Maximum zoom value (not necessary, default 1.5f)
            .maxScale(...)
            //Minimum zoom value (not necessary, default 0.5f)
            .minScale(...)
            //Air damping (not necessary, default 0.0006f)
            .mRootDensity(...)
            //The influence range of the environment walls on the planetary force (not necessary, default 300)
            .mInternalPressure(...)
            //The elastic coefficient of the force of the four walls of the environment on the planet (not necessary, default 1.44f)
            .elasticityCoefficientStart(...)
            .elasticityCoefficientTop(...)
            .elasticityCoefficientEnd(...)
            .elasticityCoefficientBot(...)
            .build());
            
DeponderControl<PlanetOption, RubberOption> deponder = new Deponder<>(LifecycleOwner, rootOption);
```

2. Declare some planetOption object.
```java
PlanetOption planetA=SimplePlanet.builder()
            //[YOUR CHILD VIEW] under [YOUR GROUPVIEW], and want this view to be controlled by Deponder. (required)
            .itemView([YOUR CHILD VIEW])
            //Unique identifier (not necessary, default String.valueOf([YOUR CHILD VIEW].hashCode())
            .id(...)
            //quality (not necessary, default 2.293f)
            .quality(...)
            //The influence range of the interaction force between planets (not necessary, default 220)
            .mInternalPressure(...)
            //The elastic coefficient of the interaction force between planets (not necessary, default 1.33f)
            .elasticityCoefficient(...)
            .build();
```
It is also possible to declare more... then put into a collection.
```java
PlanetOption planetB = SimplePlanet.builder().(...).build();
...
List<PlanetOption> listPlanet=new ArrayList<>();
listPlanet.add(planetA);
listPlanet.add(planetB);
...
```

3. Declare a rubber(rubberOption) object.
```java
RubberOption rubberA=SimpleRubber.builder()
                //The id values of the two planet objects. Their combination should be unique. (required)
                .sId(planetA.id())
                .eId(planetB.id())
                //The view that the connection object is expected to display, such as a line segment 
                //(usually a View with a rectangle with a background color and a width and height greater than 0)
                .itemView(...)
                //The elastic coefficient of the rubber (not necessary, default 1.68f)
                .elasticityCoefficient(...)
                //The natural length of the eraser (not necessary, default 300)
                .naturalLength(...)
                .build();
```
It is also possible to declare more... also into a collection.
```java
RubberOption rubberB = SimpleRubber.builder().(...).build();
...
List<RubberOption> listRubber=new ArrayList<>();
listRubber.add(rubberA);
...
```
4. Commit them at the end, of course, committing an empty collection is also expected, after that you can also submit a new planet collection or rubber collection at any time.
```java
deponder.submitPlanet(listPlanet);(It is necessary to start the animation for the first time)
deponder.submitRubber(listRubber);(It is necessary to start the animation for the first time)
```
Submit the desired scaling, which can be useful when there are too many or too few Planets, and then you can always submit a new scaling.
```java
deponder.submitScale(1);(Not necessary, defaults to 1f)
```

>Well, now they are starting to move.

If you want to drag planets, you can customize the gesture listener, or use the simple drag gesture listener provided by Deponder.
```java
DeponderHelper.bindDefTouchPlanet(planetA);
DeponderHelper.bindDefTouchPlanet(planetB);
```

### More

- elasticityCoefficient, when it is positive, it shows repulsion. If you need gravity, you can set it to negative.

- The mInternalPressure (range of influence of the force) in PlanetOption is set large enough to make the planet completely evenly distributed in space.

- The spring's telescopic animation is to stretch the spring-wrapped View (or ViewGroup). If your rubble has child Views that need to maintain the width and height (for example, the deformation of TextView, ImageView, etc. will affect the look and feel),
  You can add tag: "UN_RUBBER_RUBBER" (@string/un_rubber) in this subview to keep its aspect ratio constant.
  E.g：
  ```
  view.addtag("UN_RUBBER_RUBBER")
  ```
  or add it in the xml layout of the child view
  ```
  <View
  ...
  android:tag="UN_RUBBER_RUBBER"
  ...
  />
  
  ```
  
- If you want the animation to become static as soon as possible, you can try to increase the elastic coefficient and reduce the planet quality.

- Deponder doesn't care what layout you're using, it all applies.


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
    
    Deponder baseUrl:https://github.com/cinpeCan/DemoDeponder
    
    Deponder Subcomponents:
    
    The following components are provided under the Apache License. See project link for details.
    The text of each license is the standard Apache 2.0 license.
    
    io.reactivex.rxjava3:rxandroid from https://github.com/ReactiveX/RxJava Apache-2.0 License
    com.google.guava from https://github.com/google/guava Apache-2.0 License
    com.google.auto.value from https://github.com/google/auto Apache-2.0 License

