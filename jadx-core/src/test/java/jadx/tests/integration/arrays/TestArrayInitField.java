package jadx.tests.integration.arrays;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayInitField extends IntegrationTest {

	public static class TestCls {

		static byte[] a = new byte[]{10, 20, 30};
		byte[] b = new byte[]{40, 50, 60};
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("= {10, 20, 30};"));
		assertThat(code, containsString("= {40, 50, 60};"));
	}
}
