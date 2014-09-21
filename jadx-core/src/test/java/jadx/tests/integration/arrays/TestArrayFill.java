package jadx.tests.integration.arrays;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestArrayFill extends IntegrationTest {

	public static class TestCls {

		public String[] method() {
			return new String[]{"1", "2", "3"};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("return new String[]{\"1\", \"2\", \"3\"};"));
	}
}
