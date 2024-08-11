package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass3 extends IntegrationTest {

	public static class TestCls {
		public static class Inner {
			private int f;
			public double d;

			public void test() {
				new Thread() {
					@Override
					public void run() {
						int a = f--;
						p(a);

						f += 2;
						f *= 2;

						a = ++f;
						p(a);

						d /= 3;
					}

					public void p(int a) {
					}
				}.start();
			}
		}
	}

	@Test
	public void test() {
		disableCompilation();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains(indent(4) + "public void run() {")
				.contains(indent(3) + "}.start();")
				.doesNotContain("AnonymousClass_");
	}

	@Test
	@NotYetImplemented
	public void test2() {
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.contains("a = f--;");
	}
}
