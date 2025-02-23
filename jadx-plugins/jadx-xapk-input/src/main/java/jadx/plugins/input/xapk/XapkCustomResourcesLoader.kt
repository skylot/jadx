package jadx.plugins.input.xapk

import jadx.api.ResourceFile
import jadx.api.ResourcesLoader
import jadx.api.plugins.CustomResourcesLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.zip.ZipReader
import java.io.File

class XapkCustomResourcesLoader(private val zipReader: ZipReader) : CustomResourcesLoader {
	private val tmpFiles = mutableListOf<File>()

	override fun load(loader: ResourcesLoader, list: MutableList<ResourceFile>, file: File): Boolean {
		if (!file.name.endsWith(".xapk")) return false

		val manifest = XapkUtils.getManifest(file, zipReader) ?: return false
		if (!XapkUtils.isSupported(manifest)) return false

		val apkEntries = manifest.splitApks.map { it.file }.toHashSet()
		zipReader.visitEntries(file) { entry ->
			if (apkEntries.contains(entry.name)) {
				val tmpFile = entry.inputStream.use {
					CommonFileUtils.saveToTempFile(it, ".apk").toFile()
				}
				loader.defaultLoadFile(list, tmpFile, entry.name + "/")
				tmpFiles += tmpFile
			} else {
				loader.addEntry(list, file, entry, "")
			}
			null
		}
		return true
	}

	override fun close() {
		tmpFiles.forEach(CommonFileUtils::safeDeleteFile)
		tmpFiles.clear()
	}
}
