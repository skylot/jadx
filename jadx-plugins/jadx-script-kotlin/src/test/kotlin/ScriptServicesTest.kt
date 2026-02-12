package jadx.plugins.script

import jadx.plugins.script.kotlin.ScriptServices
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.script.experimental.api.ScriptDiagnostic.Severity.ERROR

class ScriptServicesTest {

	@Test
	fun testAnalyzeSimple() {
		val name = "simple"
		val script = getSampleScript(name)
		val result = ScriptServices().analyze(name, script)
		println(result)
		assertThat(result.success).isTrue()
		assertThat(result.issues).noneMatch { it.severity == ERROR }
	}

	@Disabled("External dependencies not resolved")
	@Test
	fun testAnalyzeDeps() {
		val name = "test-deps"
		val script = getSampleScript(name)
		val result = ScriptServices().analyze(name, script)
		println(result)
		assertThat(result.success).isTrue()
		assertThat(result.issues).noneMatch { it.severity == ERROR }
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

	@Disabled("External dependencies not resolved")
	@Test
	fun testCompleteDeps() {
		val sampleName = "test-deps"
		val script = getSampleScript(sampleName)
		val startPos = script.indexOf("StringEscapeUtils.escapeJava")
		val completePos = startPos + 26 // StringEscapeUtils.escapeJa| <- complete 'escapeJava('
		val exprEnd = script.indexOf('}', startIndex = completePos)
		val curScript = script.removeRange(completePos, exprEnd)
		val result = ScriptServices().complete(sampleName, curScript, completePos)
		println(result)
		assertThat(result.completions)
			.hasSize(1)
			.allMatch { c -> c.text == "escapeJava(" }
	}

	private fun getSampleScript(scriptName: String): String {
		val resFile = javaClass.classLoader.getResource("samples/$scriptName.jadx.kts")
		return resFile!!.readText()
	}
}
