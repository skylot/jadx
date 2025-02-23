package jadx.plugins.input.xapk

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.JadxPluginInfoBuilder
import jadx.plugins.input.dex.DexInputPlugin

class XapkInputPlugin : JadxPlugin {

	override fun getPluginInfo(): JadxPluginInfo =
		JadxPluginInfoBuilder.pluginId("xapk-input")
			.name("XAPK Input")
			.description("Load .xapk files")
			.build()

	override fun init(context: JadxPluginContext) {
		val dexInputPlugin = context.plugins().getInstance(DexInputPlugin::class.java)
		context.addCodeInput(XapkCustomCodeInput(dexInputPlugin, context.zipReader))
		context.decompiler.addCustomResourcesLoader(XapkCustomResourcesLoader(context.zipReader))
	}
}
