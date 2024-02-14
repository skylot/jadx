package jadx.plugins.input.xapk

import jadx.api.ResourceFile
import jadx.api.ResourcesLoader
import jadx.api.plugins.CustomResourcesLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import java.io.File

class XapkCustomResourcesLoader : CustomResourcesLoader {
	private val tmpFiles = mutableListOf<File>()

	override fun load(loader: ResourcesLoader, list: MutableList<ResourceFile>, file: File): Boolean {
		val manifest = XapkUtils.getManifest(file) ?: return false
		if (!XapkUtils.isSupported(manifest)) return false

		val apkEntries = manifest.splitApks.map { it.file }.toHashSet()
		ZipSecurity.visitZipEntries(file) { zip, entry ->
			if (apkEntries.contains(entry.name)) {
				val tmpFile = ZipSecurity.getInputStreamForEntry(zip, entry).use {
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
