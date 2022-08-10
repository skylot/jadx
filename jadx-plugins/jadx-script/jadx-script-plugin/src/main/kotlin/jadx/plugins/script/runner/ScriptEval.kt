package jadx.plugins.script.runner

import jadx.api.JadxDecompiler
import jadx.api.plugins.JadxPluginContext
import jadx.plugins.script.runtime.JadxScript
import jadx.plugins.script.runtime.JadxScriptData
import jadx.plugins.script.runtime.data.JadxScriptAllOptions
import mu.KotlinLogging
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

private val LOG = KotlinLogging.logger {}

class ScriptEval {

	fun process(init: JadxPluginContext, scriptOptions: JadxScriptAllOptions): ScriptStates? {
		val jadx = init.decompiler as JadxDecompiler
		val scripts = jadx.args.inputFiles.filter { f -> f.name.endsWith(".jadx.kts") }
		if (scripts.isEmpty()) {
			return null
		}
		val scriptStates = ScriptStates()
		for (scriptFile in scripts) {
			val scriptData = JadxScriptData(jadx, init, scriptOptions, scriptFile)
			load(scriptFile, scriptData)
			scriptStates.add(scriptFile, scriptData)
		}
		return scriptStates
	}

	private fun load(scriptFile: File, scriptData: JadxScriptData) {
		LOG.debug { "Loading script: ${scriptFile.absolutePath}" }
		val result = eval(scriptFile, scriptData)
		processEvalResult(result, scriptFile)
	}

	fun buildCompileConf() = createJvmCompilationConfigurationFromTemplate<JadxScript>()

	fun buildEvalConf(scriptData: JadxScriptData): ScriptEvaluationConfiguration {
		return createJvmEvaluationConfigurationFromTemplate<JadxScript> {
			constructorArgs(scriptData)
		}
	}

	private fun eval(scriptFile: File, scriptData: JadxScriptData): ResultWithDiagnostics<EvaluationResult> {
		val compilationConf = buildCompileConf()
		val evalConf = buildEvalConf(scriptData)
		return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), compilationConf, evalConf)
	}

	private fun processEvalResult(res: ResultWithDiagnostics<EvaluationResult>, scriptFile: File) {
		when (res) {
			is ResultWithDiagnostics.Success -> {
				val result = res.value.returnValue
				if (result is ResultValue.Error) {
					result.error.printStackTrace()
				}
			}
			is ResultWithDiagnostics.Failure -> {
				LOG.error { "Script execution failed: ${scriptFile.name}" }
				res.reports
					.filter { it.severity >= ScriptDiagnostic.Severity.ERROR }
					.forEach { r ->
						LOG.error(r.exception) { r.render(withSeverity = false) }
					}
			}
		}
	}
}
