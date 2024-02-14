@file:Suppress("UNCHECKED_CAST")

package jadx.plugins.kotlin.metadata.utils

import jadx.api.plugins.input.data.annotations.EncodedType
import jadx.api.plugins.input.data.annotations.EncodedValue
import jadx.api.plugins.input.data.annotations.IAnnotation
import jadx.core.dex.nodes.ClassNode
import jadx.plugins.kotlin.metadata.model.KotlinMetadataConsts
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata

fun ClassNode.getMetadata(): Metadata? {
	val annotation: IAnnotation? = getAnnotation(KotlinMetadataConsts.KOTLIN_METADATA_ANNOTATION)

	return annotation?.run {
		val k = getParamAsInt(KotlinMetadataConsts.KOTLIN_METADATA_K_PARAMETER)
		val mvArray = getParamAsIntArray(KotlinMetadataConsts.KOTLIN_METADATA_MV_PARAMETER)
		val d1Array = getParamAsStringArray(KotlinMetadataConsts.KOTLIN_METADATA_D1_PARAMETER)
		val d2Array = getParamAsStringArray(KotlinMetadataConsts.KOTLIN_METADATA_D2_PARAMETER)
		val xs = getParamAsString(KotlinMetadataConsts.KOTLIN_METADATA_XS_PARAMETER)
		val pn = getParamAsString(KotlinMetadataConsts.KOTLIN_METADATA_PN_PARAMETER)
		val xi = getParamAsInt(KotlinMetadataConsts.KOTLIN_METADATA_XI_PARAMETER)

		Metadata(
			kind = k,
			metadataVersion = mvArray,
			data1 = d1Array,
			data2 = d2Array,
			extraString = xs,
			packageName = pn,
			extraInt = xi,
		)
	}
}

private fun IAnnotation.getParamsAsList(paramName: String): List<EncodedValue>? {
	val encodedValue = values[paramName]
		?.takeIf { it.type == EncodedType.ENCODED_ARRAY && it.value is List<*> }
	return encodedValue?.value?.let { it as List<EncodedValue> }
}

private fun IAnnotation.getParamAsStringArray(paramName: String): Array<String>? {
	return getParamsAsList(paramName)
		?.map<EncodedValue, Any?>(EncodedValue::getValue)
		?.onEach { if (it != null && it !is String) return@onEach }
		?.map { "$it" }
		?.toTypedArray()
}

private fun IAnnotation.getParamAsIntArray(paramName: String): IntArray? {
	return getParamsAsList(paramName)
		?.map<EncodedValue, Any?>(EncodedValue::getValue)
		?.map { it as Int }
		?.toIntArray()
}

private fun IAnnotation.getParamAsInt(paramName: String): Int? {
	val encodedValue = values[paramName]
		?.takeIf { it.type == EncodedType.ENCODED_INT && it.value is Int }
	return encodedValue?.value?.let { it as Int }
}

private fun IAnnotation.getParamAsString(paramName: String): String? {
	val encodedValue = values[paramName]
		?.takeIf { it.type == EncodedType.ENCODED_STRING && it.value is String }
	return encodedValue?.value?.let { it as String }
}

fun ClassNode.getKotlinClassMetadata(): KotlinClassMetadata? {
	return getMetadata()?.let(KotlinClassMetadata::readLenient)
}
