package jadx.plugins.script.kotlin

import jadx.api.plugins.JadxPluginContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import kotlin.script.experimental.api.ReplAnalyzerResult
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCodeCompletionVariant
import kotlin.script.experimental.api.analysisDiagnostics
import kotlin.script.experimental.api.renderedResultType
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvm.util.toSourceCodePosition

data class ScriptCompletionResult(
	val completions: List<SourceCodeCompletionVariant>,
	val reports: MutableList<ScriptDiagnostic>,
)

data class ScriptAnalyzeResult(
	val success: Boolean,
	val issues: List<ScriptDiagnostic>,
	val renderType: String?,
)

class ScriptServices(pluginContext: JadxPluginContext? = null) {
	companion object {
		const val AUTO_COMPLETE_INSERT_STR = "ABCDEF" // defined at KJvmReplCompleter.INSERTED_STRING
	}

	private val compileConf: ScriptCompilationConfiguration
	private val replCompiler: KJvmReplCompilerWithIdeServices

	init {
		val scriptEval = ScriptEval()
		val hostConf = scriptEval.buildHostConf(pluginContext)
		hostConf.with {
			repl {
				firReplHistoryProvider(FirReplHistoryProviderImpl())
				isReplSnippetSource { sourceFile, _ ->
					sourceFile?.name?.endsWith(".jadx.kts", ignoreCase = true) ?: false
				}
			}
		}
		compileConf = scriptEval.buildCompileConf(hostConf)
		replCompiler = KJvmReplCompilerWithIdeServices(hostConf)
	}

	fun complete(scriptName: String, code: String, cursor: Int): ScriptCompletionResult {
		val snippet = code.toScriptSource(scriptName)
		val result = runBlocking {
			replCompiler.complete(snippet, cursor.toSourceCodePosition(snippet), compileConf)
		}
		return ScriptCompletionResult(
			completions = result.valueOrNull()?.toList() ?: emptyList(),
			reports = result.reports.toMutableList(),
		)
	}

	fun analyze(scriptName: String, code: String): ScriptAnalyzeResult {
		val sourceCode = code.toScriptSource(scriptName)
		val result = runBlocking {
			replCompiler.analyze(sourceCode, 0.toSourceCodePosition(sourceCode), compileConf)
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
