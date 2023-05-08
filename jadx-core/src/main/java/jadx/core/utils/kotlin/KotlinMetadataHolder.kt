@file:Suppress("UNCHECKED_CAST")
package jadx.core.utils.kotlin

import jadx.api.plugins.input.data.annotations.EncodedType
import jadx.api.plugins.input.data.annotations.EncodedValue
import jadx.api.plugins.input.data.annotations.IAnnotation
import jadx.core.dex.nodes.ClassNode
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata


class KotlinMetadataHolder(
	val classNode: ClassNode,
) {
	val annotation: Metadata = requireNotNull(classNode.getMetadata())
	val kmm by lazy { KotlinClassMetadata.read(annotation) }


	companion object {
		const val KOTLIN_METADATA_ANNOTATION = "Lkotlin/Metadata;"
		const val KOTLIN_METADATA_K_PARAMETER = "k"
		const val KOTLIN_METADATA_D1_PARAMETER = "d1"
		const val KOTLIN_METADATA_D2_PARAMETER = "d2"
		const val KOTLIN_METADATA_MV_PARAMETER = "mv"
		const val KOTLIN_METADATA_XS_PARAMETER = "xs"
		const val KOTLIN_METADATA_PN_PARAMETER = "pn"
		const val KOTLIN_METADATA_XI_PARAMETER = "xi"
	}
}

fun ClassNode.getMetadata(): Metadata? {
	val annotation: IAnnotation? = getAnnotation(KotlinMetadataHolder.KOTLIN_METADATA_ANNOTATION)

	return annotation?.run {
		val k = getParamAsInt(KotlinMetadataHolder.KOTLIN_METADATA_K_PARAMETER)
		val mvArray = getParamAsIntArray(KotlinMetadataHolder.KOTLIN_METADATA_MV_PARAMETER)
		val d1Array = getParamAsStringArray(KotlinMetadataHolder.KOTLIN_METADATA_D1_PARAMETER)
		val d2Array = getParamAsStringArray(KotlinMetadataHolder.KOTLIN_METADATA_D2_PARAMETER)
		val xs = getParamAsString(KotlinMetadataHolder.KOTLIN_METADATA_XS_PARAMETER)
		val pn = getParamAsString(KotlinMetadataHolder.KOTLIN_METADATA_PN_PARAMETER)
		val xi = getParamAsInt(KotlinMetadataHolder.KOTLIN_METADATA_XI_PARAMETER)

		Metadata(
			kind = k,
			metadataVersion = mvArray,
			data1 = d1Array,
			data2 = d2Array,
			extraString = xs,
			packageName = pn,
			extraInt = xi
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
		?.onEach { if (it != null && it !is String) /* TODO is this valid ? */ return@onEach }
		?.map { "$it" }
		?.toTypedArray()
}

private fun IAnnotation.getParamAsIntArray(paramName: String): IntArray? {
	return getParamsAsList(paramName)
		?.map<EncodedValue, Any?>(EncodedValue::getValue)
		?.onEach { if (it != null && it !is Int) /* TODO is this valid ? */ return@onEach }
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


fun ClassNode.getKotlinMetadataHolder(): KotlinMetadataHolder? {
	return getAnnotation(KotlinMetadataHolder.KOTLIN_METADATA_ANNOTATION)
		?.let { KotlinMetadataHolder(this) }
}
