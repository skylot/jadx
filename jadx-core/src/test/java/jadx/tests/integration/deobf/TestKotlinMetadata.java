package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestKotlinMetadata extends SmaliTest {
	// @formatter:off
	/*
		@file:JvmName("TestKotlinMetadata")
		class TestMetaData {

			@JvmField
			val id = 1

			@JvmName("makeTwo")
			fun double(x: Int): Int {
				return 2 * x
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		prepareArgs(true);
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("class TestMetaData {")
				.containsOne("reason: from Kotlin metadata");
	}

	@Test
	public void testIgnoreMetadata() {
		prepareArgs(false);
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("class C0000TestKotlinMetadata {");
	}

	private void prepareArgs(boolean parseKotlinMetadata) {
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		getArgs().setParseKotlinMetadata(parseKotlinMetadata);
		disableCompilation();
	}
}
