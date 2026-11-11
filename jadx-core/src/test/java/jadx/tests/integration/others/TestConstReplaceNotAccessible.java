package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.integration.others.constpkg.PublicConsts;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Constants from not accessible classes must not be used in other packages.
 */
public class TestConstReplaceNotAccessible extends IntegrationTest {

	public static class TestCls {
		public int test() {
			return 7351;
		}

		public int testPublic() {
			return 9127;
		}
	}

	@Test
	public void test() throws ClassNotFoundException {
		// package-private interface can't be referenced directly
		Class<?> hiddenConsts = Class.forName("jadx.tests.integration.others.constpkg.HiddenConsts");
		List<ClassNode> list = getClassNodes(TestCls.class, hiddenConsts, PublicConsts.class);
		ClassNode outerCls = searchCls(list, TestConstReplaceNotAccessible.class.getName());
		ClassNode testCls = outerCls.getInnerClasses().get(0);
		assertThat(testCls.searchMethodByShortName("test"))
				.code()
				.containsOne("return 7351;");
		assertThat(testCls.searchMethodByShortName("testPublic"))
				.code()
				.containsOne("return PublicConsts.PUBLIC_VALUE;");
	}
}
