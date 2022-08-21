package jadx.plugins.script.passes

import jadx.api.JadxDecompiler
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo
import jadx.api.plugins.pass.types.JadxAfterLoadPass
import jadx.plugins.script.runner.ScriptStates
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class JadxScriptAfterLoadPass(private val scriptStates: ScriptStates) : JadxAfterLoadPass {

	override fun getInfo() = SimpleJadxPassInfo("JadxScriptAfterLoad", "Execute scripts 'afterLoad' block")

	override fun init(decompiler: JadxDecompiler) {
		for (script in scriptStates.getScripts()) {
			if (script.error) {
				continue
			}
			try {
				for (b in script.scriptData.afterLoad) {
					b.invoke()
				}
			} catch (e: Throwable) {
				script.error = true
				LOG.error(e) { "Error executing 'afterLoad' block in script: ${script.scriptFile.name}" }
			}
		}
	}
}
