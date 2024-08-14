package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass6 extends IntegrationTest {

	public static class TestCls {
		public Runnable test(final double d) {
			return new Runnable() {
				public void run() {
					System.out.println(d);
				}
			};
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public Runnable test(final double d) {")
				.containsOne("return new Runnable() {")
				.containsOne("public void run() {")
				.containsOne("System.out.println(d);")
				.doesNotContain("synthetic");
	}
}
