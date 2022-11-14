package jadx.plugins.script

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.script.passes.JadxScriptAfterLoadPass
import jadx.plugins.script.runtime.data.JadxScriptAllOptions

class JadxScriptPlugin : JadxPlugin {
	private val scriptOptions = JadxScriptAllOptions()

	override fun getPluginInfo() = JadxPluginInfo("jadx-script", "Jadx Script", "Scripting support for jadx")

	override fun init(init: JadxPluginContext) {
		init.registerOptions(scriptOptions)
		val scripts = ScriptEval().process(init, scriptOptions)
		if (scripts.isNotEmpty()) {
			init.addPass(JadxScriptAfterLoadPass(scripts))
		}
	}
}
