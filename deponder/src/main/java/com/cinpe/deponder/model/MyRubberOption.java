package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.RubberOption;
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
public abstract class MyRubberOption extends RubberOption {

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
    public abstract String eId();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract NAnimator animator();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract Matrix matrix();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract RectF rectF();

    @Override
    public abstract float elasticityCoefficient();

    public static MyRubberOption create(View itemView, String id, String eId, float elasticityCoefficient) {
        return builder()
                .itemView(itemView)
                .id(id)
                .eId(eId)
                .elasticityCoefficient(elasticityCoefficient)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MyRubberOption.Builder().elasticityCoefficient(DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(View itemView);

        public abstract Builder id(String id);

        public abstract Builder eId(String eId);

        abstract Builder animator(NAnimator animator);

        abstract Builder matrix(Matrix matrix);

        abstract Builder rectF(RectF rectF);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        abstract MyRubberOption autoBuild();

        abstract View itemView();

        abstract Matrix matrix();

        public final MyRubberOption build() {
            final Rect rect = new Rect();
            itemView().getHitRect(rect);
            final RectF rectF = new RectF(rect);
            return matrix(itemView().getMatrix())
                    .animator(new NAnimator(matrix()))
                    .rectF(rectF)
                    .autoBuild();
        }
    }
}
