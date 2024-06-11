package jadx.gui.update

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jadx.api.JadxDecompiler
import jadx.core.Jadx
import jadx.gui.update.data.Release
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object JadxUpdate {
    private val LOG: Logger = LoggerFactory.getLogger(JadxUpdate::class.java)

    const val JADX_RELEASES_URL: String = "https://github.com/skylot/jadx/releases"

    private const val GITHUB_API_URL = "https://api.github.com/"
    private const val GITHUB_LATEST_RELEASE_URL = GITHUB_API_URL + "repos/skylot/jadx/releases/latest"

    private val GSON = Gson()

    private val RELEASE_TYPE: Type = object : TypeToken<Release?>() {
    }.type

    @JvmStatic
	fun check(callback: IUpdateCallback) {
        val run = Runnable {
            try {
                val release = checkForNewRelease()
                if (release != null) {
                    callback.onUpdate(release)
                }
            } catch (e: Exception) {
                LOG.debug("Jadx update error", e)
            }
        }
        val thread = Thread(run)
        thread.name = "Jadx update thread"
        thread.priority = Thread.MIN_PRIORITY
        thread.start()
    }

    @Throws(IOException::class)
    private fun checkForNewRelease(): Release? {
        if (Jadx.isDevVersion()) {
            LOG.debug("Ignore check for update: development version")
            return null
        }
        val latest = get<Release>(GITHUB_LATEST_RELEASE_URL, RELEASE_TYPE)
            ?: return null
        val currentVersion = JadxDecompiler.getVersion()
        val latestName = latest.name
        if (latestName.equals(currentVersion, ignoreCase = true)) {
            return null
        }
        if (VersionComparator.checkAndCompare(currentVersion, latestName) >= 0) {
            return null
        }
        LOG.info("Found new jadx version: {}", latest)
        return latest
    }

    @Throws(IOException::class)
    private fun <T> get(url: String, type: Type): T? {
        val obj = URL(url)
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        if (con.responseCode == 200) {
            val reader: Reader = InputStreamReader(con.inputStream, StandardCharsets.UTF_8)
            return GSON.fromJson(reader, type)
        }
        return null
    }

    interface IUpdateCallback {
        fun onUpdate(r: Release?)
    }
}
