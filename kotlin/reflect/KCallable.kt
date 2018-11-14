/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a callable entity, such as a function or a property.
 * 表示可调用的实体，例如函数或属性。
 *
 * @param R return type of the callable.
 */
public interface KCallable<out R> : KAnnotatedElement {
    /**
     * The name of this callable as it was declared in the source code.
     * 在源代码中声明的可调用名称。
     * If the callable has no name, a special invented name is created.
     * 如果callable没有名称，则会创建一个特殊的发明名称。
     * Nameless callables include:（无名称包括以下情况）
     * - constructors have the name "<init>",构造函数的名称<init>
     * - property accessors: the getter for a property named "foo" will have the name "<get-foo>",
     *   the setter, similarly, will have the name "<set-foo>".
     *   属性访问器:对于一个命名为foo的字段的getter名称为<get-foot>，setter名称为<set-foo>
     */
    public val name: String

    /**
     * 调用对象所需的参数。
     * Parameters required to make a call to this callable.
     * 如果这个callable需要`this`实例或扩展接收器参数，
     * If this callable requires a `this` instance or an extension receiver parameter,
     * 他们按顺序排在第一位。
     * they come first in the list in that order.
     */
    public val parameters: List<KParameter>

    /**
     * The type of values returned by this callable.
     * 调用返回的值的类型。
     */
    public val returnType: KType

    /**
     * 返回参数列表
     * The list of type parameters of this callable.
     */
    @SinceKotlin("1.1")
    public val typeParameters: List<KTypeParameter>

    /**
     * 使用指定的参数列表调用此callable并返回结果。
     * Calls this callable with the specified list of arguments and returns the result.
     * 如果指定参数的数量不等于[parameters]的大小或者如果它们的类型与参数的类型不匹配。，则抛出异常
     * Throws an exception if the number of specified arguments is not equal to the size of [parameters],
     * or if their types do not match the types of the parameters.
     */
    public fun call(vararg args: Any?): R

    /**
     * Calls this callable with the specified mapping of parameters to arguments and returns the result.
     * If a parameter is not found in the mapping and is not optional (as per [KParameter.isOptional]),
     * or its type does not match the type of the provided value, an exception is thrown.
     */
    public fun callBy(args: Map<KParameter, Any?>): R

    /**
     * Visibility of this callable, or `null` if its visibility cannot be represented in Kotlin.
     */
    @SinceKotlin("1.1")
    public val visibility: KVisibility?

    /**
     * `true` if this callable is `final`.
     * 判断是否是final
     */
    @SinceKotlin("1.1")
    public val isFinal: Boolean

    /**
     * `true` if this callable is `open`.
     * 判断是否是open
     */
    @SinceKotlin("1.1")
    public val isOpen: Boolean

    /**
     * `true` if this callable is `abstract`.
     *  判断是否是abstract
     */
    @SinceKotlin("1.1")
    public val isAbstract: Boolean
}
