package jadx.plugins.script.kotlin.runtime

import jadx.plugins.script.kotlin.DefCompileConf
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
	displayName = "Jadx Script",
	fileExtension = "jadx.kts",
	filePathPattern = ".*\\.jadx\\.kts",
	compilationConfiguration = DefCompileConf::class,
)
open class JadxScriptTemplate(
	scriptData: JadxScriptData,
) {
	val scriptName = scriptData.scriptName
	val log = scriptData.log

	private val scriptInstance = JadxScriptInstance(scriptData, log)

	fun getJadxInstance() = scriptInstance

	fun println(message: Any?) {
		log.info { message }
	}

	fun print(message: Any?) {
		log.info { message }
	}
}
