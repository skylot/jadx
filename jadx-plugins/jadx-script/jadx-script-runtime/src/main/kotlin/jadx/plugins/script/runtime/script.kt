package jadx.plugins.script.runtime

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
	fileExtension = "jadx.kts",
	compilationConfiguration = JadxScriptConfiguration::class
)
abstract class JadxScript

object JadxScriptConfiguration : ScriptCompilationConfiguration({
	defaultImports(DependsOn::class, Repository::class)

	jvm {
		dependenciesFromCurrentContext(
			wholeClasspath = true
		)
	}

	baseClass(JadxScriptBaseClass::class)

	refineConfiguration {
		onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
	}
})

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
	val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
		?.takeIf { it.isNotEmpty() }
		?: return context.compilationConfiguration.asSuccess()
	return runBlocking { resolver.resolveFromScriptSourceAnnotations(annotations) }
		.onSuccess {
			context.compilationConfiguration.with {
				dependencies.append(JvmDependency(it))
			}.asSuccess()
		}
}
