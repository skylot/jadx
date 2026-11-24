package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass6 extends RaungTest {

	@SuppressWarnings({ "UnnecessaryFinalOnLocalVariableOrParameter", "Convert2Lambda" })
	public static class TestCls {
		public Runnable test(final double d) {
			return new Runnable() {
				@Override
				public void run() {
					System.out.println(d);
				}
			};
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public Runnable test(final double d) {")
				.containsOne("return new Runnable() {")
				.containsOne("public void run() {")
				.containsOne("System.out.println(d);")
				.doesNotContain("synthetic");
	}

	/**
	 * Java 25 compiler change order for synthetic fields in anonymous classes
	 */
	@Test
	public void testJava25() {
		assertThat(getClassNodeFromRaungFiles())
				.code()
				.containsOne("public Runnable test(final double d) {")
				.containsOne("return new Runnable() {")
				.doesNotContain("synthetic");
	}
}
