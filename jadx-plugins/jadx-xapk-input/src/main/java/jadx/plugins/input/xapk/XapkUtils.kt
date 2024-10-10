package jadx.plugins.input.xapk

import com.google.gson.Gson
import jadx.api.plugins.utils.ZipSecurity
import jadx.core.utils.files.FileUtils
import jadx.core.utils.files.ZipFile
import java.io.File
import java.io.InputStreamReader

object XapkUtils {
	fun getManifest(file: File): XapkManifest? {
		if (!FileUtils.isZipFile(file)) return null
		try {
			ZipFile(file).use { zip ->
				val manifestEntry = zip.getEntry("manifest.json") ?: return null
				return InputStreamReader(ZipSecurity.getInputStreamForEntry(zip, manifestEntry)).use {
					Gson().fromJson(it, XapkManifest::class.java)
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
