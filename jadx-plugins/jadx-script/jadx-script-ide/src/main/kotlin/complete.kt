package jadx.plugins.script.ide

import jadx.plugins.script.runner.ScriptEval
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.toSourceCodePosition

const val AUTO_COMPLETE_INSERT_STR = "ABCDEF" // defined at KJvmReplCompleter.INSERTED_STRING

data class ScriptCompletionResult(
	val completions: List<SourceCodeCompletionVariant>,
	val reports: List<ScriptDiagnostic>
)

class JadxScriptAutoComplete(private val scriptName: String) {
	private val replCompiler = KJvmReplCompilerWithIdeServices()
	private val compileConf = ScriptEval().buildCompileConf()

	fun complete(code: String, cursor: Int): ScriptCompletionResult {
		val result = complete(code.toScriptSource(scriptName), cursor)
		return ScriptCompletionResult(
			completions = result.valueOrNull()?.toList() ?: listOf(),
			reports = result.reports
		)
	}

	private fun complete(code: SourceCode, cursor: Int): ResultWithDiagnostics<ReplCompletionResult> {
		return runBlocking {
			replCompiler.complete(code, cursor.toSourceCodePosition(code), compileConf)
		}
	}
}
