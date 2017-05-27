package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestAnonymousClass4 extends IntegrationTest {

	public static class TestCls {
		public static class Inner {
			private int f;
			private double d;

			public void test() {
				new Thread() {
					{
						f = 1;
					}

					@Override
					public void run() {
						d = 7.5;
					}
				}.start();
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(indent(3) + "new Thread() {"));
		assertThat(code, containsOne(indent(4) + "{"));
		assertThat(code, containsOne("f = 1;"));
		assertThat(code, countString(2, indent(4) + "}"));
		assertThat(code, containsOne(indent(4) + "public void run() {"));
		assertThat(code, containsOne("d = 7.5"));
		assertThat(code, containsOne(indent(3) + "}.start();"));
	}
}
