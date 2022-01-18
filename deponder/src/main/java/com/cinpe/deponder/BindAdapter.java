package com.cinpe.deponder;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.ViewDataBinding;

import com.cinpe.deponder.option.BaseOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RootOption;
import com.cinpe.deponder.option.RubberOption;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/28
 * @Version: 0.01
 */
public abstract class BindAdapter<P, R, PO extends PlanetOption, RO extends RubberOption> {

    private final String TAG;

    private final Map<String, PO> poMap;
    private final Map<String, RO> roMap;

    RootOption rootOption;

//    @NonNull
//    private final DiffUtil.ItemCallback<T> mDiffCallback;

    public BindAdapter(@NonNull RootOption rootOption) {
        this.TAG = this.getClass().getSimpleName();
        this.rootOption = rootOption;
        this.poMap = new HashMap<>();
        this.roMap = new HashMap<>();
        //        mDiffCallback = callback;

    }

//    public Map<String, PO> getPoMap() {
//        return this.poMap;
//    }
//
//    public Map<String, RO> getRoMap() {
//        return this.roMap;
//    }

    /**
     * 提交viewList.
     */
    public void submit(Collection<P> pClt, Collection<R> rClt) {

    }

    /**
     * 提交P
     */
    private void submitPlanet(@NonNull List<P> pList) {

    }

    /**
     * 提交R
     */
    private Map<String, RO> submitRubber(List<R> rList) {
        Map<String, RO> roMap = new HashMap<>();
        roMap.putAll(rList.stream().map(ro -> functionR(ro, rootOption.itemView())).collect(Collectors.<RO, String, RO>toMap(BaseOption::id, ro -> ro)));
        roMap.values().forEach(ro -> rootOption.itemView().addView(ro.itemView()));
        return roMap;
    }

    public abstract PO functionP(P p, ViewGroup parent);

    public abstract RO functionR(R p, ViewGroup parent);
}
