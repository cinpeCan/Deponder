package com.cinpe.deponder;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.MutableLiveData;

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

    private final String TAG = this.getClass().getSimpleName();

    final MutableLiveData<Collection<PO>> poClt;
    final MutableLiveData<Collection<RO>> roClt;

//    @NonNull
//    private final DiffUtil.ItemCallback<T> mDiffCallback;

    public BindAdapter(@NonNull MutableLiveData<Collection<PO>> poClt, @NonNull MutableLiveData<Collection<RO>> roClt) {

        this.poClt = poClt;
        this.roClt = roClt;
        //        mDiffCallback = callback;

    }


    /**
     * 提交P
     */
    public void submitP(@NonNull Collection<P> pList) {

    }

    /**
     * 提交R
     */
    public void submitR(@NonNull Collection<R> rList) {

    }

    public abstract @NonNull
    PO functionP(@NonNull P p, @NonNull View parent);

    public abstract @NonNull
    RO functionR(@NonNull R p, @NonNull View parent);
}
