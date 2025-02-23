package jadx.plugins.input.xapk

import jadx.api.plugins.input.ICodeLoader
import jadx.api.plugins.input.JadxCodeInput
import jadx.api.plugins.utils.CommonFileUtils
import jadx.plugins.input.dex.DexInputPlugin
import jadx.zip.ZipReader
import java.io.File
import java.nio.file.Path

class XapkCustomCodeInput(
	private val dexInputPlugin: DexInputPlugin,
	private val zipReader: ZipReader,
) : JadxCodeInput {

	override fun loadFiles(input: List<Path>): ICodeLoader {
		val apkFiles = mutableListOf<File>()
		for (file in input.map { it.toFile() }) {
			if (!file.name.endsWith(".xapk")) continue

			val manifest = XapkUtils.getManifest(file, zipReader) ?: continue
			if (!XapkUtils.isSupported(manifest)) continue

			zipReader.open(file).use { zip ->
				for (splitApk in manifest.splitApks) {
					val splitApkEntry = zip.searchEntry(splitApk.file)
					if (splitApkEntry != null) {
						val tmpFile = splitApkEntry.inputStream.use {
							CommonFileUtils.saveToTempFile(it, ".apk").toFile()
						}
						apkFiles.add(tmpFile)
					}
				}
			}
		}

		val codeLoader = dexInputPlugin.loadFiles(apkFiles.map { it.toPath() })

		apkFiles.forEach { CommonFileUtils.safeDeleteFile(it) }

		return codeLoader
	}
}
