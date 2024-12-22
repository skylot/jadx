package jadx.plugins.input.apkm

import jadx.api.plugins.utils.ZipSecurity
import jadx.core.utils.GsonUtils.buildGson
import jadx.core.utils.files.FileUtils
import jadx.core.utils.files.ZipFile
import java.io.File
import java.io.InputStreamReader

object ApkmUtils {
	fun getManifest(file: File): ApkmManifest? {
		if (!FileUtils.isZipFile(file)) return null
		try {
			ZipFile(file).use { zip ->
				val manifestEntry = zip.getEntry("info.json") ?: return null
				return InputStreamReader(ZipSecurity.getInputStreamForEntry(zip, manifestEntry)).use {
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
