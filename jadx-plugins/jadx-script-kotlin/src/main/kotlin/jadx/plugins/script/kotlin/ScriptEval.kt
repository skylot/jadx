package jadx.plugins.script.kotlin

import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.plugins.JadxPluginContext
import jadx.plugins.script.kotlin.runtime.JadxScriptData
import jadx.plugins.script.kotlin.runtime.JadxScriptTemplate
import jadx.plugins.script.kotlin.runtime.data.JadxScriptAllOptions
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicitsExceptInnermost
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptDiagnostic.Severity
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.compilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.defaultIdentifier
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.displayName
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.api.filePathPattern
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.isStandalone
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val log = KotlinLogging.logger {}

object DefCompileConf : ScriptCompilationConfiguration(ScriptEval.buildDefaultCompileConf())

class ScriptEval {
	companion object {
		fun buildDefaultCompileConf(): ScriptCompilationConfiguration {
			val scriptEval = ScriptEval()
			val hostConf = scriptEval.buildHostConf(null)
			return scriptEval.buildCompileConf(hostConf)
		}
	}

	fun process(context: JadxPluginContext, scriptOptions: JadxScriptAllOptions): List<JadxScriptData> {
		val jadx = context.decompiler
		val scripts = jadx.args.inputFiles.filter { f -> f.name.endsWith(".jadx.kts") }
		if (scripts.isEmpty()) {
			return emptyList()
		}
		val scriptDataList = mutableListOf<JadxScriptData>()
		for (scriptFile in scripts) {
			val scriptData = JadxScriptData(jadx, context, scriptOptions, scriptFile)
			scriptDataList.add(scriptData)
			eval(context, scriptData)
		}
		return scriptDataList
	}

	private fun eval(
		context: JadxPluginContext,
		scriptData: JadxScriptData,
	) {
		scriptData.log.debug { "Loading script: ${scriptData.scriptFile.absolutePath}" }
		val hostConf = buildHostConf(context)
		val compileConf = buildCompileConf(hostConf)
		val evalConf = buildEvalConf(scriptData, compileConf)
		val scriptingHost = BasicJvmScriptingHost(hostConf)
		val execTime = measureTimeMillis {
			val result = scriptingHost.eval(scriptData.scriptFile.toScriptSource(), compileConf, evalConf)
			processEvalResult(result, scriptData)
		}
		scriptData.log.debug { "Script '${scriptData.scriptName}' executed in ${execTime.toDuration(DurationUnit.MILLISECONDS)}" }
	}

	private fun processEvalResult(res: ResultWithDiagnostics<EvaluationResult>, scriptData: JadxScriptData) {
		val log = scriptData.log
		for (r in res.reports) {
			val msg = r.render(withSeverity = false)
			when (r.severity) {
				Severity.FATAL, Severity.ERROR -> log.error(r.exception) { "Script execution error: $msg" }
				Severity.WARNING -> log.warn { "Script execution issue: $msg" }
				Severity.INFO -> log.info { "Script report: $msg" }
				Severity.DEBUG -> log.debug { "Script debug: $msg" }
			}
		}
		when (res) {
			is ResultWithDiagnostics.Success -> {
				when (val retVal = res.value.returnValue) {
					is ResultValue.Error -> log.error(retVal.error) { "Script execution error:" }
					is ResultValue.Value -> log.info { "Script execution result: $retVal" }
					is ResultValue.Unit -> {}
					ResultValue.NotEvaluated -> {}
				}
			}

			is ResultWithDiagnostics.Failure -> {
				scriptData.error = true
				log.error { "Script execution failed: ${scriptData.scriptName}" }
			}
		}
	}

	fun buildHostConf(context: JadxPluginContext?) = ScriptingHostConfiguration {
		jvm {
			getScriptingClass(JvmGetScriptingClass())
			baseClassLoader.put(JadxScriptTemplate::class.java.classLoader)
			context?.let {
				compilationCache(ScriptCache().build(context))
			}
		}
	}

	fun buildCompileConf(scriptingHostConf: ScriptingHostConfiguration) = ScriptCompilationConfiguration {
		hostConfiguration.put(scriptingHostConf)

		displayName.put("Jadx script")
		defaultIdentifier.put("JadxScript")

		fileExtension.put("jadx.kts")
		filePathPattern.put(".*\\.jadx\\.kts")

		val receiversTypes = listOf(KotlinType(JadxScriptTemplate::class))
		implicitReceivers(receiversTypes)
		skipExtensionsResolutionForImplicitsExceptInnermost(receiversTypes)

		jvm {
			dependenciesFromCurrentContext(
				wholeClasspath = true,
			)
		}

		addBaseClass<JadxScriptTemplate>()
		defaultImports(DependsOn::class, Repository::class)

		refineConfiguration {
			onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
		}

		ide {
			acceptedLocations(ScriptAcceptedLocation.Everywhere)
		}

		isStandalone(true)

		// forcing compiler to not use modules while building script classpath
		// because shadow jar remove all modules-info.class (https://github.com/GradleUp/shadow/issues/710)
		compilerOptions.append("-Xjdk-release=1.8")
	}

	inline fun <reified T> ScriptCompilationConfiguration.Builder.addBaseClass() {
		val kClass = T::class
		defaultImports.append(kClass.java.name)
		hostConfiguration.update {
			it.with {
				this[jvm.baseClassLoader] = kClass.java.classLoader
			}
		}
	}

	fun buildEvalConf(scriptData: JadxScriptData, compileConf: ScriptCompilationConfiguration): ScriptEvaluationConfiguration {
		return ScriptEvaluationConfiguration {
			hostConfiguration.put(compileConf[hostConfiguration]!!)
			compilationConfiguration.put(compileConf)
			constructorArgs(JadxScriptTemplate(scriptData))
		}
	}

	private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

	fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
		val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
			?.takeIf { it.isNotEmpty() }
			?: return context.compilationConfiguration.asSuccess()
		return runBlocking {
			resolver.resolveFromScriptSourceAnnotations(annotations)
		}.onSuccess { files: List<File> ->
			log.debug { "add script dependency: $files" }
			context.compilationConfiguration.with {
				updateClasspath(files)
			}.asSuccess()
		}
	}
}
