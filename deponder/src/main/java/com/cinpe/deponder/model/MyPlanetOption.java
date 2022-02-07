package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.PlanetOption;
import com.google.auto.value.AutoValue;

import java.util.UUID;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class MyPlanetOption extends PlanetOption {


    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract View itemView();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract String id();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract PointF speed();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract NAnimator animator();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract Matrix matrix();

    @Override
    @AutoValue.CopyAnnotations
    public abstract float quality();

    @Override
    @AutoValue.CopyAnnotations
    public abstract float frontalArea();

    @Override
    public abstract float mInternalPressure();

    @Override
    public abstract float elasticityCoefficient();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract PointF acceleration();

    public static MyPlanetOption create(View itemView, String id, float quality, float frontalArea, float mInternalPressure, float elasticityCoefficient) {
        return builder()
                .itemView(itemView)
                .id(id)
                .quality(quality)
                .frontalArea(frontalArea)
                .mInternalPressure(mInternalPressure)
                .elasticityCoefficient(elasticityCoefficient)
                .build();
    }


    public static Builder builder() {
        return new AutoValue_MyPlanetOption.Builder().quality(DeponderHelper.DEFAULT_QUALITY_PROPERTY).mInternalPressure(DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE).speed(new PointF()).acceleration(new PointF()).frontalArea(DeponderHelper.DEFAULT_INIT_SCALE).elasticityCoefficient(DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(View itemView);

        public abstract Builder id(String id);

        abstract Builder animator(NAnimator animator);

        abstract Builder matrix(Matrix matrix);

        public abstract Builder quality(float quality);

        public abstract Builder frontalArea(float frontalArea);

        abstract View itemView();

        abstract MyPlanetOption autoBuild();

        abstract Builder speed(PointF speed);

        public abstract Builder mInternalPressure(float mInternalPressure);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        abstract Builder acceleration(PointF acceleration);

        public final MyPlanetOption build() {
            final Matrix matrix = itemView().getMatrix();
            return matrix(matrix)
                    .animator(new NAnimator(matrix))
                    .speed(new PointF())
                    .acceleration(new PointF())
                    .autoBuild();
        }
    }


}
