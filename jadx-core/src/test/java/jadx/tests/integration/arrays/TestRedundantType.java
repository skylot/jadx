package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRedundantType extends IntegrationTest {

	public static class TestCls {

		public byte[] method() {
			return new byte[] { 10, 11, 12 };
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return new byte[]{10, 11, 12};");
	}

	public static class TestByte {

		public byte[] method() {
			byte[] arr = new byte[50];
			arr[10] = 126;
			arr[20] = 127;
			arr[30] = (byte) 128;
			arr[40] = (byte) 129;
			return arr;
		}
	}

	@Test
	public void testByte() {
		JadxAssertions.assertThat(getClassNode(TestByte.class))
				.code()
				.contains("arr[10] = 126")
				.contains("arr[20] = Byte.MAX_VALUE")
				.contains("arr[30] = Byte.MIN_VALUE")
				.contains("arr[40] = -127");
		assertThat(new TestByte().method()[40]).isEqualTo((byte) -127);
	}

	public static class TestShort {

		public short[] method() {
			short[] arr = new short[50];
			arr[10] = 32766;
			arr[20] = 32767;
			arr[30] = (short) 32768;
			arr[40] = (short) 32769;
			return arr;
		}
	}

	@Test
	public void testShort() {
		JadxAssertions.assertThat(getClassNode(TestShort.class))
				.code()
				.contains("arr[10] = 32766")
				.contains("arr[20] = Short.MAX_VALUE")
				.contains("arr[30] = Short.MIN_VALUE")
				.contains("arr[40] = -32767");
		assertThat(new TestShort().method()[40]).isEqualTo((short) -32767);
	}
}
