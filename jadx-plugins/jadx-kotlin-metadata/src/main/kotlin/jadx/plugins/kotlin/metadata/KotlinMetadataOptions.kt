package jadx.plugins.kotlin.metadata

import jadx.api.plugins.options.OptionDescription
import jadx.api.plugins.options.impl.BaseOptionsParser
import jadx.api.plugins.options.impl.JadxOptionDescription.booleanOption
import jadx.plugins.kotlin.metadata.KotlinMetadataPlugin.Companion.PLUGIN_ID

class KotlinMetadataOptions : BaseOptionsParser() {
	var isClassAlias: Boolean = true
		private set
	var isMethodArgs: Boolean = true
		private set
	var isFields: Boolean = true
		private set
	var isCompanion: Boolean = true
		private set
	var isDataClass: Boolean = true
		private set
	var isToString: Boolean = true
		private set
	var isGetters: Boolean = true
		private set

	override fun parseOptions() {
		isClassAlias = getBooleanOption(CLASS_ALIAS_OPT, true)
		isMethodArgs = getBooleanOption(METHOD_ARGS_OPT, true)
		isFields = getBooleanOption(FIELDS_OPT, true)
		isCompanion = getBooleanOption(COMPANION_OPT, true)
		isDataClass = getBooleanOption(DATA_CLASS_OPT, true)
		isToString = getBooleanOption(TO_STRING_OPT, true)
		isGetters = getBooleanOption(GETTERS_OPT, true)
	}

	override fun getOptionsDescriptions(): List<OptionDescription> {
		return listOf(
			booleanOption(CLASS_ALIAS_OPT, "rename class alias", true),
			booleanOption(METHOD_ARGS_OPT, "rename function arguments", true),
			booleanOption(FIELDS_OPT, "rename fields", true),
			booleanOption(COMPANION_OPT, "rename companion object", true),
			booleanOption(DATA_CLASS_OPT, "add data class modifier", true),
			booleanOption(TO_STRING_OPT, "rename fields using toString", true),
			booleanOption(GETTERS_OPT, "rename simple getters to field names", true),
		)
	}

	fun isPreparePassNeeded(): Boolean {
		return isClassAlias
	}

	fun isDecompilePassNeeded(): Boolean {
		return isMethodArgs || isFields || isCompanion || isDataClass || isToString || isGetters
	}

	companion object {
		const val CLASS_ALIAS_OPT = "$PLUGIN_ID.class-alias"
		const val METHOD_ARGS_OPT = "$PLUGIN_ID.method-args"
		const val FIELDS_OPT = "$PLUGIN_ID.fields"
		const val COMPANION_OPT = "$PLUGIN_ID.companion"
		const val DATA_CLASS_OPT = "$PLUGIN_ID.data-class"
		const val TO_STRING_OPT = "$PLUGIN_ID.to-string"
		const val GETTERS_OPT = "$PLUGIN_ID.getters"
	}
}
