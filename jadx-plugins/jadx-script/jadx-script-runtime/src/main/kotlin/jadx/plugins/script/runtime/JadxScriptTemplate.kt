package jadx.plugins.script.runtime

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.ide
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
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
	displayName = "Jadx Script",
	fileExtension = "jadx.kts",
	compilationConfiguration = JadxScriptConfiguration::class,
)
abstract class JadxScriptTemplate(
	scriptData: JadxScriptData,
) {
	val scriptName = scriptData.scriptName
	val log = scriptData.log

	private val scriptInstance = JadxScriptInstance(scriptData, log)

	fun getJadxInstance() = scriptInstance

	fun println(message: Any?) {
		log.info { message }
	}

	fun print(message: Any?) {
		log.info { message }
	}
}

object JadxScriptConfiguration : ScriptCompilationConfiguration({
	defaultImports(DependsOn::class, Repository::class)

	jvm {
		dependenciesFromCurrentContext(
			wholeClasspath = true,
		)
	}
	ide {
		acceptedLocations(ScriptAcceptedLocation.Everywhere)
	}

	refineConfiguration {
		onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
	}

	isStandalone(true)

	// forcing compiler to not use modules while building script classpath
	// because shadow jar remove all modules-info.class (https://github.com/GradleUp/shadow/issues/710)
	compilerOptions.append("-Xjdk-release=1.8")
})

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
	val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
		?.takeIf { it.isNotEmpty() }
		?: return context.compilationConfiguration.asSuccess()
	return runBlocking {
		resolver.resolveFromScriptSourceAnnotations(annotations)
	}.onSuccess {
		context.compilationConfiguration.with {
			dependencies.append(JvmDependency(it))
		}.asSuccess()
	}
}
