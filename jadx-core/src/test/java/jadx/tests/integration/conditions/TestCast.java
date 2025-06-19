package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestCast extends IntegrationTest {

	public static class TestCls {

		byte myByte;
		short myShort;

		public void test1(boolean a) {
			write(a ? (byte) 0 : 1);
		}

		public void test2(boolean a) {
			write(a ? 0 : myByte);
		}

		public void test3(boolean a) {
			write(a ? 0 : (byte) 127);
		}

		public void test4(boolean a) {
			write(a ? (short) 0 : 1);
		}

		public void test5(boolean a) {
			write(a ? myShort : 0);
		}

		public void test6(boolean a) {
			write(a ? Short.MIN_VALUE : 0);
		}

		public void write(byte b) {
		}

		public void write(short b) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("write(a ? (byte) 0 : (byte) 1);")
				.contains("write(a ? (byte) 0 : this.myByte);")
				.contains("write(a ? (byte) 0 : (byte) 127);")
				.contains("write(a ? (short) 0 : (short) 1);")
				.contains("write(a ? this.myShort : (short) 0);")
				.contains("write(a ? Short.MIN_VALUE : (short) 0);");
	}
}
