package com.cinpe.deponder;


import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRubber;

import java.util.Objects;

/**
 * @Description: unused
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/28
 * @Version: 0.01
 */
public interface BindAdapter<P, R> {

    String planetId(P p);

    Pair<String, String> rubberPairId(R r);

    default String rubberId(R r) {
        return DeponderHelper.concatId(rubberPairId(r));
    }

    default int typePlanet(P p) {
        return 0;
    }

    default int typeRubber(R r) {
        return 0;
    }

    SimplePlanet createPlanet(@NonNull ViewGroup parent, P p);

    SimpleRubber createRubber(@NonNull ViewGroup parent, R r);

    SimplePlanet bindPlanet(@NonNull SimplePlanet holder, P p);

    SimpleRubber bindRubber(@NonNull SimpleRubber holder, R p);

    default boolean areContentsTheSameByP(@NonNull P oldItem, @NonNull P newItem) {
        return Objects.equals(oldItem, newItem);
    }

    default boolean areContentsTheSameByR(@NonNull R oldItem, @NonNull R newItem) {
        return Objects.equals(oldItem, newItem);
    }

}
