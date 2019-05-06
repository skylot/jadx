package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestGenerics7 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			declare(String.class);
		}

		public <T> T declare(Class<T> cls) {
			return null;
		}

		public void declare(Object cls) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("declare(String.class);"));
	}
}
