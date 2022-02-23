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
import com.cinpe.deponder.control.DeponderControl;
import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRubber;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity";

    private ActivityDemoBinding mBinding;

    DeponderControl<PlanetOption, RubberOption> deponderProxy;

    List<PlanetOption> pList = new ArrayList<>();
    List<RubberOption> rList = new ArrayList<>();


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);
        mBinding.slide.addOnChangeListener((slider, value, fromUser) -> {
            if (deponderProxy != null & fromUser) deponderProxy.submitScale(value);
        });
        mBinding.setControl(this);

        deponderProxy = new Deponder<>(this, mBinding.layoutRoot);

        incubateDate();

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
     * 随便生成些数据.
     */
    private void incubateDate() {

        PlanetOption p0 = buildPo();
        PlanetOption p1 = buildPo();

        mBinding.layoutRoot.addView(p0.itemView());
        mBinding.layoutRoot.addView(p1.itemView());

        pList.add(p0);
        pList.add(p1);

        //提交pList
        deponderProxy.submitPlanet(pList);
        //提交rList
        deponderProxy.submitRubber(rList);
        //默认scale为1,可不用.
        deponderProxy.submitScale(1);

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