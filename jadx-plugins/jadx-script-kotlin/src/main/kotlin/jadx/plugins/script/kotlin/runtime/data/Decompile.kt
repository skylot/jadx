package jadx.plugins.script.kotlin.runtime.data

import jadx.api.JadxArgs
import jadx.api.JavaClass
import jadx.plugins.script.kotlin.runtime.JadxScriptInstance
import java.util.concurrent.Executors

class Decompile(private val jadx: JadxScriptInstance) {

	fun all(ignoreCache: Boolean = false) {
		if (ignoreCache) {
			jadx.classes.forEach(JavaClass::reload)
		} else {
			jadx.classes.forEach(JavaClass::decompile)
		}
	}

	fun allThreaded(threadsCount: Int = JadxArgs.DEFAULT_THREADS_COUNT) {
		val executor = Executors.newFixedThreadPool(threadsCount)
		val batches = jadx.internalDecompiler.decompileScheduler.buildBatches(jadx.classes)
		for (batch in batches) {
			executor.submit {
				batch.forEach(JavaClass::decompile)
			}
		}
	}
}
