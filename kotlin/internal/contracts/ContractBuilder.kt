/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl
import kotlin.internal.InlineOnly

/**
 * This marker distinguishes the experimental contract declaration API and is used to opt-in for that feature
 * when declaring contracts of user functions.
 *
 * Any usage of a declaration annotated with `@ExperimentalContracts` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalContracts::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.contracts.ExperimentalContracts`.
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@Experimental
public annotation class ExperimentalContracts

/**
 * Provides a scope, where the functions of the contract DSL, such as [returns], [callsInPlace], etc.,
 * can be used to describe the contract of a function.
 *
 * This type is used as a receiver type of the lambda function passed to the [contract] function.
 *
 * @see contract
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ContractBuilder {
    /**
     * Describes a situation when a function returns normally, without any exceptions thrown.
     * 描述函数正常返回(无返回值)但没有抛出任何异常的情况。
     *
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     * 使用[SimpleEffect.Implies]函数来描述在这种情况下发生的条件效果。
     *
     */
    // @sample samples.contracts.returnsContract
    @ContractsDsl public fun returns(): Returns

    /**
     * Describes a situation when a function returns normally with the specified return [value].
     * 描述函数以指定的return [value]正常返回的情况。
     * The possible values of [value] are limited to `true`, `false` or `null`.
     *
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     *
     */
    // @sample samples.contracts.returnsTrueContract
    // @sample samples.contracts.returnsFalseContract
    // @sample samples.contracts.returnsNullContract
    @ContractsDsl public fun returns(value: Any?): Returns

    /**
     * 描述函数正常返回任何非“null”值的情况。
     * Describes a situation when a function returns normally with any value that is not `null`.
     *
     * 使用[SimpleEffect.Implies]函数来描述在这种情况下发生的条件效果。
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     *
     */
    // @sample samples.contracts.returnsNotNullContract
    @ContractsDsl public fun returnsNotNull(): ReturnsNotNull

    /**
     * 指定在适当的位置调用函数参数[lambda]。
     * Specifies that the function parameter [lambda] is invoked in place.
     *
     * This contract specifies that:
     * 1. the function [lambda] can only be invoked during the call of the owner function,
     *  and it won't be invoked after that owner function call is completed;
     * 2. _(optionally)_ the function [lambda] is invoked the amount of times specified by the [kind] parameter,
     *  see the [InvocationKind] enum for possible values.
     *
     * A function declaring the `callsInPlace` effect must be _inline_.
     * 
     * 该合同规定：
     * 1.函数[lambda]只能在所有者函数调用期间调用，
     *   并且在完成所有者函数调用后不会调用它;
     * 2. _（可选）_函数[lambda]被调用[kind]参数指定的次数，
     *   请参阅[InvocationKind]枚举以获取可能的值。
     *                                                                                                            
     * 声明`callsInPlace`效果的函数必须是_inline_。
     *
     */
    /* @sample samples.contracts.callsInPlaceAtMostOnceContract
    * @sample samples.contracts.callsInPlaceAtLeastOnceContract
    * @sample samples.contracts.callsInPlaceExactlyOnceContract
    * @sample samples.contracts.callsInPlaceUnknownContract
    */
    @ContractsDsl public fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace
}

/**
 * Specifies how many times a function invokes its function parameter in place.
 *
 * See [ContractBuilder.callsInPlace] for the details of the call-in-place function contract.
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public enum class InvocationKind {
    /**
     * A function parameter will be invoked one time or not invoked at all.
     * 函数参数将被调用一次或根本不被调用。
     */
    // @sample samples.contracts.callsInPlaceAtMostOnceContract
    @ContractsDsl AT_MOST_ONCE,

    /**
     * A function parameter will be invoked one or more times.
     * 函数参数将被调用一次或多次。
     *
     */
    // @sample samples.contracts.callsInPlaceAtLeastOnceContract
    @ContractsDsl AT_LEAST_ONCE,

    /**
     * A function parameter will be invoked exactly one time.
     * 函数参数将被调用一次。
     *
     */
    // @sample samples.contracts.callsInPlaceExactlyOnceContract
    @ContractsDsl EXACTLY_ONCE,

    /**
     * A function parameter is called in place, but it's unknown how many times it can be called.
     * 函数参数就地调用，但不知道可以调用多少次。
     *
     */
    // @sample samples.contracts.callsInPlaceUnknownContract
    @ContractsDsl UNKNOWN
}

/**
 * Specifies the contact of a function.
 * 指定一个带有合同的函数
 *
 * The contract description must be at the beginning of a function and have at least one effect.
 * 合同描述必须位于函数的开头，并且至少有一个效果。
 *
 * Only the top-level functions can have a contract for now.
 * 目前只有顶级函数才能签订合同。
 *
 * @param builder the lambda where the contract of a function is described with the help of the [ContractBuilder] members.
 * 构建函数lambda，其中函数的契约在[ContractBuilder]成员的帮助下描述。
 *
 */
/* @sample samples.contracts.returnsContract
* @sample samples.contracts.returnsTrueContract
* @sample samples.contracts.returnsFalseContract
* @sample samples.contracts.returnsNullContract
* @sample samples.contracts.returnsNotNullContract
* @sample samples.contracts.callsInPlaceAtMostOnceContract
* @sample samples.contracts.callsInPlaceAtLeastOnceContract
* @sample samples.contracts.callsInPlaceExactlyOnceContract
* @sample samples.contracts.callsInPlaceUnknownContract
*/
@ContractsDsl
@ExperimentalContracts
@InlineOnly
@SinceKotlin("1.3")
@Suppress("UNUSED_PARAMETER")
public inline fun contract(builder: ContractBuilder.() -> Unit) { }