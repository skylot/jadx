package jadx.plugins.input.xapk

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo

class XapkInputPlugin : JadxPlugin {
	private val resourcesLoader = XapkCustomResourcesLoader()

	override fun getPluginInfo() = JadxPluginInfo(
			"xapk-input",
			"XAPK Input",
			"Load .xapk files"
	)

	override fun init(context: JadxPluginContext) {
		context.decompiler.addCustomResourcesLoader(resourcesLoader)
	}
}
