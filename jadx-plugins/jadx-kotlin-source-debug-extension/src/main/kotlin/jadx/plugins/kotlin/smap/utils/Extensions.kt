@file:Suppress("UNCHECKED_CAST")

package jadx.plugins.kotlin.smap.utils

import jadx.api.plugins.input.data.annotations.EncodedType
import jadx.api.plugins.input.data.annotations.EncodedValue
import jadx.api.plugins.input.data.annotations.IAnnotation
import jadx.core.dex.nodes.ClassNode
import jadx.plugins.kotlin.smap.model.Constants
import jadx.plugins.kotlin.smap.model.SMAP

fun ClassNode.getSourceDebugExtension(): SMAP? {
	val annotation: IAnnotation? = getAnnotation(Constants.KOTLIN_SOURCE_DEBUG_EXTENSION)
	return annotation?.run {
		val smapParser = SMAPParser.parseOrNull(getParamsAsList("value")?.get(0)?.value.toString())
		return smapParser
	}
}

private fun IAnnotation.getParamsAsList(paramName: String): List<EncodedValue>? {
	val encodedValue = values[paramName]
		?.takeIf { it.type == EncodedType.ENCODED_ARRAY && it.value is List<*> }
	return encodedValue?.value?.let { it as List<EncodedValue> }
}
