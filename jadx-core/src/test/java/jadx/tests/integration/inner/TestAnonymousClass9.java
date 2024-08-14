package jadx.tests.integration.inner;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass9 extends IntegrationTest {

	public static class TestCls {

		public Callable<String> c = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "str";
			}
		};

		public Runnable test() {
			return new FutureTask<String>(this.c) {
				public void run() {
					System.out.println(6);
				}
			};
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("c = new Callable<String>() {")
				.containsOne("return new FutureTask<String>(this.c) {")
				.doesNotContain("synthetic");
	}
}
