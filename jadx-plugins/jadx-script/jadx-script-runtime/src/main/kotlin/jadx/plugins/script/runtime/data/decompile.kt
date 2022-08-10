package jadx.plugins.script.runtime.data

import jadx.api.JadxArgs
import jadx.api.JavaClass
import jadx.plugins.script.runtime.JadxScriptInstance
import java.util.concurrent.Executors

class Decompile(private val jadx: JadxScriptInstance) {

	fun all() {
		jadx.classes.forEach(JavaClass::decompile)
	}

	fun allThreaded(threadsCount: Int = JadxArgs.DEFAULT_THREADS_COUNT) {
		val executor = Executors.newFixedThreadPool(threadsCount)
		val dec = jadx.internalDecompiler
		val batches = dec.decompileScheduler.buildBatches(jadx.classes)
		for (batch in batches) {
			executor.submit {
				batch.forEach(JavaClass::decompile)
			}
		}
	}
}
