package jadx.plugins.input.apkm

import jadx.api.plugins.utils.ZipSecurity
import jadx.core.utils.GsonUtils.buildGson
import jadx.core.utils.files.FileUtils
import jadx.zip.ZipReader
import java.io.File
import java.io.InputStreamReader

object ApkmUtils {
	fun getManifest(file: File): ApkmManifest? {
		if (!FileUtils.isZipFile(file)) return null
		try {
			ZipReader.open(file).use { zip ->
				val manifestEntry = zip.searchEntry("info.json") ?: return null
				return InputStreamReader(ZipSecurity.getInputStreamForEntry(manifestEntry)).use {
					buildGson().fromJson(it, ApkmManifest::class.java)
				}
			}
		} catch (e: Exception) {
			return null
		}
	}

	fun isSupported(manifest: ApkmManifest): Boolean {
		return manifest.apkmVersion != -1
	}
}
