package jadx.plugins.kotlin.metadata

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.plugins.kotlin.metadata.pass.KotlinMetadataDecompilePass
import jadx.plugins.kotlin.metadata.pass.KotlinMetadataPreparePass

class KotlinMetadataPlugin : JadxPlugin {

	private val options = KotlinMetadataOptions()

	override fun getPluginInfo(): JadxPluginInfo {
		return JadxPluginInfo(PLUGIN_ID, "Kotlin Metadata", "Use kotlin.Metadata annotation for code generation")
	}

	override fun init(context: JadxPluginContext) {
		context.registerOptions(options)
		if (options.isPreparePassNeeded()) {
			context.addPass(KotlinMetadataPreparePass(options))
		}
		if (options.isDecompilePassNeeded()) {
			context.addPass(KotlinMetadataDecompilePass(options))
		}
	}

	companion object {
		const val PLUGIN_ID = "kotlin-metadata"
	}
}
