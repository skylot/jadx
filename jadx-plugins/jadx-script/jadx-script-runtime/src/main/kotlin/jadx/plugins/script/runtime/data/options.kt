package jadx.plugins.script.runtime.data

import jadx.api.plugins.options.JadxPluginOptions
import jadx.api.plugins.options.OptionDescription
import jadx.api.plugins.options.OptionDescription.OptionType
import jadx.api.plugins.options.impl.JadxOptionDescription
import jadx.plugins.script.runtime.JadxScriptInstance

class JadxScriptAllOptions : JadxPluginOptions {
	lateinit var values: Map<String, String>
	val descriptions: MutableList<OptionDescription> = mutableListOf()

	override fun setOptions(options: Map<String, String>) {
		values = options
	}

	override fun getOptionsDescriptions(): MutableList<OptionDescription> = descriptions
}

class ScriptOption<T>(
	val name: String,
	val id: String,
	private val getter: () -> T,
) {
	private var validate: ((T) -> Boolean)? = null

	val value: T
		get() {
			val v = getter.invoke()
			validate?.let { predicate ->
				if (!predicate.invoke(v)) {
					throw IllegalArgumentException("Invalid value '$v' for option $id")
				}
			}
			return v
		}

	fun validate(predicate: (T) -> Boolean): ScriptOption<T> {
		validate = predicate
		return this
	}
}

class JadxScriptOptions(
	private val jadx: JadxScriptInstance,
	private val options: JadxScriptAllOptions
) {

	fun <T> register(
		name: String,
		desc: String,
		values: List<String>,
		defaultValue: String,
		type: OptionType = OptionType.STRING,
		convert: (String?) -> T
	): ScriptOption<T> {
		val id = "jadx-script.${jadx.scriptName}.$name"
		options.descriptions.add(JadxOptionDescription(id, desc, defaultValue, values, type))
		return ScriptOption(name, id) { convert.invoke(options.values[id]) }
	}

	fun registerString(
		name: String,
		desc: String = "",
		values: List<String> = emptyList(),
		defaultValue: String = ""
	): ScriptOption<String> {
		return register(name, desc, values, defaultValue) { value ->
			if (value == null) {
				defaultValue
			} else {
				if (values.isEmpty() || values.contains(value)) {
					value
				} else {
					throw IllegalArgumentException("Unknown value '$value' for option '$name', expect one of $values")
				}
			}
		}
	}

	fun registerYesNo(name: String, desc: String = "", defaultValue: Boolean = false): ScriptOption<Boolean> {
		val defStr = if (defaultValue) "yes" else "no"
		return register(name, desc, listOf("yes", "no"), defStr, OptionType.BOOLEAN) { value ->
			when (value) {
				null -> defaultValue
				"yes", "true" -> true
				"no", "false" -> false
				else -> throw IllegalArgumentException("Unknown value '$value' for option '$name', expect: 'yes' or 'no'")
			}
		}
	}

	fun registerInt(name: String, desc: String = "", defaultValue: Int = 0): ScriptOption<Int> {
		return register(name, desc, emptyList(), defaultValue.toString(), OptionType.NUMBER) { value ->
			when (value) {
				null -> defaultValue
				else -> value.toInt()
			}
		}
	}
}
