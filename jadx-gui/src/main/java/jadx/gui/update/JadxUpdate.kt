package jadx.gui.update

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.JadxDecompiler
import jadx.core.Jadx
import jadx.gui.settings.JadxUpdateChannel
import org.jetbrains.kotlin.konan.file.use
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import kotlin.reflect.KClass

data class Release(val name: String)

data class ArtifactList(val artifacts: List<Artifact>)

data class Artifact(
	val name: String,
	@SerializedName("workflow_run") val workflowRun: WorkflowRun,
)

data class WorkflowRun(
	@SerializedName("head_branch") val branch: String,
)

interface IUpdateCallback {
	fun onUpdate(r: Release)
}

object JadxUpdate {
	private val log = KotlinLogging.logger {}

	const val JADX_ARTIFACTS_URL = "https://nightly.link/skylot/jadx/workflows/build-artifacts/master"
	const val JADX_RELEASES_URL = "https://github.com/skylot/jadx/releases"

	private const val GITHUB_API_URL = "https://api.github.com/repos/skylot/jadx"
	private const val GITHUB_LATEST_ARTIFACTS_URL = "$GITHUB_API_URL/actions/artifacts?per_page=5&page=1"
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
				log.warn(e) { "Jadx update error" }
			}
		}.apply {
			name = "Jadx update thread"
			priority = Thread.MIN_PRIORITY
			start()
		}
	}

	private fun checkForNewRelease(updateChannel: JadxUpdateChannel): Release? {
		if (Jadx.isDevVersion()) {
			log.debug { "Ignore check for update: development version" }
			return null
		}
		log.info {
			"Checking for updates... Update channel: $updateChannel, current version: ${JadxDecompiler.getVersion()}"
		}
		return when (updateChannel) {
			JadxUpdateChannel.STABLE -> checkForNewStableRelease()
			JadxUpdateChannel.UNSTABLE -> checkForNewUnstableRelease()
		}
	}

	private fun checkForNewStableRelease(): Release? {
		val currentVersion = JadxDecompiler.getVersion()
		if (currentVersion.startsWith("r")) {
			// current version is 'unstable', but update channel set to 'stable'
			log.info { "Skip update check: can't compare unstable and stable versions" }
			return null
		}
		val latestRelease = getAndParse(GITHUB_LATEST_RELEASE_URL, Release::class) ?: return null
		if (VersionComparator.checkAndCompare(currentVersion, latestRelease.name) >= 0) return null
		log.info { "Found new jadx version: ${latestRelease.name}" }
		return latestRelease
	}

	private fun checkForNewUnstableRelease(): Release? {
		val artifacts = getAndParse(GITHUB_LATEST_ARTIFACTS_URL, ArtifactList::class)
			?.artifacts
			?.filter { it.workflowRun.branch == "master" }
			?: return null
		if (artifacts.isEmpty()) return null

		val latestVersion = artifacts[0].name.removePrefix("jadx-gui-").removePrefix("jadx-").substringBefore('-')
		if (VersionComparator.checkAndCompare(JadxDecompiler.getVersion(), latestVersion) >= 0) return null
		log.info { "Found new unstable version: $latestVersion" }
		return Release(latestVersion)
	}

	private fun <T : Any> getAndParse(url: String, klass: KClass<T>): T? {
		val con = URI(url).toURL().openConnection() as? HttpURLConnection
		if (con == null || con.responseCode != 200) {
			return null
		}
		return con.inputStream.use { stream ->
			InputStreamReader(stream).use { reader ->
				Gson().fromJson(reader, klass.java)
			}
		}
	}
}
