package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
			write(a ? 0 : Byte.MAX_VALUE);
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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(a ? (byte) 0 : (byte) 1);"));
		assertThat(code, containsString("write(a ? (byte) 0 : this.myByte);"));
		assertThat(code, containsString("write(a ? (byte) 0 : Byte.MAX_VALUE);"));

		assertThat(code, containsString("write(a ? (short) 0 : (short) 1);"));
		assertThat(code, containsString("write(a ? this.myShort : (short) 0);"));
		assertThat(code, containsString("write(a ? Short.MIN_VALUE : (short) 0);"));
	}
}
