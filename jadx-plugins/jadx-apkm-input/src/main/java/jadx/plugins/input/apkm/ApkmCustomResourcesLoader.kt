package jadx.plugins.input.apkm

import jadx.api.ResourceFile
import jadx.api.ResourcesLoader
import jadx.api.plugins.CustomResourcesLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import java.io.File

class ApkmCustomResourcesLoader : CustomResourcesLoader {
	private val tmpFiles = mutableListOf<File>()

	override fun load(loader: ResourcesLoader, list: MutableList<ResourceFile>, file: File): Boolean {
		// Check if this is a valid APKM file
		val manifest = ApkmUtils.getManifest(file) ?: return false
		if (!ApkmUtils.isSupported(manifest)) return false

		// Load all files ending with .apk
		ZipSecurity.visitZipEntries<Any>(file) { zip, entry ->
			if (entry.name.endsWith(".apk")) {
				val tmpFile = ZipSecurity.getInputStreamForEntry(zip, entry).use {
					CommonFileUtils.saveToTempFile(it, ".apk").toFile()
				}
				loader.defaultLoadFile(list, tmpFile, entry.name + "/")
				tmpFiles += tmpFile
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
