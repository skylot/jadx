package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrimitiveConversion extends SmaliTest {
	// @formatter:off
	/*
		public void test(long j, boolean z) {
			putByte(j, z ? (byte) 1 : (byte) 0);
		}

		private static void putByte(long j, byte z) {
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("putByte(j, z);")
				.containsOne("putByte(j, z ? (byte) 1 : (byte) 0);");
	}
}
