package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass8 extends IntegrationTest {

	public static class TestCls {

		public final double d = Math.abs(4);

		public Runnable test() {
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
				.containsOne("public Runnable test() {")
				.containsOne("return new Runnable() {")
				.containsOne("public void run() {")
				.containsOne("this.d);")
				.doesNotContain("synthetic");
	}
}
