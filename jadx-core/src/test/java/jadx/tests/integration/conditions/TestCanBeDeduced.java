package jadx.tests.integration.conditions;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestCanBeDeduced extends IntegrationTest {

	public static class TestCls {
		public boolean test1() {
			boolean value = test2();
			return value ^ true;
		}

		public boolean test2() {
			return true;
		}
	}

	@Test
	@NotYetImplemented
	public void test409() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return test2();"));
	}
}
