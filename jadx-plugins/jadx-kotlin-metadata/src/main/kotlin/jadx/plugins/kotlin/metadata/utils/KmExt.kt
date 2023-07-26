package jadx.plugins.kotlin.metadata.utils

import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.signature

inline val KmFunction.shortId: String? get() = signature?.toString()

inline val KmProperty.shortId: String? get() = fieldSignature?.toString()
