package jadx.tests.integration.inner;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestInnerClass4 extends IntegrationTest {

	public static class TestCls {
		public class C {
			public String c;

			private C() {
				this.c = "c";
			}
		}

		private String test() {
			return new C().c;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new C().c;"));
	}
}
