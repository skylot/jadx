package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestDeboxing extends IntegrationTest {

	public static class TestCls {
		public Object testInt() {
			return 1;
		}

		public Object testBoolean() {
			return true;
		}

		public Object testByte() {
			return (byte) 2;
		}

		public Short testShort() {
			return 3;
		}

		public Character testChar() {
			return 'c';
		}

		public Long testLong() {
			return 4L;
		}

		public void testConstInline() {
			Boolean v = true;
			use(v);
			use(v);
		}

		private void use(Boolean v) {
		}

		public void check() {
			// don't mind weird comparisons
			// need to get primitive without using boxing or literal
			// otherwise will get same result after decompilation
			assertThat(testInt(), is(Integer.sum(0, 1)));
			assertThat(testBoolean(), is(Boolean.TRUE));
			assertThat(testByte(), is(Byte.parseByte("2")));
			assertThat(testShort(), is(Short.parseShort("3")));
			assertThat(testChar(), is("c".charAt(0)));
			assertThat(testLong(), is(Long.valueOf("4")));
			testConstInline();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return 1;"));
		assertThat(code, containsOne("return true;"));
		assertThat(code, containsOne("return (byte) 2;"));
		assertThat(code, containsOne("return (short) 3;"));
		assertThat(code, containsOne("return 'c';"));
		assertThat(code, containsOne("return 4L;"));
		assertThat(code, countString(2, "use(true);"));
	}
}
