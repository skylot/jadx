package jadx.plugins.script

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JadxScriptPluginTest {

	@Test
	fun integrationTest() {
		val args = JadxArgs()
		args.inputFiles.run {
			add(getSampleFile("hello.smali"))
			add(getSampleFile("test.jadx.kts"))
		}
		val elapsed = measureTimeMillis {
			JadxDecompiler(args).use { jadx ->
				jadx.load()
				assertThat(jadx.classes)
					.hasSize(1)
					.allMatch { it.name == "HelloJadx" }
			}
		}
		println("Elapsed time: ${elapsed.toDuration(DurationUnit.MILLISECONDS)}")
	}

	private fun getSampleFile(file: String): File {
		val resFile = javaClass.classLoader.getResource("samples/$file")
		return File(resFile!!.toURI())
	}
}
