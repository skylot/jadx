package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestAnonymousClass14 extends SmaliTest {

	/*
		public class OuterCls implements Runnable {
			class AnonymousClass1 {
				AnonymousClass1(Runnable runnable) {
				}

				public void someMethod() {
				}
			}

			class TestCls {
				private TestCls() {
					ArrayList arrayList = new ArrayList();
				}

				synthetic TestCls(OuterCls outerCls, AnonymousClass1 anonymousClass1) {
					this();
				}
			}

			public void makeAnonymousCls() {
				AnonymousClass1 anonymousClass1 = new AnonymousClass1(this);
			}

			public void makeTestCls() {
				TestCls testCls = new TestCls(this, null);
			}

			public void run() {
			}

			public void use(AnonymousClass1 anonymousClass1) {
			}
		}
	 */

	@Test
	public void test() {
		ClassNode clsNode = getClassNodeFromSmaliFiles("inner", "TestAnonymousClass14", "OuterCls");
		String code = clsNode.getCode().toString();

		assertThat(code, not(containsString("AnonymousClass1")));
		assertThat(code, not(containsString("synthetic")));
	}
}
