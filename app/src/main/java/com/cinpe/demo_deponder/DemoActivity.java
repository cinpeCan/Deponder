package com.cinpe.demo_deponder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cinpe.demo_deponder.databinding.ActivityDemoBinding;
import com.cinpe.deponder.Deponder;
import com.cinpe.deponder.DeponderHelper;
import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/*
    Copyright (c) 2022-present, Cinpecan and Deponder Contributors.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    Deponder baseUrl:https://github.com/cinpeCan/DemoDeponder

    Deponder Subcomponents:

    The following components are provided under the Apache License. See project link for details.
    The text of each license is the standard Apache 2.0 license.

    io.reactivex.rxjava3:rxandroid from https://github.com/ReactiveX/RxJava Apache-2.0 License
    com.google.guava from https://github.com/google/guava Apache-2.0 License
    com.google.auto.value from https://github.com/google/auto Apache-2.0 License
 */

/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity";

    private ActivityDemoBinding mBinding;

    DeponderControl<PlanetOption, RubberOption> deponderProxy;

    final List<PlanetOption> pList = new ArrayList<>();
    final List<RubberOption> rList = new ArrayList<>();


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null && fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

        deponderProxy = new Deponder<>(this, mBinding.layoutRoot);

        startAnimation();

    }

    @Override
    public void onClickAddPO(View view) {

        PlanetOption po = buildPo();

        mBinding.layoutRoot.addView(po.itemView());

        //add the defTouchListener,or your custom TouchListener.
        DeponderHelper.bindDefTouchPlanet(po);

        pList.add(po);

        //submit the new planetCte.
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
     * when both planetList and rubberList are submitted for the first time, the Deponder starts to run.
     * the order of submits doesn't matter
     */
    private void startAnimation() {

        //submit planetList
        deponderProxy.submitPlanet(pList);
        //submit rubberList
        deponderProxy.submitRubber(rList);

//        //def scale is 1.0f already.
//        deponderProxy.submitScale(1);

    }

    private PlanetOption buildPo() {
        return SimplePlanet
                .builder()
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.planet_demo, mBinding.layoutRoot, false).getRoot())
                .id(UUID.randomUUID().toString())
                .build();
    }

    private RubberOption buildRo(@NonNull PlanetOption s, @NonNull PlanetOption e) {
        return SimpleRubber.builder()
                .sId(s.id())
                .eId(e.id())
                .itemView(DataBindingUtil.inflate(LayoutInflater.from(mBinding.layoutRoot.getContext()), R.layout.rubber_demo, mBinding.layoutRoot, false).getRoot())
                .build();
    }

}