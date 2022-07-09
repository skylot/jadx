package jadx.plugins.script

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.gui.JadxGuiContext
import jadx.api.plugins.pass.JadxPassContext
import jadx.plugins.script.passes.JadxScriptAfterLoadPass
import jadx.plugins.script.runner.ScriptEval

class JadxScriptPlugin : JadxPlugin {

	override fun getPluginInfo() = JadxPluginInfo("jadx-script", "Jadx Script", "Scripting support for jadx")

	override fun init(init: JadxPluginContext) {
		val scriptStates = ScriptEval().process(init) ?: return
		init.passContext.addPass(JadxScriptAfterLoadPass(scriptStates))
	}
}
