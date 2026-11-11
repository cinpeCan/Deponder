@file:JvmName("DeponderDsl")

package com.cinpe.deponder

import android.util.Pair
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.cinpe.deponder.control.DeponderControl
import com.cinpe.deponder.model.SimpleOption
import com.cinpe.deponder.model.SimplePlanet
import com.cinpe.deponder.model.SimpleRoot
import com.cinpe.deponder.model.SimpleRubber
import com.cinpe.deponder.option.SimpleRootOption

/**
 * DSL scope marker preventing accidental access to outer receivers when configuring Deponder options.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class DeponderDslMarker

/**
 * Builds a [SimplePlanet] using Kotlin property syntax.
 */
@JvmSynthetic
public inline fun simplePlanet(block: SimplePlanet.Builder.() -> Unit): SimplePlanet =
    SimplePlanet.builder().apply(block).build()

/**
 * Copies this [SimplePlanet] applying modifications in [block].
 */
@JvmSynthetic
public inline fun SimplePlanet.copy(block: SimplePlanet.Builder.() -> Unit): SimplePlanet =
    toBuilder().apply(block).build()

/**
 * Builds a [SimpleRubber] using Kotlin property syntax.
 */
@JvmSynthetic
public inline fun simpleRubber(block: SimpleRubber.Builder.() -> Unit): SimpleRubber =
    SimpleRubber.builder().apply(block).build()

/**
 * Copies this [SimpleRubber] applying modifications in [block].
 */
@JvmSynthetic
public inline fun SimpleRubber.copy(block: SimpleRubber.Builder.() -> Unit): SimpleRubber =
    toBuilder().apply(block).build()

/**
 * Builds a [SimpleRoot] using Kotlin property syntax.
 */
@JvmSynthetic
public inline fun simpleRoot(block: SimpleRoot.Builder.() -> Unit): SimpleRoot =
    SimpleRoot.builder().apply(block).build()

/**
 * Builds a [SimpleOption] using Kotlin property syntax.
 */
@JvmSynthetic
public inline fun simpleOption(block: SimpleOption.Builder.() -> Unit): SimpleOption =
    SimpleOption.builder().apply(block).build()

/**
 * Lambda-based configuration scope for creating a [Deponder] controller without subclassing [Deponder].
 */
@DeponderDslMarker
public class DeponderBuilder<P, R> internal constructor() {
    private var planetIdFn: ((P) -> String)? = null
    private var rubberPairIdFn: ((R) -> Pair<String, String>)? = null
    private var createPlanetFn: ((ViewGroup, P) -> SimplePlanet)? = null
    private var createRubberFn: ((ViewGroup, R) -> SimpleRubber)? = null
    private var bindPlanetFn: ((SimplePlanet, P) -> SimplePlanet)? = null
    private var bindRubberFn: ((SimpleRubber, R) -> SimpleRubber)? = null
    private var typePlanetFn: ((P) -> Int)? = null
    private var typeRubberFn: ((R) -> Int)? = null
    private var samePlanetFn: ((P & Any, P & Any) -> Boolean)? = null
    private var sameRubberFn: ((R & Any, R & Any) -> Boolean)? = null

    public fun planetId(block: (P) -> String) {
        planetIdFn = block
    }

    public fun rubberPairId(block: (R) -> Pair<String, String>) {
        rubberPairIdFn = block
    }

    public fun createPlanet(block: (parent: ViewGroup, item: P) -> SimplePlanet) {
        createPlanetFn = block
    }

    public fun createRubber(block: (parent: ViewGroup, item: R) -> SimpleRubber) {
        createRubberFn = block
    }

    public fun bindPlanet(block: (holder: SimplePlanet, item: P) -> SimplePlanet) {
        bindPlanetFn = block
    }

    public fun bindRubber(block: (holder: SimpleRubber, item: R) -> SimpleRubber) {
        bindRubberFn = block
    }

    public fun typePlanet(block: (P) -> Int) {
        typePlanetFn = block
    }

    public fun typeRubber(block: (R) -> Int) {
        typeRubberFn = block
    }

    public fun areContentsTheSameByP(block: (oldItem: P & Any, newItem: P & Any) -> Boolean) {
        samePlanetFn = block
    }

    public fun areContentsTheSameByR(block: (oldItem: R & Any, newItem: R & Any) -> Boolean) {
        sameRubberFn = block
    }

    internal fun build(owner: LifecycleOwner, rootOption: SimpleRootOption): DeponderControl<P, R> {
        val pId = requireNotNull(planetIdFn) { "planetId { ... } must be provided in deponder { ... }" }
        val rPairId = requireNotNull(rubberPairIdFn) { "rubberPairId { ... } must be provided in deponder { ... }" }
        val cPlanet = requireNotNull(createPlanetFn) { "createPlanet { ... } must be provided in deponder { ... }" }
        val cRubber = requireNotNull(createRubberFn) { "createRubber { ... } must be provided in deponder { ... }" }
        val bPlanet = bindPlanetFn
        val bRubber = bindRubberFn
        val tPlanet = typePlanetFn
        val tRubber = typeRubberFn
        val sPlanet = samePlanetFn
        val sRubber = sameRubberFn

        return object : Deponder<P, R>(owner, rootOption) {
            override fun planetId(p: P): String = pId(p)

            override fun rubberPairId(r: R): Pair<String, String> = rPairId(r)

            override fun createPlanet(parent: ViewGroup, p: P): SimplePlanet = cPlanet(parent, p)

            override fun createRubber(parent: ViewGroup, r: R): SimpleRubber = cRubber(parent, r)

            override fun bindPlanet(holder: SimplePlanet, p: P): SimplePlanet =
                bPlanet?.invoke(holder, p) ?: super.bindPlanet(holder, p)

            override fun bindRubber(holder: SimpleRubber, r: R): SimpleRubber =
                bRubber?.invoke(holder, r) ?: super.bindRubber(holder, r)

            override fun typePlanet(p: P): Int = tPlanet?.invoke(p) ?: super.typePlanet(p)

            override fun typeRubber(r: R): Int = tRubber?.invoke(r) ?: super.typeRubber(r)

            override fun areContentsTheSameByP(oldItem: P & Any, newItem: P & Any): Boolean =
                sPlanet?.invoke(oldItem, newItem) ?: (oldItem == newItem)

            override fun areContentsTheSameByR(oldItem: R & Any, newItem: R & Any): Boolean =
                sRubber?.invoke(oldItem, newItem) ?: (oldItem == newItem)
        }
    }
}

/**
 * Creates a [DeponderControl] configured with Kotlin lambdas using default [SimpleRoot] environment settings.
 */
@JvmSynthetic
public fun <P, R> deponder(
    owner: LifecycleOwner,
    rootView: ViewGroup,
    block: DeponderBuilder<P, R>.() -> Unit,
): DeponderControl<P, R> =
    deponder(
        owner = owner,
        rootOption = SimpleRoot.builder().itemView(rootView).build(),
        block = block,
    )

/**
 * Creates a [DeponderControl] configured with Kotlin lambdas and a custom [SimpleRootOption].
 */
@JvmSynthetic
public fun <P, R> deponder(
    owner: LifecycleOwner,
    rootOption: SimpleRootOption,
    block: DeponderBuilder<P, R>.() -> Unit,
): DeponderControl<P, R> =
    DeponderBuilder<P, R>().apply(block).build(owner, rootOption)
