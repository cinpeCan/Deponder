package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.RootOption;
import com.google.auto.value.AutoValue;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class SimpleRootOption extends RootOption {


    @NonNull
    @Override
    public abstract ViewGroup itemView();

    @NonNull
    @Override
    public abstract String id();

    @NonNull
    @Override
    public abstract NAnimator animator();

    @NonNull
    @Override
    public abstract Matrix matrix();

    @Override
    public abstract float initScale();

    @Override
    public abstract float maxScale();

    @Override
    public abstract float minScale();

    @Override
    public abstract float mRootDensity();

    @Override
    public abstract float mInternalPressure();

    @Override
    public abstract float elasticityCoefficient();

    public static SimpleRootOption create(ViewGroup itemView, String id, float initScale, float maxScale, float minScale, float mRootDensity, float mInternalPressure, float elasticityCoefficient) {
        return builder()
                .itemView(itemView)
                .id(id)
                .initScale(initScale)
                .maxScale(maxScale)
                .minScale(minScale)
                .mRootDensity(mRootDensity)
                .mInternalPressure(mInternalPressure)
                .elasticityCoefficient(elasticityCoefficient)

                .build();
    }

    public static Builder builder() {
        return new AutoValue_SimpleRootOption.Builder().mInternalPressure(DeponderHelper.DEFAULT_Internal_Pressure).elasticityCoefficient(DeponderHelper.DEFAULT_ROOT_ELASTICITY_COEFFICIENT);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(ViewGroup itemView);

        public abstract Builder id(String id);

        abstract Builder animator(NAnimator animator);

        abstract Builder matrix(Matrix matrix);

        public abstract Builder initScale(float initScale);

        public abstract Builder maxScale(float maxScale);

        public abstract Builder minScale(float minScale);

        public abstract Builder mRootDensity(float mRootDensity);

        public abstract Builder mInternalPressure(float mInternalPressure);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        abstract SimpleRootOption autoBuild();

        abstract ViewGroup itemView();

        abstract Matrix matrix();

        public final SimpleRootOption build() {
            return matrix(itemView().getMatrix())
                    .animator(new NAnimator(matrix()))
                    .autoBuild();
        }
    }
}
