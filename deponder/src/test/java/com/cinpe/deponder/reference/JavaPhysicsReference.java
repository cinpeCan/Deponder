package com.cinpe.deponder.reference;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

/**
 * Test oracle: a VERBATIM copy of the physics math of the original Java implementation
 * ({@code Deponder.evaluate(..)}, {@code drawPo}, {@code drawRo} and the {@code DeponderHelper.calculate/dDistance}
 * helpers as of v0.3.x). Field accesses were replaced with explicit parameters so the functions are pure.
 * <p>
 * Do not "improve" this file: it exists to prove the Kotlin engine is numerically identical.
 */
public final class JavaPhysicsReference {

    private JavaPhysicsReference() {
    }

    // ------------------------------------------------------------- DeponderHelper (verbatim)

    public static float[] values(Matrix matrix) {
        float[] floats = new float[9];
        matrix.getValues(floats);
        return floats;
    }

    public static PointF centerPointF(PointF s, PointF e) {
        return new PointF((s.x + e.x) / 2, (s.y + e.y) / 2);
    }

    /** {@code centerPointF(BaseOption)} with the option's hit rect / matrix passed explicitly. */
    public static PointF centerPointF(RectF src, Matrix matrix) {
        float[] values = values(matrix);
        float dw = src.width() * (values[Matrix.MSCALE_X] - 1) * 0.5f;
        float dh = src.height() * (values[Matrix.MSCALE_Y] - 1) * 0.5f;
        return new PointF(src.centerX() + values[Matrix.MTRANS_X] + dw, src.centerY() + values[Matrix.MTRANS_Y] + dh);
    }

