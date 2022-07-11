package com.cinpe.deponder.control;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import java.util.Collection;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public interface DeponderControl<P, R> {

    void submitPlanet(@NonNull Collection<P> pList);

    void submitRubber(@NonNull Collection<R> rList);

    void submitScale(@FloatRange(from = 0, fromInclusive = false) float scale);

    void cancel();

}
