/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.serialization.deserialization

import kotlin.reflect.jvm.internal.impl.protobuf.GeneratedMessageLite

fun <M : GeneratedMessageLite.ExtendableMessage<M>, T> GeneratedMessageLite.ExtendableMessage<M>.getExtensionOrNull(
    extension: GeneratedMessageLite.GeneratedExtension<M, T>
): T? = if (hasExtension(extension)) getExtension(extension) else null
