package jadx.plugins.kotlin.metadata.tests

import jadx.tests.api.IntegrationTest
import jadx.tests.api.utils.assertj.JadxAssertions.assertThat
import org.junit.jupiter.api.Test

class TestJavaParser : IntegrationTest() {

	@Test
	fun test() {
		val sampleCls = getResourceFile("samples/MainKt.class")
		assertThat(getClassNodeFromFiles(listOf(sampleCls), "MainKt"))
			.code()
			.doesNotContain("Exception occurred when reading Kotlin metadata")
	}
}
