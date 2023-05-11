package jadx.core.utils

import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.signature

inline val KmFunction.shortId: String? get() = signature?.asString()

inline val KmProperty.shortId: String? get() = fieldSignature?.asString()
