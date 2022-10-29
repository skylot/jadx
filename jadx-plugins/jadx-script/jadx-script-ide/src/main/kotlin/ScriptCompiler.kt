package jadx.plugins.script.ide

import jadx.plugins.script.runner.ScriptEval
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import kotlin.script.experimental.api.ReplAnalyzerResult
import kotlin.script.experimental.api.ReplCompletionResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.SourceCodeCompletionVariant
import kotlin.script.experimental.api.analysisDiagnostics
import kotlin.script.experimental.api.renderedResultType
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.toSourceCodePosition

const val AUTO_COMPLETE_INSERT_STR = "ABCDEF" // defined at KJvmReplCompleter.INSERTED_STRING

data class ScriptCompletionResult(
	val completions: List<SourceCodeCompletionVariant>,
	val reports: List<ScriptDiagnostic>
)

data class ScriptAnalyzeResult(
	val errors: List<ScriptDiagnostic>,
	val renderType: String?,
	val reports: List<ScriptDiagnostic>
)

class ScriptCompiler(private val scriptName: String) {
	private val replCompiler = KJvmReplCompilerWithIdeServices()
	private val compileConf = ScriptEval().buildCompileConf()

	fun complete(code: String, cursor: Int): ScriptCompletionResult {
		val result = complete(code.toScriptSource(scriptName), cursor)
		return ScriptCompletionResult(
			completions = result.valueOrNull()?.toList() ?: emptyList(),
			reports = result.reports
		)
	}

	fun analyze(code: String, cursor: Int): ScriptAnalyzeResult {
		val result = analyze(code.toScriptSource(scriptName), cursor)
		val analyzerResult = result.valueOrNull()
		return ScriptAnalyzeResult(
			errors = analyzerResult?.get(ReplAnalyzerResult.analysisDiagnostics)?.toList() ?: emptyList(),
			renderType = analyzerResult?.get(ReplAnalyzerResult.renderedResultType),
			reports = result.reports
		)
	}

	private fun complete(code: SourceCode, cursor: Int): ResultWithDiagnostics<ReplCompletionResult> {
		return runBlocking {
			replCompiler.complete(code, cursor.toSourceCodePosition(code), compileConf)
		}
	}

	private fun analyze(code: SourceCode, cursor: Int): ResultWithDiagnostics<ReplAnalyzerResult> {
		return runBlocking {
			replCompiler.analyze(code, cursor.toSourceCodePosition(code), compileConf)
		}
	}
}
