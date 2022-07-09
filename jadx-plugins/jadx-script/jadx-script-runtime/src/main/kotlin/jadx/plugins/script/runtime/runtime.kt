@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jadx.plugins.script.runtime

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.JavaClass
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.pass.JadxPass
import jadx.plugins.script.runtime.data.*
import mu.KLogger
import mu.KotlinLogging
import java.io.File


open class JadxScriptBaseClass(private val scriptData: JadxScriptData) {
	val scriptName = scriptData.scriptName
	val log = KotlinLogging.logger("JadxScript:${scriptName}")

	fun getJadxInstance() = JadxScriptInstance(scriptData, log)

	fun println(message: Any?) {
		log.info(message?.toString())
	}

	fun print(message: Any?) {
		log.info(message?.toString())
	}
}

class JadxScriptData(
	val jadxInstance: JadxDecompiler,
	val pluginContext: JadxPluginContext,
	val scriptFile: File
) {
	val afterLoad: MutableList<() -> Unit> = ArrayList()

	val scriptName get() = scriptFile.name.removeSuffix(".jadx.kts")
}

class JadxScriptInstance(
	private val scriptData: JadxScriptData,
	val log: KLogger
) {
	private val decompiler = scriptData.jadxInstance

	val rename: RenamePass by lazy { RenamePass(this) }
	val stages: Stages by lazy { Stages(this) }
	val replace: Replace by lazy { Replace(this) }
	val decompile: Decompile by lazy { Decompile(this) }
	val search: Search by lazy { Search(this) }
	val gui: Gui by lazy { Gui(this, scriptData.pluginContext.guiContext) }
	val debug: Debug by lazy { Debug(this) }

	val args: JadxArgs
		get() = decompiler.args

	val classes: List<JavaClass>
		get() = decompiler.classes

	val scriptFile get() = scriptData.scriptFile

	val scriptName get() = scriptData.scriptName

	fun afterLoad(block: () -> Unit) {
		scriptData.afterLoad.add(block)
	}

	fun addPass(pass: JadxPass) {
		scriptData.pluginContext.passContext.addPass(pass)
	}

	val internalDecompiler: JadxDecompiler
		get() = decompiler
}
