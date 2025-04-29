package jadx.plugins.script

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.script.passes.JadxScriptAfterLoadPass
import jadx.plugins.script.runtime.data.JadxScriptAllOptions

class JadxScriptPlugin : JadxPlugin {
	override fun getPluginInfo() = JadxPluginInfo("jadx-script", "Jadx Script", "Scripting support for jadx")

	override fun init(context: JadxPluginContext) {
		val scriptOptions = JadxScriptAllOptions()
		context.registerOptions(scriptOptions)
		val scripts = ScriptEval().process(context, scriptOptions)
		if (scripts.isNotEmpty()) {
			context.addPass(JadxScriptAfterLoadPass(scripts))
			context.guiContext?.let { JadxScriptOptionsUI.setup(it, scriptOptions) }
		}
	}
}
