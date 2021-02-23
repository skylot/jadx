package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassReGen extends IntegrationTest {

	public static class TestCls {
		private int intField = 5;

		public static class A {
		}

		public int test() {
			return 0;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.code()
				.containsOnlyOnce("private int intField = 5;")
				.containsOnlyOnce("public static class A {")
				.containsOnlyOnce("public int test() {");

		cls.getInnerClasses().get(0).getClassInfo().changeShortName("ARenamed");
		cls.searchMethodByShortName("test").getMethodInfo().setAlias("testRenamed");
		cls.searchFieldByName("intField").getFieldInfo().setAlias("intFieldRenamed");

		assertThat(cls)
				.reloadCode(this)
				.containsOnlyOnce("private int intFieldRenamed = 5;")
				.containsOnlyOnce("public static class ARenamed {")
				.containsOnlyOnce("public int testRenamed() {");
	}
}
