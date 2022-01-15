package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrimitiveCasts extends IntegrationTest {

	public static class TestCls {

		public void test() {
			useShort((short) 0);
			useShort((short) getInt());
			useByte((byte) 0);
			useByte((byte) getInt());
			useChar((char) 0);
			useChar((char) getInt());

			useShort((short) 0L);
			useShort((short) getLong());
			useByte((byte) 0L);
			useByte((byte) getLong());
			useChar((char) 0L);
			useChar((char) getLong());

			useShort((short) ' ');
			useShort((short) getChar());
			useByte((byte) ' ');
			useByte((byte) getChar());

			useInt((byte) 7);
			useInt((char) ' ');
			useInt(getChar());
			useInt((int) 2L);
			useInt((int) getLong());
		}

		private long getLong() {
			return 1L;
		}

		private char getChar() {
			return ' ';
		}

		private int getInt() {
			return 1;
		}

		private void useChar(char c) {
		}

		private void useByte(byte b) {
		}

		private void useShort(short s) {
		}

		private void useInt(int i) {
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("(0)")
				.doesNotContain(") ((int) getLong())");
	}
}
