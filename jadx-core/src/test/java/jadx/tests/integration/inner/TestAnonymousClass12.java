package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestAnonymousClass12 extends IntegrationTest {

	public static class TestCls {

		public abstract static class BasicAbstract {
			public abstract void doSomething();
		}

		public BasicAbstract outer;
		public BasicAbstract inner;

		public void test() {
			outer = new BasicAbstract() {
				@Override
				public void doSomething() {
					inner = new BasicAbstract() {
						@Override
						public void doSomething() {
							inner = null;
						}
					};
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("outer = new BasicAbstract() {"));
		assertThat(code, containsOne("inner = new BasicAbstract() {"));
		assertThat(code, containsOne("inner = null;"));
	}
}
