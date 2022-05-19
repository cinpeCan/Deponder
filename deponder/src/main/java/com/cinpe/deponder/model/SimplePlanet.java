package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.PlanetOption;
import com.google.auto.value.AutoValue;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class SimplePlanet extends PlanetOption {


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
    public abstract Matrix speed();

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
    @Deprecated
    public abstract float frontalArea();

    @Override
    public abstract float mInternalPressure();

    @Override
    public abstract float elasticityCoefficient();

    public static SimplePlanet create(View itemView, String id, float quality, float frontalArea, float mInternalPressure, float elasticityCoefficient) {
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
        return new AutoValue_SimplePlanet.Builder().quality(DeponderHelper.DEFAULT_QUALITY_PROPERTY).mInternalPressure(DeponderHelper.DEFAULT_PLANET_INTERNAL_PRESSURE).frontalArea(DeponderHelper.DEFAULT_INIT_SCALE).elasticityCoefficient(DeponderHelper.DEFAULT_ELASTICITY_COEFFICIENT);
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

        abstract Matrix matrix();

        abstract SimplePlanet autoBuild();

        abstract Builder speed(Matrix speed);

        public abstract Builder mInternalPressure(float mInternalPressure);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        public final SimplePlanet build() {
            return matrix(itemView().getMatrix())
                    .animator(new NAnimator(matrix()))
                    .speed(new Matrix())
                    .autoBuild();
        }
    }


}
