package jadx.plugins.input.xapk

import jadx.core.utils.GsonUtils.buildGson
import jadx.core.utils.files.FileUtils
import jadx.zip.ZipReader
import java.io.File
import java.io.InputStreamReader

object XapkUtils {
	fun getManifest(file: File, zipReader: ZipReader): XapkManifest? {
		if (!FileUtils.isZipFile(file)) return null
		try {
			zipReader.open(file).use { zip ->
				val manifestEntry = zip.searchEntry("manifest.json") ?: return null
				return InputStreamReader(manifestEntry.inputStream).use {
					buildGson().fromJson(it, XapkManifest::class.java)
				}
			}
		} catch (e: Exception) {
			return null
		}
	}

	fun isSupported(manifest: XapkManifest): Boolean {
		return manifest.xapkVersion == 2 && manifest.splitApks.isNotEmpty()
	}
}
