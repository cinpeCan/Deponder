package com.cinpe.deponder

import android.util.Pair
import android.view.ViewGroup
import com.cinpe.deponder.model.SimplePlanet
import com.cinpe.deponder.model.SimpleRubber

/**
 * @Description: unused
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/28
 * @Version: 0.01
 */
public interface BindAdapter<P, R> {

    public fun planetId(p: P): String

    public fun rubberPairId(r: R): Pair<String, String>

    public fun rubberId(r: R): String = DeponderHelper.concatId(rubberPairId(r))

    public fun typePlanet(p: P): Int = 0

    public fun typeRubber(r: R): Int = 0

    public fun createPlanet(parent: ViewGroup, p: P): SimplePlanet

    public fun createRubber(parent: ViewGroup, r: R): SimpleRubber

    public fun bindPlanet(holder: SimplePlanet, p: P): SimplePlanet =
        holder.toBuilder().id(planetId(p)).build()

    public fun bindRubber(holder: SimpleRubber, r: R): SimpleRubber {
        val pair = rubberPairId(r)
        return holder.toBuilder().sId(pair.first).eId(pair.second).build()
    }

    public fun areContentsTheSameByP(oldItem: P & Any, newItem: P & Any): Boolean =
        oldItem == newItem

    public fun areContentsTheSameByR(oldItem: R & Any, newItem: R & Any): Boolean =
        oldItem == newItem
}
