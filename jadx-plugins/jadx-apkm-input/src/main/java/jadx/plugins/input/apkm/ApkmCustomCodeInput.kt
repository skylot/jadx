package jadx.plugins.input.apkm

import jadx.api.plugins.input.ICodeLoader
import jadx.api.plugins.input.JadxCodeInput
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import java.io.File
import java.nio.file.Path

class ApkmCustomCodeInput(
	private val plugin: ApkmInputPlugin,
) : JadxCodeInput {
	override fun loadFiles(input: List<Path>): ICodeLoader {
		val apkFiles = mutableListOf<File>()
		for (file in input.map { it.toFile() }) {
			// Check if this is a valid APKM file
			val manifest = ApkmUtils.getManifest(file) ?: continue
			if (!ApkmUtils.isSupported(manifest)) continue

			// Load all files ending with .apk
			ZipSecurity.visitZipEntries<Any>(file) { zip, entry ->
				if (entry.name.endsWith(".apk")) {
					val tmpFile = ZipSecurity.getInputStreamForEntry(zip, entry).use {
						CommonFileUtils.saveToTempFile(it, ".apk").toFile()
					}
					apkFiles.add(tmpFile)
				}
				null
			}
		}

		val codeLoader = plugin.dexInputPlugin.loadFiles(apkFiles.map { it.toPath() })

		apkFiles.forEach { CommonFileUtils.safeDeleteFile(it) }

		return codeLoader
	}
}
