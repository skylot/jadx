package jadx.plugins.script.runner

import jadx.plugins.script.runtime.JadxScriptData
import java.io.File

data class ScriptStateData(
	val scriptFile: File,
	val scriptData: JadxScriptData,
	var error: Boolean = false
)

class ScriptStates {

	private val data: MutableList<ScriptStateData> = ArrayList()

	fun add(scriptFile: File, scriptData: JadxScriptData) {
		data.add(ScriptStateData(scriptFile, scriptData))
	}

	fun getScripts() = data
}
