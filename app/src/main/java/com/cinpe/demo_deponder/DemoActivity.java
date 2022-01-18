package com.cinpe.demo_deponder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.cinpe.demo_deponder.databinding.ActivityDemoBinding;
import com.cinpe.deponder.DeponderProxy;
import com.cinpe.deponder.model.MyPlanetOption;
import com.cinpe.deponder.model.MyRubberOption;
import com.cinpe.deponder.option.PlanetOption;
import com.cinpe.deponder.option.RubberOption;

import java.util.ArrayList;
import java.util.List;


/**
 * An example activity that shows how to use DeponderProxy.
 */
public class DemoActivity extends AppCompatActivity implements DemoActivityControl {

    private static final String TAG = "DemoActivity";

    private ActivityDemoBinding mBinding;

    DeponderProxy<PlanetOption, RubberOption> deponderProxy;


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_demo);

        deponderProxy = new DeponderProxy<PlanetOption, RubberOption>(mBinding.layoutRoot) {
            @Override
            public PlanetOption functionP(View p) {
                return MyPlanetOption
                        .builder()
                        .itemView(p)
                        .build();
            }

            @Override
            public RubberOption functionR(View r) {
                return null;
//                return MyRubberOption
//                        .builder()
//                        .id()
//                        .eId()
//                        .itemView(r)
//                        .build();
            }
        };

        incubateDate();

        Log.i(TAG, "[submit]childCount:" + mBinding.layoutRoot);
    }

    @Override
    public void onClickPlanet(View view) {
        Log.d(TAG, "onClickPlanet() called with: view = [" + view + "]");
    }

    /**
     * 随便生成些数据.
     */
    private void incubateDate() {

        List<View> list = new ArrayList<>();
        list.add(mBinding.item0);
        list.add(mBinding.item1);
        list.add(mBinding.item2);

        //提交view集合.
        deponderProxy.submit(list);

    }
}