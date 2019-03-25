package jadx.tests.integration.conditions;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestXor extends IntegrationTest {

	public static class TestCls {
		public boolean test() {
			return test2() ^ true;
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

		assertThat(code, not(containsOne("1")));
	}

}
