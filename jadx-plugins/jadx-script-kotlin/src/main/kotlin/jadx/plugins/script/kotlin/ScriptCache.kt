package jadx.plugins.script.kotlin

import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.plugins.JadxPluginContext
import jadx.core.utils.files.FileUtils
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.loadScriptFromJar
import kotlin.script.experimental.jvmhost.saveToJar

private val log = KotlinLogging.logger {}

class ScriptCache {
	private val enableCache = System.getProperty("JADX_SCRIPT_CACHE_ENABLE", "true").equals("true", ignoreCase = true)

	fun build(context: JadxPluginContext): CompiledJvmScriptsCache {
		if (!enableCache) {
			return CompiledJvmScriptsCache.NoCache
		}
		val cacheDir = getCacheDir(context)
		log.debug { "script cache created in : $cacheDir" }
		return JadxScriptsCache(cacheDir)
	}

	/**
	 * Same as CompiledScriptJarsCache implementation,
	 * but remove all previous cache versions for the script with the same path and name.
	 * This should reduce old cache entries count
	 */
	class JadxScriptsCache(private val baseCacheDir: Path) : CompiledJvmScriptsCache {
		override fun get(
			script: SourceCode,
			scriptCompilationConfiguration: ScriptCompilationConfiguration,
		): CompiledScript? {
			val cacheDir = hashDir(baseCacheDir, script)
			val file = hashFile(cacheDir, script, scriptCompilationConfiguration)
			if (file.exists()) {
				file.toFile().loadScriptFromJar().let {
					log.debug { "loaded script from cache: $file" }
					return it
				}
			}
			log.debug { "script not found in cache: $file" }
			FileUtils.deleteDirIfExists(cacheDir)
			return null
		}

		override fun store(
			compiledScript: CompiledScript,
			script: SourceCode,
			scriptCompilationConfiguration: ScriptCompilationConfiguration,
		) {
			val jvmScript = (compiledScript as? KJvmCompiledScript)
				?: throw IllegalArgumentException("Unsupported script type ${compiledScript::class.java.name}")

			val cacheDir = hashDir(baseCacheDir, script)
			val file = hashFile(cacheDir, script, scriptCompilationConfiguration)

			FileUtils.deleteDirIfExists(cacheDir)
			FileUtils.makeDirs(cacheDir)
			jvmScript.saveToJar(file.toFile())
			log.debug { "script cached: $file" }
		}
	}

	private fun getCacheDir(context: JadxPluginContext): Path {
		val cacheBaseDir = context.files().pluginCacheDir.resolve("compiled")
		FileUtils.makeDirs(cacheBaseDir)
		return cacheBaseDir
	}

	companion object {
		private fun hashDir(baseCacheDir: Path, script: SourceCode): Path {
			if (script.name == null && script.locationId == null) {
				return baseCacheDir.resolve("tmp")
			}
			val digest = MessageDigest.getInstance("MD5")
			digest.add(script.name)
			digest.add(script.locationId)
			return baseCacheDir.resolve(digest.digest().toHexString())
		}

		private fun hashFile(
			cacheDir: Path,
			script: SourceCode,
			scriptCompilationConfiguration: ScriptCompilationConfiguration,
		): Path {
			val digest = MessageDigest.getInstance("MD5")
			digest.add(script.text)
			scriptCompilationConfiguration.notTransientData.entries
				.sortedBy { it.key.name }
				.forEach {
					digest.add(it.key.name)
					digest.add(it.value.toString())
				}
			return cacheDir.resolve(digest.digest().toHexString() + ".jar")
		}

		private fun MessageDigest.add(str: String?) {
			str?.let { this.update(it.toByteArray()) }
		}

		private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
	}
}
