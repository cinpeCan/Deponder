package com.cinpe.deponder;


import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cinpe.deponder.model.SimplePlanet;
import com.cinpe.deponder.model.SimpleRubber;

/**
 * @Description: unused
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/28
 * @Version: 0.01
 */
public interface BindAdapter<P, R> {


    SimplePlanet onCreatePlanet(@NonNull ViewGroup parent, P p);

    SimpleRubber onCreateRubber(@NonNull ViewGroup parent, R r);

//    SimplePlanet onBindViewHolder(@NonNull ViewGroup parent, P p);

}
