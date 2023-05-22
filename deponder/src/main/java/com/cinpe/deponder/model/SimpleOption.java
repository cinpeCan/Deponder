package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.BaseOption;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

@AutoValue
public abstract class SimpleOption implements BaseOption {


    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract View itemView();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    @Memoized
    public String id() {
        return String.valueOf(itemView().hashCode());
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

    public static SimpleOption create(View itemView) {
        return builder()
                .itemView(itemView)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SimpleOption.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(View itemView);
ListView
        public abstract SimpleOption build();
    }
}
