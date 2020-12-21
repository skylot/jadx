package jadx.tests.integration.rename;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstReplace extends IntegrationTest {

	public static class TestCls {
		public static final String CONST = "SOME_CONST";

		public String test() {
			return CONST;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOnlyOnce("return CONST;");

		assertThat(cls).reloadCode(this)
				.containsOnlyOnce("return CONST;");
	}
}
