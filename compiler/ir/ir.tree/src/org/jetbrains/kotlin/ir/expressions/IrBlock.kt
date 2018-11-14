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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrReturnTarget
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol

interface IrContainerExpression : IrExpression, IrStatementContainer {
    val origin: IrStatementOrigin?
    val isTransparentScope: Boolean
}

interface IrBlock : IrContainerExpression {
    override val isTransparentScope: Boolean
        get() = false
}

interface IrComposite : IrContainerExpression {
    override val isTransparentScope: Boolean
        get() = true
}

interface IrReturnableBlock : IrBlock, IrSymbolOwner, IrReturnTarget {
    override val symbol: IrReturnableBlockSymbol
    val sourceFileName: String
}