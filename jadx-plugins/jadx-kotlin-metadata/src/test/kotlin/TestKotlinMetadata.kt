package jadx.plugins.kotlin.metadata.tests

import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.CLASS_ALIAS_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.COMPANION_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.DATA_CLASS_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.FIELDS_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.GETTERS_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.METHOD_ARGS_OPT
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions.Companion.TO_STRING_OPT
import jadx.tests.api.SmaliTest
import jadx.tests.api.utils.assertj.JadxAssertions.assertThat
import jadx.tests.api.utils.assertj.JadxCodeAssertions
import org.junit.jupiter.api.Test

class TestKotlinMetadata : SmaliTest() {
	// @formatter:off
	/*
		package deobf

		data class DataClassSample(
			val name: String,
			private val id: Int,
		) {
			var inner: Short = 3

			companion object {
				fun getTag(): String {
					return "TAG"
				}
			}
		}
	*/
	// @formatter:on

	@Test
	fun testMethodArgs() {
		setupArgs { this[METHOD_ARGS_OPT] = true }
		assertThatClass()
			.containsOne("public boolean equals(Object other) {")
	}

	@Test
	fun testIgnoreMethodArgs() {
		setupArgs()
		assertThatClass()
			.containsOne("public boolean equals(Object obj) {")
	}

	@Test
	fun testFields() {
		setupArgs { this[FIELDS_OPT] = true }
		assertThatClass()
			.containsOne("private final String name;")
			.containsOne("private final int id;")
			.containsOne("private short inner;")
			.countString(3, "reason: from kotlin metadata")
	}

	@Test
	fun testIgnoreFields() {
		setupArgs()
		assertThatClass()
			.containsOne("private final String a;")
			.containsOne("private final int b;")
			.containsOne("private short c;")
			.countString(0, "reason: from kotlin metadata")
	}

	@Test
	fun testCompanion() {
		setupArgs { this[COMPANION_OPT] = true }
		assertThatClass()
			.containsOne("public static final Companion INSTANCE = new Companion(null);")
			.containsOne("public static final class Companion {")
			.countString(2, "reason: from kotlin metadata")
	}

	@Test
	fun testIgnoreCompanion() {
		setupArgs()
		assertThatClass()
			.containsOne("public static final b d = new b(null);")
			.containsOne("public static final class b {")
			.countString(0, "reason: from kotlin metadata")
	}

	@Test
	fun testDataClass() {
		setupArgs { this[DATA_CLASS_OPT] = true }
		assertThatClass()
			.containsOne("/* data */")
	}

	@Test
	fun testIgnoreDataClass() {
		setupArgs()
		assertThatClass()
			.countString(0, "/* data */")
	}

	@Test
	fun testToString() {
		setupArgs { this[TO_STRING_OPT] = true }
		assertThatClass()
			.containsOne("public final class DataClassSample {")
			.containsOne("private final String name;")
			.containsOne("private final int id;")
			.countString(3, "reason: from toString")
	}

	@Test
	fun testIgnoreToString() {
		setupArgs()
		assertThatClass()
			.containsOne("public final class a {")
			.containsOne("private final String a;")
			.containsOne("private final int b;")
			.countString(0, "reason: from toString")
	}

	@Test
	fun testGetters() {
		setupArgs { this[GETTERS_OPT] = true }
		assertThatClass()
			.containsOne("public final String getA() {")
			.countString(1, "reason: from getter")
	}

	@Test
	fun testGettersAlias() {
		setupArgs {
			this[FIELDS_OPT] = true
			this[GETTERS_OPT] = true
		}
		assertThatClass()
			.containsOne("public final String getName() {")
			.countString(1, "reason: from getter")
	}

	@Test
	fun testIgnoreGetters() {
		setupArgs()
		assertThatClass()
			.countString(0, "reason: from getter")
	}

	private fun setupArgs(builder: MutableMap<String, Boolean>.() -> Unit = {}) {
		val allOff = mutableMapOf(
			CLASS_ALIAS_OPT to false,
			METHOD_ARGS_OPT to false,
			FIELDS_OPT to false,
			COMPANION_OPT to false,
			DATA_CLASS_OPT to false,
			TO_STRING_OPT to false,
			GETTERS_OPT to false,
		)
		args.pluginOptions = allOff.apply(builder).mapValues {
			if (it.value) "yes" else "no"
		}
	}

	private fun assertThatClass(): JadxCodeAssertions =
		assertThat(getClassNodeFromSmaliFiles("deobf", "TestKotlinMetadata", "a"))
			.code()
}
