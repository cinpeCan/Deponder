# DemoDeponder
version 0.01

一个基于 Android原生 的散点图表库

特性是利用原生优势,达到高性能(skip onMeasure,onLayout,onDraw)且极低侵入性(no override view):

游动的view,仅第一帧经过onMeasure,onLayout,onDraw绘制, 其后view的真实位置不变,
通过矩阵计算和动画(占用view的animal接口)进行移动,缩放,旋转,翻转(不再触发onMeasure,onLayout,onDraw), 因此开发者仍然可以使用它们对view进行叠加调整.
touch事件也将自动通过(占用view的touchListener)逆矩阵偏移到view真实位置上处理.
对其它view的left,top,right,bottom,translationX,translationY等接口均不主动占用.

游动动画模仿物理特性,开发者可以通过设置 
散点(planetOption)的质量,引力(或斥力)的辐射范围,引力(或斥力)的系数
连线(rubberOption)的自然长度,弹性系数
环境(rootOption)的空气摩擦,四壁约束力(斥力)的辐射范围,四壁约束力(斥力)的系数
缩放(scale),对所有散点(planetOption)和连线(rubberOption)生效, 但对环境(rootOption)的视图和空气摩擦不生效(会对辐射范围和斥力生效).
来控制动画效果.

目前最新是0.01版.

