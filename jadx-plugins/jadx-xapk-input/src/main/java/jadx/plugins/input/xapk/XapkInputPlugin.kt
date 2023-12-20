package jadx.plugins.input.xapk

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.input.dex.DexInputPlugin

class XapkInputPlugin : JadxPlugin {
	private val codeInput = XapkCustomCodeInput(this)
	private val resourcesLoader = XapkCustomResourcesLoader()
	internal var dexInputPlugin: DexInputPlugin? = null

	override fun getPluginInfo() = JadxPluginInfo(
		"xapk-input",
		"XAPK Input",
		"Load .xapk files",
	)

	override fun init(context: JadxPluginContext) {
		dexInputPlugin = context.decompiler.pluginManager.allPluginContexts
			.map { it.plugin }
			.firstOrNull { it is DexInputPlugin } as? DexInputPlugin

		context.addCodeInput(codeInput)
		context.decompiler.addCustomResourcesLoader(resourcesLoader)
	}
}
