package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestFloatValue extends IntegrationTest {

	public static class TestCls {
		public float[] method() {
			float[] fa = {0.55f};
			fa[0] /= 2;
			return fa;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("1073741824")));
		assertThat(code, containsString("0.55f"));
		assertThat(code, containsString("fa[0] = fa[0] / 2.0f;"));
	}
}
