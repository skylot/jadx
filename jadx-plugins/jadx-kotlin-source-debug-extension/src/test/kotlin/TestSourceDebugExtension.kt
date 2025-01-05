package jadx.plugins.kotlin.metadata.tests

import jadx.plugins.kotlin.smap.KotlinSmapOptions.Companion.CLASS_ALIAS_SOURCE_DBG_OPT
import jadx.tests.api.SmaliTest
import jadx.tests.api.utils.assertj.JadxAssertions.assertThat
import jadx.tests.api.utils.assertj.JadxCodeAssertions
import org.junit.jupiter.api.Test

class TestSourceDebugExtension : SmaliTest() {

	@Test
	fun testRenameClass() {
		setupArgs {
			this[CLASS_ALIAS_SOURCE_DBG_OPT] = true
		}
		assertThatClass()
			.containsOne("androidx.compose.ui")
			.containsOne("public final class ActualKt")
			.countString(1, "reason: from SourceDebugExtension")
	}

	private fun setupArgs(builder: MutableMap<String, Boolean>.() -> Unit = {}) {
		val allOff = mutableMapOf(
			CLASS_ALIAS_SOURCE_DBG_OPT to false,
		)
		args.pluginOptions = allOff.apply(builder).mapValues {
			if (it.value) "yes" else "no"
		}
	}

	private fun assertThatClass(): JadxCodeAssertions =
		assertThat(getClassNodeFromSmaliFiles("deobf", "TestKotlinSourceDebugExtension", "C6"))
			.code()
}
