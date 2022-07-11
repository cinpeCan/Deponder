package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
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
public abstract class SimpleRootOption extends com.cinpe.deponder.option.SimpleRootOption {


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
    public abstract float elasticityCoefficientStart();

    public abstract float elasticityCoefficientTop();

    public abstract float elasticityCoefficientEnd();

    public abstract float elasticityCoefficientBot();

    public static SimpleRootOption create(ViewGroup itemView, String id, float initScale, float maxScale, float minScale, float mRootDensity, float mInternalPressure, float elasticityCoefficientStart, float elasticityCoefficientTop, float elasticityCoefficientEnd, float elasticityCoefficientBot) {
        return builder()
                .itemView(itemView)
                .id(id)
                .initScale(initScale)
                .maxScale(maxScale)
                .minScale(minScale)
                .mRootDensity(mRootDensity)
                .mInternalPressure(mInternalPressure)
                .elasticityCoefficientStart(elasticityCoefficientStart)
                .elasticityCoefficientTop(elasticityCoefficientTop)
                .elasticityCoefficientEnd(elasticityCoefficientEnd)
                .elasticityCoefficientBot(elasticityCoefficientBot)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SimpleRootOption.Builder().mInternalPressure(DeponderHelper.DEFAULT_Internal_Pressure)
                .elasticityCoefficientStart(DeponderHelper.DEFAULT_Internal_Pressure_START)
                .elasticityCoefficientTop(DeponderHelper.DEFAULT_Internal_Pressure_TOP)
                .elasticityCoefficientEnd(DeponderHelper.DEFAULT_Internal_Pressure_END)
                .elasticityCoefficientBot(DeponderHelper.DEFAULT_Internal_Pressure_BOTTOM)
                .initScale(DeponderHelper.DEFAULT_INIT_SCALE).maxScale(DeponderHelper.DEFAULT_MAX_SCALE).minScale(DeponderHelper.DEFAULT_MIN_SCALE).mRootDensity(DeponderHelper.DEFAULT_ROOT_DENSITY).id(UUID.randomUUID().toString());
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

        public abstract Builder elasticityCoefficientStart(float elasticityCoefficientStart);

        public abstract Builder elasticityCoefficientTop(float elasticityCoefficientTop);

        public abstract Builder elasticityCoefficientEnd(float elasticityCoefficientEnd);

        public abstract Builder elasticityCoefficientBot(float elasticityCoefficientBot);

        abstract SimpleRootOption autoBuild();

        abstract ViewGroup itemView();

        abstract Matrix matrix();

        public final SimpleRootOption build() {
            return matrix(itemView().getMatrix())
                    .animator(new NAnimator(matrix()))
                    .id(String.valueOf(itemView().hashCode()))
                    .autoBuild();
        }
    }
}
