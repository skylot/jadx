package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticConstructor extends SmaliTest {
	// @formatter:off
	/*
		public class Test {
			static {
				new BuggyConstructor();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmaliFiles("Test"))
				.code()
				.containsLine(2, "new BuggyConstructor();");
	}
}
