package jadx.gui.update

import com.google.gson.Gson
import com.google.gson.JsonParser
import jadx.api.JadxDecompiler
import jadx.core.Jadx
import jadx.gui.settings.JadxUpdateChannel
import jadx.gui.update.data.Artifact
import jadx.gui.update.data.Release
import org.jetbrains.kotlin.konan.file.use
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object JadxUpdate {
	private val LOG: Logger = LoggerFactory.getLogger(JadxUpdate::class.java)

	const val JADX_ARTIFACTS_URL = "https://nightly.link/skylot/jadx/workflows/build-artifacts/master"
	const val JADX_RELEASES_URL = "https://github.com/skylot/jadx/releases"

	private const val GITHUB_API_URL = "https://api.github.com/repos/skylot/jadx"
	private const val GITHUB_ARTIFACTS_URL = "$GITHUB_API_URL/actions/artifacts"
	private const val GITHUB_LATEST_RELEASE_URL = "$GITHUB_API_URL/releases/latest"

	@JvmStatic
	fun check(updateChannel: JadxUpdateChannel, callback: IUpdateCallback) {
		Thread {
			try {
				val release = checkForNewRelease(updateChannel)
				if (release != null) {
					callback.onUpdate(release)
				}
			} catch (e: Exception) {
				LOG.debug("Jadx update error", e)
			}
		}.apply {
			name = "Jadx update thread"
			priority = Thread.MIN_PRIORITY
			start()
		}
	}

	private fun checkForNewRelease(updateChannel: JadxUpdateChannel): Release? {
		if (Jadx.isDevVersion()) {
			LOG.debug("Ignore check for update: development version")
			return null
		}

		LOG.info("Checking for updates... Update channel: {}, current version: {}", updateChannel, JadxDecompiler.getVersion())

		return when (updateChannel) {
			JadxUpdateChannel.STABLE -> checkForNewStableRelease()
			JadxUpdateChannel.UNSTABLE -> checkForNewUnstableRelease()
		}
	}

	private fun checkForNewStableRelease(): Release? {
		val latestRelease = get(GITHUB_LATEST_RELEASE_URL)?.let { inputStream ->
			InputStreamReader(inputStream).use {
				Gson().fromJson(it, Release::class.java)
			}
		} ?: return null

		val currentVersion = JadxDecompiler.getVersion()

		if (currentVersion.equals(latestRelease.name, ignoreCase = true)) return null
		if (VersionComparator.checkAndCompare(currentVersion, latestRelease.name) >= 0) return null

		LOG.info("Found new jadx version: {}", latestRelease)

		return latestRelease
	}

	private fun checkForNewUnstableRelease(): Release? {
		val artifacts = getArtifacts() ?: return null

		val currentVersion = JadxDecompiler.getVersion()
		val currentArtifactName = "jadx-$currentVersion"

		var newestArtifact: Artifact? = null
		var currentArtifact: Artifact? = null

		for (artifact in artifacts) {
			if (newestArtifact == null && artifact.name.startsWith("jadx-") && !artifact.name.startsWith("jadx-gui-")) {
				newestArtifact = artifact
			}
			if (currentArtifact == null && artifact.name == currentArtifactName) {
				currentArtifact = artifact
			}
			if (newestArtifact != null && currentArtifact != null) break
		}

		LOG.debug("Current artifact: {}, newest artifact: {}", currentArtifact, newestArtifact)

		return if (currentArtifact != null && newestArtifact != null && newestArtifact.createdAt > currentArtifact.createdAt) {
			newestArtifact.let { Release().apply { name = it.name } }
		} else {
			null
		}
	}

	private fun getArtifacts(): List<Artifact>? {
		return get(GITHUB_ARTIFACTS_URL)?.let { inputStream ->
			InputStreamReader(inputStream).use { reader ->
				val response = JsonParser.parseReader(reader).asJsonObject

				val count = response.get("total_count").asInt
				LOG.debug("Fetched $count artifacts...")

				response.getAsJsonArray("artifacts").map {
					val obj = it.asJsonObject
					val name = obj.get("name").asString
					val sizeInBytes = obj.get("size_in_bytes").asLong
					val createdAt = obj.get("created_at").asString
					val parsedCreatedAt = ZonedDateTime.parse(createdAt, DateTimeFormatter.ISO_ZONED_DATE_TIME)
					Artifact(name, sizeInBytes, Date.from(parsedCreatedAt.toInstant()))
				}
			}
		}
	}

	private fun get(url: String): InputStream? {
		val con = URL(url).openConnection() as HttpURLConnection
		return if (con.responseCode == 200) con.inputStream else null
	}

	interface IUpdateCallback {
		fun onUpdate(r: Release)
	}
}
