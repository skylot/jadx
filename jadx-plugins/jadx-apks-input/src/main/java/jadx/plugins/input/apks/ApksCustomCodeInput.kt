package jadx.plugins.input.apks

import jadx.api.plugins.input.ICodeLoader
import jadx.api.plugins.input.JadxCodeInput
import jadx.api.plugins.utils.CommonFileUtils
import jadx.plugins.input.dex.DexInputPlugin
import jadx.zip.ZipReader
import java.io.File
import java.nio.file.Path

class ApksCustomCodeInput(
	private val dexInputPlugin: DexInputPlugin,
	private val zipReader: ZipReader,
) : JadxCodeInput {
	override fun loadFiles(input: List<Path>): ICodeLoader {
		val apkFiles = mutableListOf<File>()
		for (file in input.map { it.toFile() }) {
			if (!file.name.endsWith(".apks")) continue

			// Load all files ending with .apk
			zipReader.visitEntries<Any>(file) { entry ->
				if (entry.name.endsWith(".apk")) {
					val tmpFile = entry.inputStream.use {
						CommonFileUtils.saveToTempFile(it, ".apk").toFile()
					}
					apkFiles.add(tmpFile)
				}
				null
			}
		}

		val codeLoader = dexInputPlugin.loadFiles(apkFiles.map { it.toPath() })

		apkFiles.forEach { CommonFileUtils.safeDeleteFile(it) }

		return codeLoader
	}
}
