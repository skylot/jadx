package jadx.plugins.script

import dev.dirs.ProjectDirectories
import java.io.File
import java.security.MessageDigest
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.loadScriptFromJar
import kotlin.script.experimental.jvmhost.saveToJar

class ScriptCache {
	private val enableCache = System.getProperty("JADX_SCRIPT_CACHE_ENABLE", "true").equals("true", ignoreCase = true)

	fun build(): CompiledJvmScriptsCache {
		if (!enableCache) {
			return CompiledJvmScriptsCache.NoCache
		}
		return JadxScriptsCache(getCacheDir())
	}

	/**
	 * Same as CompiledScriptJarsCache implementation,
	 * but remove all previous cache versions for the script with the same path and name.
	 * This should reduce old cache entries count
	 */
	class JadxScriptsCache(private val baseCacheDir: File) : CompiledJvmScriptsCache {
		override fun get(
			script: SourceCode,
			scriptCompilationConfiguration: ScriptCompilationConfiguration,
		): CompiledScript? {
			val cacheDir = hashDir(baseCacheDir, script)
			val file = hashFile(cacheDir, script, scriptCompilationConfiguration)
			if (!file.exists()) {
				return null
			}
			return file.loadScriptFromJar() ?: run {
				// invalidate cache if the script cannot be loaded
				cacheDir.deleteRecursively()
				null
			}
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

			cacheDir.deleteRecursively()
			cacheDir.mkdirs()
			jvmScript.saveToJar(file)
		}
	}

	private fun getCacheDir(): File {
		val dirs = ProjectDirectories.from("io.github", "skylot", "jadx")
		val cacheBaseDir = File(dirs.cacheDir, "scripts")
		cacheBaseDir.mkdirs()
		return cacheBaseDir
	}

	companion object {
		private fun hashDir(baseCacheDir: File, script: SourceCode): File {
			if (script.name == null && script.locationId == null) {
				return File(baseCacheDir, "tmp")
			}
			val digest = MessageDigest.getInstance("MD5")
			digest.add(script.name)
			digest.add(script.locationId)
			return File(baseCacheDir, digest.digest().toHexString())
		}

		private fun hashFile(
			cacheDir: File,
			script: SourceCode,
			scriptCompilationConfiguration: ScriptCompilationConfiguration,
		): File {
			val digest = MessageDigest.getInstance("MD5")
			digest.add(script.text)
			scriptCompilationConfiguration.notTransientData.entries
				.sortedBy { it.key.name }
				.forEach {
					digest.add(it.key.name)
					digest.add(it.value.toString())
				}
			return File(cacheDir, digest.digest().toHexString() + ".jar")
		}

		private fun MessageDigest.add(str: String?) {
			str?.let { this.update(it.toByteArray()) }
		}

		private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
	}
}
