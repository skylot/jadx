package jadx.plugins.kotlin.smap

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.kotlin.smap.pass.KotlinSourceDebugExtensionPass

class KotlinSmapPlugin : JadxPlugin {

	private val options = KotlinSmapOptions()

	override fun getPluginInfo(): JadxPluginInfo {
		return JadxPluginInfo(PLUGIN_ID, "Kotlin SMAP", "Use kotlin.SourceDebugExtension annotation for rename class alias")
	}

	override fun init(context: JadxPluginContext) {
		context.registerOptions(options)

		if (options.isClassSourceDbg()) {
			context.addPass(KotlinSourceDebugExtensionPass(options))
		}
	}

	companion object {
		const val PLUGIN_ID = "kotlin-smap"
	}
}
