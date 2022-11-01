package com.cinpe.deponder.model;

import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.NAnimator;
import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.RubberOption;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.BiConsumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2022/1/6
 * @Version: 0.01
 */
@AutoValue
public abstract class SimpleRubber implements RubberOption {

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract View itemView();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    @Memoized
    public String id() {
        return DeponderHelper.concatId(sId(), eId());
    }

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract String sId();

    @NonNull
    @Override
    @AutoValue.CopyAnnotations
    public abstract String eId();

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
    public abstract float elasticityCoefficient();

    @Override
    public abstract int naturalLength();

    @Override
    @Memoized
    public ImmutableList<BaseOption> vArr() {

        return Observable.just(itemView())
                .ofType(ViewGroup.class)
                .flatMap((Function<ViewGroup, ObservableSource<View>>) viewGroup -> {
                    final ImmutableList.Builder<View> builder = new ImmutableList.Builder<>();
                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                        builder.add(viewGroup.getChildAt(i));
                    }
                    return Observable.fromIterable(builder.build());
                })
                .filter(view -> {
                    if (view.getTag() instanceof String) {
                        String tag = (String) view.getTag();
                        return TextUtils.equals(tag, DeponderHelper.TAG_OF_UN_RUBBER);
                    }
                    return false;
                })
                .map((Function<View, BaseOption>) SimpleOption::create)
                .collect((Supplier<ImmutableList.Builder<BaseOption>>) ImmutableList.Builder::new, ImmutableList.Builder::add)
                .map(ImmutableList.Builder::build)
                .blockingGet();
    }

    public static SimpleRubber create(View itemView, String sId, String eId, float elasticityCoefficient, int naturalLength) {
        return builder()
                .itemView(itemView)
                .sId(sId)
                .eId(eId)
                .elasticityCoefficient(elasticityCoefficient)
                .naturalLength(naturalLength)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SimpleRubber.Builder().elasticityCoefficient(DeponderHelper.DEFAULT_RUBBER_ELASTICITY_COEFFICIENT).naturalLength(DeponderHelper.DEFAULT_RUBBER_NATURAL_LENGTH);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemView(View itemView);

        public abstract Builder sId(String id);

        public abstract Builder eId(String eId);

        public abstract Builder elasticityCoefficient(float elasticityCoefficient);

        public abstract Builder naturalLength(int naturalLength);

        abstract SimpleRubber autoBuild();

        abstract String sId();

        abstract String eId();

        public final SimpleRubber build() {
            if (TextUtils.equals(sId(), eId()))
                throw new IllegalStateException("Start and End points cannot be same");
            return autoBuild();
        }
    }

    public abstract Builder toBuilder();
}
