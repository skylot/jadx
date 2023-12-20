package jadx.plugins.input.xapk

import jadx.api.ResourceFile
import jadx.api.ResourcesLoader
import jadx.api.plugins.CustomResourcesLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import java.io.File
import java.util.zip.ZipFile

class XapkCustomResourcesLoader : CustomResourcesLoader {
	override fun load(loader: ResourcesLoader, list: MutableList<ResourceFile>, file: File): Boolean {
		val manifest = XapkUtils.getManifest(file) ?: return false
		if (!XapkUtils.isSupported(manifest)) return false

		ZipFile(file).use { zip ->
			for (splitApk in manifest.splitApks) {
				val splitApkEntry = zip.getEntry(splitApk.file)
				if (splitApkEntry != null) {
					val tmpFile = ZipSecurity.getInputStreamForEntry(zip, splitApkEntry).use {
						CommonFileUtils.saveToTempFile(it, ".apk").toFile()
					}
					loader.defaultLoadFile(list, tmpFile)
					// TODO: how to handle tmpFile deletion?
// 					CommonFileUtils.safeDeleteFile(tmpFile)
				}
			}
		}

		return true
	}
}
