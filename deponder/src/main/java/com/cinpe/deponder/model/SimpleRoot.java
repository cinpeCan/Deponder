package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.SimpleRootOption;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

import java.util.UUID;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class SimpleRoot implements SimpleRootOption {


    @NonNull
    @Override
    public abstract ViewGroup itemView();

    @NonNull
    @Override
    @Memoized
    public String id() {
        return String.valueOf(itemView().hashCode());
    }

    @NonNull
    @Override
    @Memoized
    public NAnimator animator() {
        return new NAnimator(matrix());
    }

    @NonNull
    @Override
    @Memoized
    public Matrix matrix() {
        return itemView().getMatrix();
    }

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

    public static SimpleRoot create(ViewGroup itemView, float initScale, float maxScale, float minScale, float mRootDensity, float mInternalPressure, float elasticityCoefficientStart, float elasticityCoefficientTop, float elasticityCoefficientEnd, float elasticityCoefficientBot) {
        return builder()
                .itemView(itemView)
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
        return new AutoValue_SimpleRoot.Builder().mInternalPressure(DeponderHelper.DEFAULT_Internal_Pressure)
                .elasticityCoefficientStart(DeponderHelper.DEFAULT_Internal_Pressure_START)
                .elasticityCoefficientTop(DeponderHelper.DEFAULT_Internal_Pressure_TOP)
                .elasticityCoefficientEnd(DeponderHelper.DEFAULT_Internal_Pressure_END)
                .elasticityCoefficientBot(DeponderHelper.DEFAULT_Internal_Pressure_BOTTOM)
                .initScale(DeponderHelper.DEFAULT_INIT_SCALE).maxScale(DeponderHelper.DEFAULT_MAX_SCALE).minScale(DeponderHelper.DEFAULT_MIN_SCALE).mRootDensity(DeponderHelper.DEFAULT_ROOT_DENSITY);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(ViewGroup itemView);

        public abstract Builder initScale(float initScale);

        public abstract Builder maxScale(float maxScale);

        public abstract Builder minScale(float minScale);

        public abstract Builder mRootDensity(float mRootDensity);

        public abstract Builder mInternalPressure(float mInternalPressure);

        public abstract Builder elasticityCoefficientStart(float elasticityCoefficientStart);

        public abstract Builder elasticityCoefficientTop(float elasticityCoefficientTop);

        public abstract Builder elasticityCoefficientEnd(float elasticityCoefficientEnd);

        public abstract Builder elasticityCoefficientBot(float elasticityCoefficientBot);

        abstract SimpleRoot autoBuild();

        public final SimpleRoot build() {
            return autoBuild();
        }
    }
}
