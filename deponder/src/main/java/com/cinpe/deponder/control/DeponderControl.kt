package com.cinpe.deponder.control

import androidx.annotation.FloatRange

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public interface DeponderControl<P, R> {

    public fun submitPlanet(pList: @JvmSuppressWildcards Collection<P>)

    public fun submitRubber(rList: @JvmSuppressWildcards Collection<R>)

    public fun submitScale(@FloatRange(from = 0.0, fromInclusive = false) scale: Float)

    public fun cancel()
}
