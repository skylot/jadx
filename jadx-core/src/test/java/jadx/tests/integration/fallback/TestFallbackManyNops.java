package jadx.tests.integration.fallback;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFallbackManyNops extends SmaliTest {

	@Test
	public void test() {
		setFallback();
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.contains("public static void test() {")
				.containsOne("return")
				.doesNotContain("Method dump skipped");
	}
}
