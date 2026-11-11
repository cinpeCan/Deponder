package com.cinpe.deponder

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.cinpe.deponder.internal.PhysicsEngine
import com.cinpe.deponder.internal.Vec2
import com.cinpe.deponder.reference.JavaPhysicsReference
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random
import kotlin.math.atan2

/**
 * Fuzzes the pure-float Kotlin [PhysicsEngine] against the verbatim [JavaPhysicsReference] across 30,000+ randomized inputs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhysicsEngineTest {

    private fun values(m: Matrix): FloatArray = FloatArray(9).also { m.getValues(it) }

    @Test
    fun wallForce_matchesJavaOracleAcross10000Cases() {
        val rng = Random(42L)
        repeat(10_000) { i ->
            val point = PointF(rng.nextFloat() * 1400f - 200f, rng.nextFloat() * 1400f - 200f)
            val scale = 0.5f + rng.nextFloat() * 1.5f
            val rootRect = RectF(0f, 0f, 300f + rng.nextFloat() * 1500f, 300f + rng.nextFloat() * 1500f)
            val pressure = rng.nextFloat() * 500f
            val ecS = rng.nextFloat() * 4f - 1f
            val ecT = rng.nextFloat() * 4f - 1f
            val ecE = rng.nextFloat() * 4f - 1f
            val ecB = rng.nextFloat() * 4f - 1f

            val expected = values(JavaPhysicsReference.wallForce(point, scale, rootRect, pressure, ecS, ecT, ecE, ecB))
            val actual = PhysicsEngine.wallForcePure(point, scale, rootRect, pressure, ecS, ecT, ecE, ecB)
            assertEquals("wallForce fx case $i", expected[Matrix.MTRANS_X], actual.x, 0f)
            assertEquals("wallForce fy case $i", expected[Matrix.MTRANS_Y], actual.y, 0f)
        }
    }

    @Test
    fun pairForce_matchesJavaOracleAcross10000Cases() {
        val rng = Random(1337L)
        repeat(10_000) { i ->
            val s = PointF(rng.nextFloat() * 1000f - 500f, rng.nextFloat() * 1000f - 500f)
            val e = if (i % 50 == 0) PointF(s.x, s.y) else PointF(rng.nextFloat() * 1000f - 500f, rng.nextFloat() * 1000f - 500f)
            val scale = 0.5f + rng.nextFloat() * 1.5f
            val rubberEc = if (i % 3 == 0) 0f else rng.nextFloat() * 3f - 0.5f
            val naturalLen = rng.nextInt(500)
            val planetEc = rng.nextFloat() * 3f - 1f
            val planetPressure = rng.nextFloat() * 400f

            val expected = values(JavaPhysicsReference.pairForce(s, e, scale, rubberEc, naturalLen, planetEc, planetPressure))
            val actual = PhysicsEngine.pairForcePure(s, e, scale, rubberEc, naturalLen, planetEc, planetPressure)
            assertEquals("pairForce fx case $i", expected[Matrix.MTRANS_X], actual.x, 0f)
            assertEquals("pairForce fy case $i", expected[Matrix.MTRANS_Y], actual.y, 0f)
        }
    }

    @Test
    fun integratePlanet_matchesJavaOracleAcross10000Cases() {
        val rng = Random(7L)
        repeat(10_000) { i ->
            val vx0 = rng.nextFloat() * 20f - 10f
            val vy0 = rng.nextFloat() * 20f - 10f
            val speedRef = Matrix().apply { setTranslate(vx0, vy0) }
            val speedKt = PointF(vx0, vy0)

            val scale = 0.5f + rng.nextFloat()
            val tx0 = rng.nextFloat() * 600f
            val ty0 = rng.nextFloat() * 600f
            val poRef = Matrix().apply {
                setScale(scale, scale)
                postTranslate(tx0, ty0)
            }
            val rect = Rect(0, 0, 20 + rng.nextInt(200), 20 + rng.nextInt(200))
            val pressed = i % 17 == 0
            val quality = 0.5f + rng.nextFloat() * 5f
            val density = rng.nextFloat() * 0.01f
            val fx = rng.nextFloat() * 400f - 200f
            val fy = rng.nextFloat() * 400f - 200f
            val newton = Matrix().apply { setTranslate(fx, fy) }
            val force = PointF(fx, fy)
            val dt = 8L + rng.nextInt(25)

            val beforePo = values(poRef)
            JavaPhysicsReference.drawPlanet(speedRef, poRef, rect, pressed, quality, density, newton, dt, scale)
            val delta = PhysicsEngine.integratePlanetPure(speedKt, pressed, quality, density, force, dt, scale)

            val expectedSpeed = values(speedRef)
            val afterPo = values(poRef)
            assertEquals("integratePlanet vx case $i", expectedSpeed[Matrix.MTRANS_X], speedKt.x, 0f)
            assertEquals("integratePlanet vy case $i", expectedSpeed[Matrix.MTRANS_Y], speedKt.y, 0f)
            assertEquals("integratePlanet dx case $i", afterPo[Matrix.MTRANS_X] - beforePo[Matrix.MTRANS_X], delta.x, 1e-4f)
            assertEquals("integratePlanet dy case $i", afterPo[Matrix.MTRANS_Y] - beforePo[Matrix.MTRANS_Y], delta.y, 1e-4f)
        }
    }

    @Test
    fun computeRubberTransform_producesContinuousFloatRotationAndExactGeometryAcross2000Cases() {
        val rng = Random(99L)
        repeat(2_000) { i ->
            val rWidth = 10 + rng.nextInt(300)
            val sP = PointF(rng.nextFloat() * 500f, rng.nextFloat() * 500f)
            val eP = PointF(sP.x + 5f + rng.nextFloat() * 400f, sP.y + 5f + rng.nextFloat() * 400f)
            val scale = 0.5f + rng.nextFloat()

            val transform = PhysicsEngine.computeRubberTransformPure(rWidth, sP, eP, scale)
            val dx = eP.x - sP.x
            val dy = eP.y - sP.y
            val expectedLen = PointF.length(dx, dy)
            val expectedDeg = (90.0 - Math.toDegrees(atan2(dx.toDouble(), dy.toDouble()))).toFloat()

            assertEquals("rubber centerX case $i", (sP.x + eP.x) * 0.5f, transform.centerX, 0f)
            assertEquals("rubber centerY case $i", (sP.y + eP.y) * 0.5f, transform.centerY, 0f)
            assertEquals("rubber length case $i", expectedLen, transform.length, 0f)
            assertEquals("rubber rotationDeg case $i", expectedDeg, transform.rotationDeg, 1e-4f)
            assertEquals("rubber childScaleX case $i", (rWidth.toFloat() / expectedLen) * scale, transform.childScaleX, 1e-6f)
            // Parent rubber View already applies `scaleY = scale`, so `childScaleY = 1f` gives uniform `scale` in screen space.
            assertEquals("rubber childScaleY case $i", 1f, transform.childScaleY, 0f)
        }
    }

    @Test
    fun vec2_packsAndUnpacksNegativeZeroAndArbitraryFloatsLosslessly() {
        assertEquals(0f, Vec2.Zero.x, 0f)
        assertEquals(0f, Vec2.Zero.y, 0f)
        val rng = Random(2026L)
        repeat(5_000) {
            val x = (rng.nextFloat() - 0.5f) * 1e6f
            val y = (rng.nextFloat() - 0.5f) * 1e6f
            val v = Vec2(x, y)
            assertEquals(x.toRawBits(), v.x.toRawBits())
            assertEquals(y.toRawBits(), v.y.toRawBits())
        }
    }
}
