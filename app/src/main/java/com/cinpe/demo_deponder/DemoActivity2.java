package com.cinpe.demo_deponder;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cinpe.demo_deponder.databinding.ActivityDemoBinding;
import com.cinpe.deponder.Deponder;
import com.cinpe.deponder.DeponderProxy;
import com.cinpe.deponder.model.MyPlanetOption;
import com.cinpe.deponder.model.MyRubberOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;
import com.google.android.material.slider.Slider;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding4.slidingpanelayout.RxSlidingPaneLayout;
import com.jakewharton.rxbinding4.widget.RxSeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity2 extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity2";

    private ActivityDemoBinding mBinding;

    Deponder<PlanetOption, RubberOption> deponderProxy;

    PlanetOption item0;
    PlanetOption item1;
    PlanetOption item2;

    RubberOption ro0to1;


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null & fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

        item0 = functionP(mBinding.item0);
        item1 = functionP(mBinding.item1);
        item2 = functionP(mBinding.item2);
        ro0to1 = functionR(item0, item1);

        deponderProxy = new Deponder<PlanetOption, RubberOption>(this, mBinding.layoutRoot);

        incubateDate2();

        Log.i(TAG, "[submit]childCount:" + mBinding.layoutRoot);
    }

    @Override
    public void onClickPlanet(View view) {
        Log.d(TAG, "onClickPlanet() called with: view = [" + view + "]");
    }

    @Override
    public void onClickAddPO(View view) {
        deponderProxy.submit(ImmutableList.of(item0, item1, item2), ImmutableList.of(functionR(item0, item1)), 1f);
    }

    @Override
    public void onClickAddRO(View view) {

        deponderProxy.submit(ImmutableList.of(item0, item1, item2), ImmutableList.of(functionR(item0, item1), functionR(item1, item2)), 1f);

    }


    /**
     * 随便生成些数据2.
     */
    private void incubateDate2() {

        //提交view集合.
        deponderProxy.submit(ImmutableList.of(item0, item1), ImmutableList.of(ro0to1), 1f);

    }

    private PlanetOption functionP(@NonNull View p) {
        return MyPlanetOption
                .builder()
                .itemView(p)
                .id(String.valueOf(p.hashCode()))
                .build();
    }

    private RubberOption functionR(PlanetOption s, PlanetOption e) {
        return MyRubberOption.builder()
                .id(s.id())
                .eId(e.id())
                .naturalLength(300)
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.rubber_demo, mBinding.layoutRoot, false).getRoot())
                .build();
    }
}