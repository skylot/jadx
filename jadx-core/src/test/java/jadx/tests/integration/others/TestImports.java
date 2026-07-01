package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("DataFlowIssue")
public class TestImports extends IntegrationTest {

	public static class TestCls1 {
		public Character.UnicodeBlock test1() {
			return null;
		}

		public Character test2() {
			return 'a';
		}
	}

	public static class TestCls2 {
		public static final class Character {
		}

		public Character test1() {
			return new Character();
		}

		public java.lang.Character test2() {
			return 'c';
		}

		public java.lang.Character.UnicodeBlock test3() {
			return null;
		}
	}

	@Test
	public void test1() {
		noDebugInfo();
		assertThat(getClassNode(TestCls1.class))
				.code()
				.doesNotContain("import java.lang.Character;"); // import not needed
	}

	@Test
	public void test2() {
		noDebugInfo();
		assertThat(getClassNode(TestCls2.class))
				.code();
	}
}
