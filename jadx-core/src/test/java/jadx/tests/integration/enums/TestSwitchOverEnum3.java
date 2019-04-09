package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TestSwitchOverEnum3 extends IntegrationTest {

	public static class TestCls {

		public enum Count {
			ONE, TWO, THREE
		}

		public int testEnum(Count c) {
			switch (c) {
				case ONE:
					return 1;
				case TWO:
					return 2;
			}
			return 0;
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("switch (c) {"));
		assertThat(code, containsString("case ONE:"));
		assertThat(code, not(containsString("ordinal")));
	}
}
