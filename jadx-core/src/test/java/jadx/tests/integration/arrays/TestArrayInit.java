package jadx.tests.integration.arrays;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayInit extends IntegrationTest {

	public static class TestCls {

		byte[] bytes;

		@SuppressWarnings("unused")
		public void test() {
			byte[] arr = new byte[]{10, 20, 30};
		}

		public void test2() {
			bytes = new byte[]{10, 20, 30};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("= {10, 20, 30};"));
		assertThat(code, containsString("this.bytes = new byte[]{10, 20, 30};"));
	}
}
