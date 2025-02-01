package jadx.gui.update

import com.google.gson.annotations.SerializedName
import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.core.Jadx
import jadx.core.plugins.versions.VersionComparator
import jadx.core.utils.GsonUtils.buildGson
import jadx.gui.settings.JadxUpdateChannel
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import kotlin.reflect.KClass

data class Release(var name: String = "")

data class ArtifactList(var artifacts: List<Artifact>? = null)

data class Artifact(
	var name: String = "",
	@SerializedName("workflow_run") var workflowRun: WorkflowRun? = null,
)

data class WorkflowRun(
	@SerializedName("head_branch") var branch: String = "",
)

interface IUpdateCallback {
	fun onUpdate(r: Release)
}

class JadxUpdate(private val jadxVersion: String = Jadx.getVersion()) {

	companion object {
		private val LOG = KotlinLogging.logger {}

		const val JADX_ARTIFACTS_URL = "https://nightly.link/skylot/jadx/workflows/build-artifacts/master"
		const val JADX_RELEASES_URL = "https://github.com/skylot/jadx/releases"

		private const val GITHUB_API_URL = "https://api.github.com/repos/skylot/jadx"
		private const val GITHUB_LATEST_ARTIFACTS_URL = "$GITHUB_API_URL/actions/artifacts?per_page=5&page=1"
		private const val GITHUB_LATEST_RELEASE_URL = "$GITHUB_API_URL/releases/latest"
	}

	fun check(updateChannel: JadxUpdateChannel, callback: IUpdateCallback) {
		if (jadxVersion == Jadx.VERSION_DEV) {
			LOG.debug { "Ignore update check: development version" }
			return
		}
		Thread {
			try {
				val release = checkForNewRelease(updateChannel)
				if (release != null) {
					callback.onUpdate(release)
				} else {
					LOG.info { "No updates found" }
				}
			} catch (e: Exception) {
				LOG.warn(e) { "Jadx update error" }
			}
		}.apply {
			name = "Jadx update thread"
			priority = Thread.MIN_PRIORITY
			start()
		}
	}

	fun checkForNewRelease(updateChannel: JadxUpdateChannel): Release? {
		LOG.info { "Checking for updates..." }
		LOG.info { "Update channel: $updateChannel, current version: $jadxVersion" }
		return when (updateChannel) {
			JadxUpdateChannel.STABLE -> checkForNewStableRelease()
			JadxUpdateChannel.UNSTABLE -> checkForNewUnstableRelease()
		}
	}

	private fun checkForNewStableRelease(): Release? {
		if (jadxVersion.startsWith("r")) {
			// current version is 'unstable', but update channel set to 'stable'
			LOG.info { "Skip update check: can't compare unstable and stable versions" }
			return null
		}
		val latestRelease = getAndParse(GITHUB_LATEST_RELEASE_URL, Release::class) ?: return null
		if (VersionComparator.checkAndCompare(jadxVersion, latestRelease.name) >= 0) return null
		LOG.info { "Found new jadx version: ${latestRelease.name}" }
		return latestRelease
	}

	private fun checkForNewUnstableRelease(): Release? {
		val artifacts = getAndParse(GITHUB_LATEST_ARTIFACTS_URL, ArtifactList::class)
			?.artifacts
			?.filter { it.workflowRun?.branch == "master" }
			?: return null
		if (artifacts.isEmpty()) return null

		val latestVersion = artifacts[0].name.removePrefix("jadx-gui-").removePrefix("jadx-").substringBefore('-')
		if (VersionComparator.checkAndCompare(jadxVersion, latestVersion) >= 0) return null
		LOG.info { "Found new unstable version: $latestVersion" }
		return Release(latestVersion)
	}

	private fun <T : Any> getAndParse(url: String, klass: KClass<T>): T? {
		val con = URI(url).toURL().openConnection() as? HttpURLConnection
		if (con == null || con.responseCode != 200) {
			return null
		}
		return con.inputStream.use { stream ->
			InputStreamReader(stream).use { reader ->
				buildGson().fromJson(reader, klass.java)
			}
		}
	}
}
