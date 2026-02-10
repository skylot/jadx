package jadx.plugins.script.kotlin

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.JadxPluginInfoBuilder
import jadx.plugins.script.kotlin.gui.JadxScriptInputCategory
import jadx.plugins.script.kotlin.gui.JadxScriptOptionsUI
import jadx.plugins.script.kotlin.passes.JadxScriptAfterLoadPass
import jadx.plugins.script.kotlin.runtime.data.JadxScriptAllOptions

class JadxScriptKotlinPlugin : JadxPlugin {
	companion object {
		const val PLUGIN_ID = "jadx-script-kotlin"
	}

	override fun getPluginInfo(): JadxPluginInfo = JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
		.name("Jadx Script (Kotlin)")
		.description("Scripting support for jadx using Kotlin script")
		.homepage("https://github.com/jadx-decompiler/jadx-script-kotlin")
		.requiredJadxVersion("1.5.4, r2596")
		.provides("jadx-script") // conflict with bundled plugin from older jadx versions
		.build()

	override fun init(context: JadxPluginContext) {
		val scriptOptions = JadxScriptAllOptions()
		context.registerOptions(scriptOptions)
		val scripts = ScriptEval().process(context, scriptOptions)
		if (scripts.isNotEmpty()) {
			context.addPass(JadxScriptAfterLoadPass(scripts))
			context.guiContext?.let { guiContext ->
				JadxScriptOptionsUI.setup(guiContext, scriptOptions)
				JadxScriptInputCategory.register(context, guiContext)
			}
		}
	}
}
