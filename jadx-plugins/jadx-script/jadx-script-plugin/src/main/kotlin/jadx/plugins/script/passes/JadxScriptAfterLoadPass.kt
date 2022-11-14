package jadx.plugins.script.passes

import jadx.api.JadxDecompiler
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo
import jadx.api.plugins.pass.types.JadxAfterLoadPass
import jadx.plugins.script.runtime.JadxScriptData

class JadxScriptAfterLoadPass(private val scripts: List<JadxScriptData>) : JadxAfterLoadPass {

	override fun getInfo() = SimpleJadxPassInfo("JadxScriptAfterLoad", "Execute scripts 'afterLoad' block")

	override fun init(decompiler: JadxDecompiler) {
		for (script in scripts) {
			if (script.error) {
				continue
			}
			try {
				for (b in script.afterLoad) {
					b.invoke()
				}
			} catch (e: Throwable) {
				script.error = true
				script.log.error(e) { "Error executing 'afterLoad' block in script: ${script.scriptFile.name}" }
			}
		}
	}
}
