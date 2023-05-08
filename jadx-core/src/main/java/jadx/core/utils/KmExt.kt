package jadx.core.utils

import jadx.core.dex.nodes.ClassNode
import jadx.core.utils.kotlin.getKotlinMetadataHolder
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.signature

inline val KmFunction.shortId: String? get() = signature?.asString()

inline val KmProperty.shortId: String? get() = fieldSignature?.asString()


inline val ClassNode.kmCls: KmClass?
	get(): KmClass? = (getKotlinMetadataHolder()?.kmm as? KotlinClassMetadata.Class)?.toKmClass()
