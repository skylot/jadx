package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestIfInTryCatch extends IntegrationTest {
	public static class TestCls {
		private void test() {
			/*
			 * 1. if in try
			 * 2. then branch is return
			 * 3. after if, there's more blocks inside try
			 * 4. after try, there's more blocks
			 * this will result in if block and below moved out of try
			 */
			try {
				if (getDouble() > 0.5) {
					return;
				}
				System.out.println("after if");
			} catch (Exception e) {
				System.out.println("exception");
			}
			System.out.println("after try");
		}

		private static double getDouble() throws InterruptedException {
			Thread.sleep(50L);
			return Math.random();
		}
	}

	@Test
	public void test() {
		// if ifBlock is moved out of try, there will be uncaught exception
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code();
	}
}
