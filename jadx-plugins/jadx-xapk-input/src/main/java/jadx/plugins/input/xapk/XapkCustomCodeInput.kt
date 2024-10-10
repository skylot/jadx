package jadx.plugins.input.xapk

import jadx.api.plugins.input.ICodeLoader
import jadx.api.plugins.input.JadxCodeInput
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import jadx.core.utils.files.ZipFile
import java.io.File
import java.nio.file.Path

class XapkCustomCodeInput(
	private val plugin: XapkInputPlugin,
) : JadxCodeInput {
	override fun loadFiles(input: List<Path>): ICodeLoader {
		val apkFiles = mutableListOf<File>()
		for (file in input.map { it.toFile() }) {
			val manifest = XapkUtils.getManifest(file) ?: continue
			if (!XapkUtils.isSupported(manifest)) continue

			ZipFile(file).use { zip ->
				for (splitApk in manifest.splitApks) {
					val splitApkEntry = zip.getEntry(splitApk.file)
					if (splitApkEntry != null) {
						val tmpFile = ZipSecurity.getInputStreamForEntry(zip, splitApkEntry).use {
							CommonFileUtils.saveToTempFile(it, ".apk").toFile()
						}
						apkFiles.add(tmpFile)
					}
				}
			}
		}

		val codeLoader = plugin.dexInputPlugin.loadFiles(apkFiles.map { it.toPath() })

		apkFiles.forEach { CommonFileUtils.safeDeleteFile(it) }

		return codeLoader
	}
}
