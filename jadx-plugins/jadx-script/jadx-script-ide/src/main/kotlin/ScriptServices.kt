package jadx.plugins.script.ide

import jadx.plugins.script.ScriptEval
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import kotlin.script.experimental.api.ReplAnalyzerResult
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.SourceCodeCompletionVariant
import kotlin.script.experimental.api.analysisDiagnostics
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.renderedResultType
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvm.util.toSourceCodePosition

const val AUTO_COMPLETE_INSERT_STR = "ABCDEF" // defined at KJvmReplCompleter.INSERTED_STRING

data class ScriptCompletionResult(
	val completions: List<SourceCodeCompletionVariant>,
	val reports: List<ScriptDiagnostic>,
)

data class ScriptAnalyzeResult(
	val success: Boolean,
	val issues: List<ScriptDiagnostic>,
	val renderType: String?,
)

class ScriptServices {
	private val compileConf = ScriptEval.compileConf
	private val replCompiler = KJvmReplCompilerWithIdeServices(
		compileConf[ScriptCompilationConfiguration.hostConfiguration]
			?: defaultJvmScriptingHostConfiguration,
	)

	fun complete(scriptName: String, code: String, cursor: Int): ScriptCompletionResult {
		val snippet = code.toScriptSource(scriptName)
		val result = runBlocking {
			replCompiler.complete(snippet, cursor.toSourceCodePosition(snippet), compileConf)
		}
		return ScriptCompletionResult(
			completions = result.valueOrNull()?.toList() ?: emptyList(),
			reports = result.reports,
		)
	}

	fun analyze(scriptName: String, code: String): ScriptAnalyzeResult {
		val sourceCode = code.toScriptSource(scriptName)
		val result = runBlocking {
			val cursor = SourceCode.Position(0, 0) // not used
			replCompiler.analyze(sourceCode, cursor, compileConf)
		}
		val analyzerResult = result.valueOrNull()
		val issues = mutableListOf<ScriptDiagnostic>()
		analyzerResult?.get(ReplAnalyzerResult.analysisDiagnostics)?.let(issues::addAll)
		issues.addAll(result.reports)
		return ScriptAnalyzeResult(
			success = !result.isError(),
			issues = issues,
			renderType = analyzerResult?.get(ReplAnalyzerResult.renderedResultType),
		)
	}
}
