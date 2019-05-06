package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayFill3 extends IntegrationTest {

	public static class TestCls {

		public byte[] test(int a) {
			return new byte[] { 0, 1, 2 };
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		useEclipseCompiler();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new byte[]{0, 1, 2}"));
	}
}
