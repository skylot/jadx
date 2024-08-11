package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass2 extends IntegrationTest {

	public static class TestCls {
		public static class Inner {
			public int f;

			public Runnable test() {
				return new Runnable() {
					@Override
					public void run() {
						f = 1;
					}
				};
			}

			public Runnable test2() {
				return new Runnable() {
					@Override
					@SuppressWarnings("unused")
					public void run() {
						Object obj = Inner.this;
					}
				};
			}

			public Runnable test3() {
				final int i = f + 2;
				return new Runnable() {
					@Override
					public void run() {
						f = i;
					}
				};
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("AnonymousClass_")
				.contains("f = 1;")
				.contains("f = i;")
				.doesNotContain("Inner obj = ;")
				.contains("Inner.this;");
	}
}
