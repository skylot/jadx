package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDuplicateVarNames extends IntegrationTest {

	public static class TestCls {
		public static class A {
			public String mth(A a) {
				return null;
			}

			@Override
			public String toString() {
				return "1";
			}
		}

		public A test(A a) {
			return new A() {
				@Override
				public String mth(A innerA) {
					return a + "." + innerA;
				}
			};
		}

		public void check() {
			String str = test(new A()).mth(new A() {
				@Override
				public String toString() {
					return "2";
				}
			});
			assertThat(str).isEqualTo("1.2");
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);

		assertThat(cls)
				.code()
				.doesNotContain("return a + \".\" + a;")
				.doesNotContain("AnonymousClass1");
	}
}
