package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestTypeResolver9 extends IntegrationTest {

	public static class TestCls {
		public int test(byte b) {
			return 16777216 * b;
		}

		public int test2(byte[] array, int offset) {
			return (array[offset] * 128) + (array[offset + 1] & 0xFF);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return 16777216 * b;"));
		assertThat(code, not(containsString("Byte.MIN_VALUE")));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
