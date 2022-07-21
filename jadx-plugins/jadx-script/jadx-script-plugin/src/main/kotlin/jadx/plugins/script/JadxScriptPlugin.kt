package jadx.plugins.script

import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.options.JadxPluginOptions
import jadx.api.plugins.options.OptionDescription
import jadx.plugins.script.passes.JadxScriptAfterLoadPass
import jadx.plugins.script.runner.ScriptEval
import jadx.plugins.script.runtime.data.JadxScriptAllOptions

class JadxScriptPlugin : JadxPluginOptions {
	var scriptOptions: JadxScriptAllOptions = JadxScriptAllOptions(emptyMap())

	override fun getPluginInfo() = JadxPluginInfo("jadx-script", "Jadx Script", "Scripting support for jadx")

	override fun setOptions(options: Map<String, String>) {
		scriptOptions = JadxScriptAllOptions(options)
	}

	override fun init(init: JadxPluginContext) {
		val scriptStates = ScriptEval().process(init, scriptOptions) ?: return
		init.passContext.addPass(JadxScriptAfterLoadPass(scriptStates))
	}

	override fun getOptionsDescriptions(): List<OptionDescription> = scriptOptions.descriptions
}
