package jadx.plugins.input.apkm

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.input.dex.DexInputPlugin

class ApkmInputPlugin : JadxPlugin {
	private val codeInput = ApkmCustomCodeInput(this)
	private val resourcesLoader = ApkmCustomResourcesLoader()
	internal lateinit var dexInputPlugin: DexInputPlugin

	override fun getPluginInfo() = JadxPluginInfo(
		"apkm-input",
		"APKM Input",
		"Load .apkm files",
	)

	override fun init(context: JadxPluginContext) {
		dexInputPlugin = context.plugins().getInstance(DexInputPlugin::class.java)
		context.addCodeInput(codeInput)
		context.decompiler.addCustomResourcesLoader(resourcesLoader)
	}
}
