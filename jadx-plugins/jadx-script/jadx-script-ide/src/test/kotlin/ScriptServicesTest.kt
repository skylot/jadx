package jadx.plugins.script.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScriptServicesTest {

	@Test
	fun testAnalyzeSimple() {
		val name = "simple"
		val script = getSampleScript(name)
		val result = ScriptServices().analyze(name, script)
		println(result)
		assertThat(result.success).isTrue()
	}

	@Test
	fun testAnalyzeDeps() {
		val name = "test-deps"
		val script = getSampleScript(name)
		val result = ScriptServices().analyze(name, script)
		println(result)
		assertThat(result.success).isTrue()
	}

	@Test
	fun testComplete() {
		val name = "simple"
		val script = getSampleScript(name)
		val idx = script.indexOf("jadx.log.info")
		val completePos = idx + 7 // jadx.lo| <- complete 'log'
		val curScript = script.substring(0, completePos)

		val result = ScriptServices().complete(name, curScript, completePos)
		println(result)
		assertThat(result.completions)
			.hasSize(1)
			.allMatch { c -> c.text == "log" }
	}

	private fun getSampleScript(scriptName: String): String {
		val resFile = javaClass.classLoader.getResource("samples/$scriptName.jadx.kts")
		return resFile!!.readText()
	}
}
