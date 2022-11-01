package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.PlanetOption;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

import java.util.Objects;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class SimplePlanet implements PlanetOption {


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
    @Memoized
    public Matrix speed() {
        return new Matrix();
    }

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    @Memoized
    public NAnimator animator() {
        return new NAnimator(matrix());
    }

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    @Memoized
    public Matrix matrix() {
        return itemView().getMatrix();
    }

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

        public abstract Builder quality(float quality);

        public abstract Builder frontalArea(float frontalArea);

        abstract View itemView();

        abstract String id();

        abstract SimplePlanet autoBuild();

        public abstract Builder mInternalPressure(float mInternalPressure);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        public final SimplePlanet build() {
            try {
                if (id().length() == 0)
                    throw new NullPointerException("Property \"id\" has not been set");
                return autoBuild();
            } catch (NullPointerException | IllegalStateException e) {
                return this.id(String.valueOf(itemView().hashCode())).autoBuild();
            }

        }
    }

    public abstract Builder toBuilder();

}
