package jadx.plugins.input.apks

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.input.dex.DexInputPlugin

class ApksInputPlugin : JadxPlugin {
	override fun getPluginInfo() = JadxPluginInfo(
		"apks-input",
		"APKS Input",
		"Load .apks files",
	)

	override fun init(context: JadxPluginContext) {
		val dexInputPlugin = context.plugins().getInstance(DexInputPlugin::class.java)
		context.addCodeInput(ApksCustomCodeInput(dexInputPlugin, context.zipReader))
		context.decompiler.addCustomResourcesLoader(ApksCustomResourcesLoader(context.zipReader))
	}
}
