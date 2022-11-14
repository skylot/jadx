@file:JvmName("ScriptRuntime")
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jadx.plugins.script.runtime

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.JavaClass
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.pass.JadxPass
import jadx.plugins.script.runtime.data.Debug
import jadx.plugins.script.runtime.data.Decompile
import jadx.plugins.script.runtime.data.Gui
import jadx.plugins.script.runtime.data.JadxScriptAllOptions
import jadx.plugins.script.runtime.data.JadxScriptOptions
import jadx.plugins.script.runtime.data.Rename
import jadx.plugins.script.runtime.data.Replace
import jadx.plugins.script.runtime.data.Search
import jadx.plugins.script.runtime.data.Stages
import mu.KLogger
import mu.KotlinLogging
import java.io.File

const val JADX_SCRIPT_LOG_PREFIX = "JadxScript:"

class JadxScriptData(
	val jadxInstance: JadxDecompiler,
	val pluginContext: JadxPluginContext,
	val options: JadxScriptAllOptions,
	val scriptFile: File,
) {
	val scriptName = scriptFile.name.removeSuffix(".jadx.kts")
	val log = KotlinLogging.logger("$JADX_SCRIPT_LOG_PREFIX$scriptName")
	val afterLoad: MutableList<() -> Unit> = ArrayList()
	var error: Boolean = false
}

class JadxScriptInstance(
	private val scriptData: JadxScriptData,
	val log: KLogger,
) {
	private val decompiler = scriptData.jadxInstance

	val options: JadxScriptOptions by lazy { JadxScriptOptions(this, scriptData.options) }
	val rename: Rename by lazy { Rename(this) }
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
		scriptData.pluginContext.addPass(pass)
	}

	val internalDecompiler: JadxDecompiler
		get() = decompiler
}
