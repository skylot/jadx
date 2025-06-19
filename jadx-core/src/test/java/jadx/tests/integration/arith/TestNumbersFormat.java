package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.api.args.IntegerFormat;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNumbersFormat extends IntegrationTest {

	@SuppressWarnings({ "FieldCanBeLocal", "UnusedAssignment", "unused" })
	public static class TestCls {
		private Object obj;

		public void test() {
			obj = new byte[] { 0, -1, -0xA, (byte) 0xff, Byte.MIN_VALUE, Byte.MAX_VALUE };
			obj = new short[] { 0, -1, -0xA, (short) 0xffff, Short.MIN_VALUE, Short.MAX_VALUE };
			obj = new int[] { 0, -1, -0xA, 0xffff_ffff, Integer.MIN_VALUE, Integer.MAX_VALUE };
			obj = new long[] { 0, -1, -0xA, 0xffff_ffff_ffff_ffffL, Long.MIN_VALUE, Long.MAX_VALUE };
		}
	}

	@Test
	public void test() {
		getArgs().setIntegerFormat(IntegerFormat.AUTO);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("new byte[]{0, -1, -10, -1, -128, 127}")
				.containsOne("new short[]{0, -1, -10, -1, Short.MIN_VALUE, Short.MAX_VALUE}")
				.containsOne("new int[]{0, -1, -10, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}")
				.containsOne("new long[]{0, -1, -10, -1, Long.MIN_VALUE, Long.MAX_VALUE}");
	}

	@Test
	public void testDecimalFormat() {
		getArgs().setIntegerFormat(IntegerFormat.DECIMAL);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("new byte[]{0, -1, -10, -1, -128, 127}")
				.containsOne("new short[]{0, -1, -10, -1, -32768, 32767}")
				.containsOne("new int[]{0, -1, -10, -1, -2147483648, 2147483647}")
				.containsOne("new long[]{0, -1, -10, -1, -9223372036854775808L, 9223372036854775807L}");
	}

	@Test
	public void testHexFormat() {
		getArgs().setIntegerFormat(IntegerFormat.HEXADECIMAL);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("new byte[]{0x0, (byte) 0xff, (byte) 0xf6, (byte) 0xff, (byte) 0x80, 0x7f}")
				.containsOne("new short[]{0x0, (short) 0xffff, (short) 0xfff6, (short) 0xffff, (short) 0x8000, 0x7fff}")
				.containsOne("new int[]{0x0, (int) 0xffffffff, (int) 0xfffffff6, (int) 0xffffffff, (int) 0x80000000, 0x7fffffff}")
				.containsOne(
						"new long[]{0x0, 0xffffffffffffffffL, 0xfffffffffffffff6L, 0xffffffffffffffffL, 0x8000000000000000L, 0x7fffffffffffffffL}");
	}
}
