package com.cinpe.demo_deponder;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    DeponderControl<PModel, RModel> deponderProxy;

    final List<PModel> pList = new ArrayList<>();
    final List<RModel> rList = new ArrayList<>();

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null && fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

        deponderProxy = new Deponder<PModel, RModel>(this, mBinding.layoutRoot) {
            @Override
            public String planetId(PModel pModel) {
                return pModel.id;
            }

            @Override
            public Pair<String, String> rubberPairId(RModel rModel) {
                return Pair.create(rModel.sId, rModel.eId);
            }

            @Override
            public SimplePlanet createPlanet(@NonNull ViewGroup parent, PModel pModel) {
                return SimplePlanet
                        .builder()
                        .itemView(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.planet_demo, parent, false).getRoot())
                        .id(pModel.id)
                        .build();
            }

            @Override
            public SimpleRubber createRubber(@NonNull ViewGroup parent, RModel rModel) {

                return SimpleRubber.builder()
                        .sId(rModel.sId)
                        .eId(rModel.eId)
                        .itemView(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.rubber_demo, parent, false).getRoot())
                        .build();

            }

            @Override
            public SimplePlanet bindPlanet(@NonNull SimplePlanet holder, PModel pModel) {
                return holder;
            }

            @Override
            public SimpleRubber bindRubber(@NonNull SimpleRubber holder, RModel pModel) {
                return holder;
            }

        };

        startAnimation();

    }

    @Override
    public void onClickAddPO(View view) {

        PModel pModel = new PModel(UUID.randomUUID().toString());

        pList.add(pModel);

        //submit the new planetCte.
        deponderProxy.submitPlanet(pList);
    }

    @Override
    public void onClickAddRO(View view) {

        if (pList.size() < 2) {
            return;
        }

        int sIndex = new Random().nextInt(pList.size());
        int eIndex = new Random().nextInt(pList.size());
        while (sIndex == eIndex) {
            eIndex = new Random().nextInt(pList.size());
        }
        rList.add(new RModel(pList.get(sIndex).id, pList.get(eIndex).id));
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


    static class PModel {
        public PModel(String id) {
            this.id = id;
        }

        final String id;
    }

    static class RModel {
        public RModel(String sId, String eId) {
            this.sId = sId;
            this.eId = eId;
        }

        final String sId;
        final String eId;
    }

}