package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString(indent(4) + "public void run() {"));
		assertThat(code, containsString(indent(3) + "}.start();"));

		assertThat(code, not(containsString("AnonymousClass_")));
	}

	@Test
	@NotYetImplemented
	public void test2() {
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("synthetic")));
		assertThat(code, containsString("a = f--;"));
	}
}
