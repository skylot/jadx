package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestAnonymousClass2 extends IntegrationTest {

	public static class TestCls {
		public static class Inner {
			private int f;

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
					public void run() {
						Object obj = Inner.this;
					}
				};
			}
			/*
			public Runnable test3() {
				final int i = f + 2;
				return new Runnable() {
					@Override
					public void run() {
						f = i;
					}
				};
			}
			*/
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("AnonymousClass_")));
		assertThat(code, containsString("f = 1;"));
//		assertThat(code, containsString("f = i;"));
		assertThat(code, not(containsString("Inner obj = ;")));
		assertThat(code, containsString("Inner.this;"));
	}
}
