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
import com.cinpe.demo_deponder.databinding.RubberDemoBinding;
import com.cinpe.deponder.Deponder;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;


/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity2";

    private ActivityDemoBinding mBinding;

    Deponder<PlanetOption, RubberOption> deponderProxy;

//    PlanetOption item0;
//    PlanetOption item1;
//    PlanetOption item2;

    List<PlanetOption> pList = new ArrayList<>();
    List<RubberOption> rList = new ArrayList<>();

//    RubberOption ro0to1;


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null & fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

//        item0 = functionP(mBinding.item0);
//        item1 = functionP(mBinding.item1);
//        item2 = functionP(mBinding.item2);
//        ro0to1 = functionR(item0, item1);

        deponderProxy = new Deponder<PlanetOption, RubberOption>(this, mBinding.layoutRoot);

        incubateDate2();

        Log.i(TAG, "[submit]childCount:" + mBinding.layoutRoot);
    }

    @Override
    public void onClickAddPO(View view) {

        PlanetOption po = buildPo();
        mBinding.layoutRoot.addView(po.itemView());
        pList.add(po);

        //提交view集合.
        deponderProxy.submitPlanet(pList);
    }

    @Override
    public void onClickAddRO(View view) {

        int sIndex = new Random().nextInt(pList.size());
        int eIndex = new Random().nextInt(pList.size());
        while (sIndex == eIndex) {
            eIndex = new Random().nextInt(pList.size());
        }

        rList.add(buildRo(pList.get(sIndex), pList.get(eIndex)));
        deponderProxy.submitRubber(rList);
    }


    /**
     * 随便生成些数据2.
     */
    private void incubateDate2() {

        PlanetOption p0 = buildPo();
        PlanetOption p1 = buildPo();

        mBinding.layoutRoot.addView(p0.itemView());
        mBinding.layoutRoot.addView(p1.itemView());

        pList.add(p0);
        pList.add(p1);

        //提交view集合.
        deponderProxy.submit(pList, rList, 1f);

    }

    private PlanetOption buildPo() {
        return MyPlanetOption
                .builder()
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.planet_demo, mBinding.layoutRoot, false).getRoot())
                .id(UUID.randomUUID().toString())
                .build();
    }

    private RubberOption buildRo(@NonNull PlanetOption s, @NonNull PlanetOption e) {
        return MyRubberOption.builder()
                .sId(s.id())
                .eId(e.id())
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.rubber_demo, mBinding.layoutRoot, false).getRoot())
                .build();
    }
}