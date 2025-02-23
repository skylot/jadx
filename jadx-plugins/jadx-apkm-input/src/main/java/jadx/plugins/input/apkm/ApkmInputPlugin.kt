package jadx.plugins.input.apkm

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.input.dex.DexInputPlugin

class ApkmInputPlugin : JadxPlugin {

	override fun getPluginInfo() = JadxPluginInfo(
		"apkm-input",
		"APKM Input",
		"Load .apkm files",
	)

	override fun init(context: JadxPluginContext) {
		val dexInputPlugin = context.plugins().getInstance(DexInputPlugin::class.java)
		context.addCodeInput(ApkmCustomCodeInput(dexInputPlugin, context.zipReader))
		context.decompiler.addCustomResourcesLoader(ApkmCustomResourcesLoader(context.zipReader))
	}
}
