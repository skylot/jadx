package jadx.plugins.input.xapk

import jadx.api.plugins.input.ICodeLoader
import jadx.api.plugins.input.JadxCodeInput
import jadx.api.plugins.input.data.impl.EmptyCodeLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

class XapkCustomCodeInput(
	private val plugin: XapkInputPlugin,
) : JadxCodeInput {
	override fun loadFiles(input: List<Path>): ICodeLoader {
		val dexInputPlugin = plugin.dexInputPlugin ?: return EmptyCodeLoader.INSTANCE

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

		val codeLoader = dexInputPlugin.loadFiles(apkFiles.map { it.toPath() })

		apkFiles.forEach { CommonFileUtils.safeDeleteFile(it) }

		return codeLoader
	}
}