    /** {@code centerPointFInternal(BaseOption)} with the option's hit rect / matrix passed explicitly. */
    public static PointF centerPointFInternal(Rect rect, Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        return new PointF((rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
    }

    public static float dDistance(float distance, float internalPressure) {
        if (distance == 0) {
            return 0;
        } else if (distance > 0) {
            return internalPressure - distance;
        } else {
            return -internalPressure - distance;
        }
    }

    public static float calculate(float difDistance, float elasticity_coefficient) {
        return difDistance * elasticity_coefficient;
    }

    public static PointF calculate(PointF difDistance, float elasticity_coefficient) {
        return new PointF(calculate(difDistance.x, elasticity_coefficient), calculate(difDistance.y, elasticity_coefficient));
    }

    public static PointF calculate(PointF speed, PointF acceleration, long dt) {
        float x = Math.round(speed.x * dt + .5f * acceleration.x * Math.pow(dt, 2)) / 1000000f;
        float y = Math.round(speed.y * dt + .5f * acceleration.y * Math.pow(dt, 2)) / 1000000f;
        return new PointF(x, y);
    }

    // ------------------------------------------------------------- Deponder (verbatim)

    /** {@code Deponder.evaluate(PointF, float)}: force of the four walls. */
    public static Matrix wallForce(PointF point, float scale, RectF rootRect, float rootInternalPressure,
                                   float ecStart, float ecTop, float ecEnd, float ecBot) {
        PointF distance = new PointF();
        final Matrix matrix = new Matrix();
        float l = point.x;
        float t = point.y;
        float r = rootRect.width() - point.x;
        float b = rootRect.height() - point.y;
        final float internalPressure = rootInternalPressure * scale;
        l = l < internalPressure ? internalPressure - l : 0;
        t = t < internalPressure ? internalPressure - t : 0;
        r = r < internalPressure ? internalPressure - r : 0;
        b = b < internalPressure ? internalPressure - b : 0;
        distance.x = calculate(l, ecStart) - calculate(r, ecEnd);
        distance.y = calculate(t, ecTop) - calculate(b, ecBot);
        matrix.postTranslate(distance.x, distance.y);
        return matrix;
    }

    /** {@code Deponder.evaluate(PointF, PointF, ...)}: force between two planets (+ optional rubber). */
    public static Matrix pairForce(PointF s, PointF e, float scale, float rubber_elasticity_coefficient,
                                   int rubber_natural_length, float elasticity_coefficient, float planet_internal_pressure) {
        final Matrix matrix = new Matrix();
        if (s.equals(e)) return matrix;
        final PointF diff = new PointF(e.x - s.x, e.y - s.y);
        double angle = Math.atan2(diff.x, diff.y);
        final float length = diff.length();
        if (rubber_elasticity_coefficient != 0) {
            final float natural = rubber_natural_length * scale;
            float rubberDiff = dDistance(length, natural);
            PointF rubberDiffDistance = new PointF(Math.round(Math.sin(angle) * rubberDiff), Math.round(Math.cos(angle) * rubberDiff));
            PointF rubberNewton = calculate(rubberDiffDistance, rubber_elasticity_coefficient);
            matrix.postTranslate(rubberNewton.x, rubberNewton.y);
        }
        final float mInternalPressure = planet_internal_pressure * scale;
        if (length < mInternalPressure) {
            float planetDiff = dDistance(length, mInternalPressure);
            PointF planetDiffDistance = new PointF(Math.round(Math.sin(angle) * planetDiff), Math.round(Math.cos(angle) * planetDiff));
            PointF planetDiffNewton = calculate(planetDiffDistance, elasticity_coefficient);
            matrix.postTranslate(planetDiffNewton.x, planetDiffNewton.y);
        }
        return matrix;
    }

    /** {@code Deponder.drawPo(..)}: integrates the planet's motion (mutates {@code speed} and {@code poMatrix}). */
    public static void drawPlanet(Matrix speedMatrix, Matrix poMatrix, Rect rect, boolean pressed, float quality,
                                  float rootDensity, Matrix newton, long interval, final float scale) {
        final float[] n = values(newton);
        final float[] s = values(speedMatrix);
        PointF acceleration = new PointF(n[Matrix.MTRANS_X] / quality, n[Matrix.MTRANS_Y] / quality);
        PointF speed = new PointF(s[Matrix.MTRANS_X], s[Matrix.MTRANS_Y]);
        acceleration.offset(-speed.x * rootDensity, -speed.y * rootDensity);
        PointF p = calculate(speed, acceleration, interval);
        final float minAcceleration = 0.01f * scale;
        float[] values = values(poMatrix);
        float tempX = scale / values[Matrix.MSCALE_X];
        float tempY = scale / values[Matrix.MSCALE_Y];
        Matrix matrix = new Matrix();
        matrix.postScale(tempX, tempY, (rect.width() / 2f) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X], (rect.height() / 2f) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]);
        if (p.length() < minAcceleration || pressed) {
            speedMatrix.setTranslate(0, 0);
            p.set(0, 0);
        } else {
            matrix.postTranslate(p.x, p.y);
            speedMatrix.postTranslate(acceleration.x * interval, acceleration.y * interval);
        }
        poMatrix.postConcat(matrix);
    }

    /** {@code Deponder.drawRo(..)}: stretches/rotates the rubber (mutates {@code roMatrix} and {@code vMatrices}). */
    public static void drawRubber(Matrix roMatrix, Rect rRect, List<Rect> vRects, List<Matrix> vMatrices,
                                  PointF sP, PointF eP, final float scale) {
        final PointF diff = new PointF(eP.x - sP.x, eP.y - sP.y);
        final PointF center = centerPointF(sP, eP);
        final float length = PointF.length(diff.x, diff.y);
        final double angle = Math.atan2(diff.x, diff.y);
        final Matrix d = new Matrix();
        d.setRectToRect(new RectF(rRect), new RectF(center.x - length / 2, center.y - rRect.height() * scale / 2, center.x + length / 2, center.y + rRect.height() * scale / 2), Matrix.ScaleToFit.FILL);
        d.postRotate(Math.round(90 - angle / Math.PI * 180), center.x, center.y);
        for (int i = 0; i < vMatrices.size(); i++) {
            PointF c = centerPointFInternal(vRects.get(i), vMatrices.get(i));
            vMatrices.get(i).setScale(rRect.width() / length * scale, scale, c.x, c.y);
        }
        roMatrix.set(d);
    }
}
