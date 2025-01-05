package jadx.plugins.kotlin.smap

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder
import jadx.plugins.kotlin.smap.KotlinSmapPlugin.Companion.PLUGIN_ID

class KotlinSmapOptions : BasePluginOptionsBuilder() {
	var isClassAliasSourceDbg: Boolean = true
		private set

	override fun registerOptions() {
		boolOption(CLASS_ALIAS_SOURCE_DBG_OPT)
			.description("rename class alias from SourceDebugExtension")
			.defaultValue(false)
			.setter { isClassAliasSourceDbg = it }
	}

	fun isClassSourceDbg(): Boolean {
		return isClassAliasSourceDbg
	}

	companion object {
		const val CLASS_ALIAS_SOURCE_DBG_OPT = "$PLUGIN_ID.class-alias-source-dbg"
	}
}
