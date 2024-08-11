package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestMultiExceptionCatchSameJump extends SmaliTest {
	// @formatter:off
	/*
		public static class TestCls {
			public void test() {
				try {
					System.out.println("Test");
				} catch (ProviderException | DateTimeException e) {
					throw new RuntimeException(e);
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithPkg("trycatch", "TestMultiExceptionCatchSameJump"))
				.code()
				.containsOne("try {")
				.containsOne("} catch (ProviderException | DateTimeException e) {")
				.containsOne("throw new RuntimeException(e);")
				.doesNotContain("RuntimeException e;");
	}
}
