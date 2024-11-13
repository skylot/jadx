package jadx.gui.update

import jadx.gui.settings.JadxUpdateChannel
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Test updates fetch.
 * All tests disabled because of network requests, run manually on JadxUpdate changes
 */
@Disabled("Network requests")
class TestJadxUpdate {

	@Test
	fun testStableCheck() {
		JadxUpdate("1.5.0").checkForNewRelease(JadxUpdateChannel.STABLE)?.let {
			println("Latest release: $it")
		}
	}

	@Test
	fun testUnstableCheck() {
		JadxUpdate("r2000").checkForNewRelease(JadxUpdateChannel.UNSTABLE)?.let {
			println("Latest unstable: $it")
		}
	}
}
