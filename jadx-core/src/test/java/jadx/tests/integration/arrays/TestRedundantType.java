package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRedundantType extends IntegrationTest {

	public static class TestCls {

		public byte[] method() {
			return new byte[] { 10, 11, 12 };
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new byte[]{10, 11, 12};"));
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
		ClassNode cls = getClassNode(TestByte.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("arr[10] = 126"));
		assertThat(code, containsString("arr[20] = Byte.MAX_VALUE"));
		assertThat(code, containsString("arr[30] = Byte.MIN_VALUE"));
		assertThat(code, containsString("arr[40] = -127"));
		assertEquals(-127, new TestByte().method()[40]);
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
		ClassNode cls = getClassNode(TestShort.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("arr[10] = 32766"));
		assertThat(code, containsString("arr[20] = Short.MAX_VALUE"));
		assertThat(code, containsString("arr[30] = Short.MIN_VALUE"));
		assertThat(code, containsString("arr[40] = -32767"));
		assertEquals(-32767, new TestShort().method()[40]);
	}
}
