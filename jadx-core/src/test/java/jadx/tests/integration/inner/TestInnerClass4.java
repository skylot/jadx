package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInnerClass4 extends IntegrationTest {

	public static class TestCls {
		public class C {
			public String c;

			private C() {
				this.c = "c";
			}
		}

		public String test() {
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
