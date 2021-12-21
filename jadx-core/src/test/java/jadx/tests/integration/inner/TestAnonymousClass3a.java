package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass3a extends IntegrationTest {

	public static class TestCls {
		public static class Inner {
			private int f;
			private int r;

			public void test() {
				new Runnable() {
					@Override
					public void run() {
						int a = --Inner.this.f;
						p(a);
					}

					public void p(int a) {
						Inner.this.r = a;
					}
				}.run();
			}
		}

		public void check() {
			Inner inner = new Inner();
			inner.f = 2;
			inner.test();
			assertThat(inner.f).isEqualTo(1);
			assertThat(inner.r).isEqualTo(1);
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.NONE);
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("access$00")
				.doesNotContain("AnonymousClass_")
				.doesNotContain("unused = ")
				.containsLine(4, "public void run() {")
				.containsLine(3, "}.run();");
	}
}
