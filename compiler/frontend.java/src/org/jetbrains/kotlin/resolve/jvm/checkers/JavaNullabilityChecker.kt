/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker

class JavaNullabilityChecker : AdditionalTypeChecker {

    override fun checkType(expression: KtExpression, expressionType: KotlinType, expressionTypeWithSmartCast: KotlinType, c: ResolutionContext<*>) {
        doCheckType(
                expressionType,
                c.expectedType,
                { c.dataFlowValueFactory.createDataFlowValue(expression, expressionType, c) } ,
                c.dataFlowInfo
        ) { expectedType, actualType ->
            c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(expression, expectedType, actualType))
        }

        when (expression) {
            is KtWhenExpression ->
                if (expression.elseExpression == null) {
                    // Check for conditionally-exhaustive when on platform enums, see KT-6399
                    val subjectExpression = expression.subjectExpression ?: return
                    val type = c.trace.getType(subjectExpression) ?: return
                    if (type.isFlexible() && TypeUtils.isNullableType(type.asFlexibleType().upperBound)) {
                        val enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(type) ?: return
                        val context = c.trace.bindingContext
                        if (WhenChecker.getEnumMissingCases(expression, context, enumClassDescriptor).isEmpty()
                            && !WhenChecker.containsNullCase(expression, context)) {
                            val subjectDataFlowValue = c.dataFlowValueFactory.createDataFlowValue(subjectExpression, type, c)
                            val dataFlowInfo = c.trace[BindingContext.EXPRESSION_TYPE_INFO, subjectExpression]?.dataFlowInfo
                            if (dataFlowInfo != null && !dataFlowInfo.getStableNullability(subjectDataFlowValue).canBeNull()) {
                                return
                            }

                            c.trace.report(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA.on(expression.subjectExpression!!))
                        }
                    }
                }
            is KtPostfixExpression ->
                if (expression.operationToken == KtTokens.EXCLEXCL) {
                    val baseExpression = expression.baseExpression ?: return
                    val baseExpressionType = c.trace.getType(baseExpression) ?: return
                    doIfNotNull(
                            baseExpressionType,
                            { c.dataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c) },
                            c
                    ) {
                        c.trace.report(Errors.UNNECESSARY_NOT_NULL_ASSERTION.on(expression.operationReference, baseExpressionType))
                    }
                }
            is KtBinaryExpression ->
                when (expression.operationToken) {
                    KtTokens.EQEQ,
                    KtTokens.EXCLEQ,
                    KtTokens.EQEQEQ,
                    KtTokens.EXCLEQEQEQ -> {
                        if (expression.left != null && expression.right != null) {
                            SenselessComparisonChecker.checkSenselessComparisonWithNull(
                                    expression, expression.left!!, expression.right!!, c,
                                    { c.trace.getType(it) },
                                    { value ->
                                        doIfNotNull(value.type, { value }, c) { Nullability.NOT_NULL } ?: Nullability.UNKNOWN
                                    }
                            )
                        }
                    }
                }
        }
    }

    override fun checkReceiver(receiverParameter: ReceiverParameterDescriptor, receiverArgument: ReceiverValue, safeAccess: Boolean, c: CallResolutionContext<*>) {
        val dataFlowValue by lazy(LazyThreadSafetyMode.NONE) {
            c.dataFlowValueFactory.createDataFlowValue(receiverArgument, c)
        }

        if (safeAccess) {
            val safeAccessElement = c.call.callOperationNode?.psi ?: return
            doIfNotNull(receiverArgument.type, { dataFlowValue }, c) {
                c.trace.report(Errors.UNNECESSARY_SAFE_CALL.on(safeAccessElement, receiverArgument.type))
            }

            return
        }

        doCheckType(
                receiverArgument.type,
                receiverParameter.type,
                { dataFlowValue },
                c.dataFlowInfo
        ) { expectedType,
            actualType ->
            val receiverExpression = (receiverArgument as? ExpressionReceiver)?.expression
            if (receiverExpression != null) {
                c.trace.report(ErrorsJvm.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(receiverExpression, actualType))
            } else {
                val reportOn = c.call.calleeExpression ?: c.call.callElement
                c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(reportOn, expectedType, actualType))
            }
        }
    }

    private fun doCheckType(
            expressionType: KotlinType,
            expectedType: KotlinType,
            dataFlowValue: () -> DataFlowValue,
            dataFlowInfo: DataFlowInfo,
            reportWarning: (expectedType: KotlinType, actualType: KotlinType) -> Unit
    ) {
        if (TypeUtils.noExpectedType(expectedType)) {
            return
        }

        val expectedMustNotBeNull = expectedType.mustNotBeNull() ?: return
        val actualMayBeNull = expressionType.mayBeNull() ?: return
        if (expectedMustNotBeNull.isFromKotlin && actualMayBeNull.isFromKotlin) {
            // a type mismatch error will be reported elsewhere
            return
        }

        if (dataFlowInfo.getStableNullability(dataFlowValue()) != Nullability.NOT_NULL) {
            reportWarning(expectedMustNotBeNull.enhancedType, actualMayBeNull.enhancedType)
        }
    }

    private fun <T : Any> doIfNotNull(
            type: KotlinType,
            dataFlowValue: () -> DataFlowValue,
            c: ResolutionContext<*>,
            body: () -> T
    ) = if (type.mustNotBeNull()?.isFromJava == true &&
            c.dataFlowInfo.getStableNullability(dataFlowValue()).canBeNull())
            body()
        else
            null

    private class EnhancedNullabilityInfo(val enhancedType: KotlinType, val isFromJava: Boolean) {
        val isFromKotlin get() = !isFromJava
    }

    private fun KotlinType.enhancementFromKotlin() = EnhancedNullabilityInfo(this, isFromJava = false)
    private fun TypeWithEnhancement.enhancementFromJava() = EnhancedNullabilityInfo(enhancement, isFromJava = true)

    private fun KotlinType.mustNotBeNull(): EnhancedNullabilityInfo? = when {
        !isError && !isFlexible() && !TypeUtils.acceptsNullable(this) -> enhancementFromKotlin()
        isFlexible() && !TypeUtils.acceptsNullable(asFlexibleType().upperBound) -> enhancementFromKotlin()
        this is TypeWithEnhancement && enhancement.mustNotBeNull() != null -> enhancementFromJava()
        else -> null
    }

    private fun KotlinType.mayBeNull(): EnhancedNullabilityInfo? = when {
        !isError && !isFlexible() && TypeUtils.acceptsNullable(this) -> enhancementFromKotlin()
        isFlexible() && TypeUtils.acceptsNullable(asFlexibleType().lowerBound) -> enhancementFromKotlin()
        this is TypeWithEnhancement && enhancement.mayBeNull() != null -> enhancementFromJava()
        else -> null
    }
}