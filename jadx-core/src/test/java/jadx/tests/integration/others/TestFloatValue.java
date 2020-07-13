package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFloatValue extends IntegrationTest {

	public static class TestCls {
		public float[] method() {
			float[] fa = { 0.55f };
			fa[0] /= 2;
			return fa;
		}

		public void check() {
			assertEquals(0.275f, method()[0], 0.0001f);
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
