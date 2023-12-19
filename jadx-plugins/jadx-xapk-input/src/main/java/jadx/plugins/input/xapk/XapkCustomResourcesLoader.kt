package jadx.plugins.input.xapk

import com.google.gson.Gson
import jadx.api.ResourceFile
import jadx.api.ResourcesLoader
import jadx.api.plugins.CustomResourcesLoader
import jadx.api.plugins.utils.CommonFileUtils
import jadx.api.plugins.utils.ZipSecurity
import jadx.core.utils.files.FileUtils
import java.io.File
import java.util.zip.ZipFile

class XapkCustomResourcesLoader : CustomResourcesLoader {
	override fun load(loader: ResourcesLoader, list: MutableList<ResourceFile>, file: File): Boolean {
		if (!FileUtils.isZipFile(file)) return false
		val manifest = try {
			ZipFile(file).use { zip ->
				val manifestEntry = zip.getEntry("manifest.json")
				if (manifestEntry != null) {
					val manifestString = ZipSecurity.getInputStreamForEntry(zip, manifestEntry).use { it.readBytes().toString(Charsets.UTF_8) }
					Gson().fromJson(manifestString, XapkManifest::class.java)
				} else {
					null
				}
			}
		} catch (e: Exception) {
			null
		}

		if (manifest == null || manifest.xapkVersion != 2 || manifest.splitApks.isEmpty()) {
			if (manifest != null && manifest.xapkVersion != 2) {
				println("Unsupported XAPK version: ${manifest.xapkVersion}")
				// TODO: Change println to logger
			}
			return false
		}

		ZipFile(file).use { zip ->
			for (splitApk in manifest.splitApks) {
				val splitApkEntry = zip.getEntry(splitApk.file)
				if (splitApkEntry != null) {
					val tmpFile = ZipSecurity.getInputStreamForEntry(zip, splitApkEntry).use {
						CommonFileUtils.saveToTempFile(it, ".zip").toFile()
					}
					loader.defaultLoadFile(list, tmpFile)
					// TODO: decide how to handle tmpFile deletion
//					CommonFileUtils.safeDeleteFile(tmpFile)
				}
			}
		}

		return true
	}
}
