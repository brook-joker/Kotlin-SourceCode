/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl

/**
 * Represents an effect of a function invocation,
 * 表示函数的调用效果
 * either directly observable, such as the function returning normally,
 * 要么是直接可以观察的，例如正常返回的函数
 * or a side-effect, such as the function's lambda parameter being called in place.
 * 要么可能是间接作用的，例如函数的lambda参数被调用
 * The inheritors are used in [ContractBuilder] to describe the contract of a function.
 *
 * @see ConditionalEffect
 * @see SimpleEffect
 * @see CallsInPlace
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Effect

/**
 * An effect of some condition being true after observing another effect of a function.
 * 观察函数的另一个影响之后，某一些条件的效果为真
 *
 * This effect is specified in the `contract { }` block by attaching a boolean expression
 * to another [SimpleEffect] effect with the function [SimpleEffect.implies].
 * 通过使用函数[SimpleEffect.implies]将布尔表达式附加到另一个[SimpleEffect]效果，
 * 可以在`contract {}`块中指定此效果。
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ConditionalEffect : Effect

/**
 * An effect that can be observed after a function invocation.
 * 在函数调用之后可以观察到的效果。
 *
 * @see ContractBuilder.returns
 * @see ContractBuilder.returnsNotNull
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface SimpleEffect : Effect {
    /**
     * 指定此效果在观察时保证[booleanExpression]为true。
     * Specifies that this effect, when observed, guarantees [booleanExpression] to be true.
     *
     * Note: [booleanExpression] can accept only a subset of boolean expressions,
     * where a function parameter or receiver (`this`) undergoes
     * - true of false checks, in case if the parameter or receiver is `Boolean`;
     * - null-checks (`== null`, `!= null`);
     * - instance-checks (`is`, `!is`);
     * - a combination of the above with the help of logic operators (`&&`, `||`, `!`).
     * 注意：[booleanExpression]只能接受布尔表达式的子集，
     * 函数参数或接收器（`this`）经历的地方
     *  - 如果参数或接收器为“布尔”，则为假检查;
     *  -  null-checks（`== null`，`！= null`）;
     *  - 实例检查（`是`，`！是`）;
     *  - 上面的逻辑运算符（`&&`，`||`，`！`）的组合。
     */
    @ContractsDsl
    @ExperimentalContracts
    public infix fun implies(booleanExpression: Boolean): ConditionalEffect
}

/**
 * Describes a situation when a function returns normally with a given return value.
 * 描述函数正常返回给定返回值的情况。
 * @see ContractBuilder.returns
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Returns : SimpleEffect

/**
 * Describes a situation when a function returns normally with any non-null return value.
 * 描述函数正常返回任何非null返回值的情况。
 * @see ContractBuilder.returnsNotNull
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ReturnsNotNull : SimpleEffect

/**
 * An effect of calling a functional parameter in place.
 * 调用功能参数的效果。
 *
 * A function is said to call its functional parameter in place, if the functional parameter is only invoked
 * while the execution has not been returned from the function, and the functional parameter cannot be
 * invoked after the function is completed.
 * 表示调用函数式参数(lambda表达式参数)的效果，并且函数式参数(lambda表达式参数)只能在自己函数被调用期间被调用，
 * 当自己函数被调用结束后，函数式参数(lambda表达式参数)不能被执行.
 *
 * @see ContractBuilder.callsInPlace
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface CallsInPlace : Effect