package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.Named;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTernary3 extends IntegrationTest {

	public static class TestCls {

		public boolean isNameEquals(InsnArg arg) {
			String n = getName(arg);
			if (n == null || !(arg instanceof Named)) {
				return false;
			}
			return n.equals(((Named) arg).getName());
		}

		private String getName(InsnArg arg) {
			if (arg instanceof RegisterArg) {
				return "r";
			}
			if (arg instanceof Named) {
				return "n";
			}
			return arg.toString();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (n == null || !(arg instanceof Named)) {")
				.containsOne("return n.equals(((Named) arg).getName());")
				.doesNotContain("if ((arg instanceof RegisterArg)) {");
	}
}
