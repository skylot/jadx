package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestAnonymousClass14 extends SmaliTest {
	// @formatter:off
	/*
		public class OuterCls implements Runnable {

			class TestCls {
				private TestCls() {
					new ArrayList();
				}
			}

			public void makeAnonymousCls() {
				use(new Thread(this) {
					public void someMethod() {
					}
				});
			}

			public void makeTestCls() {
				new TestCls();
			}

			public void run() {
			}

			public void use(Thread thread) {
			}
		}
	 */
	// @formatter:on

	@Test
	public void test() {
		ClassNode clsNode = getClassNodeFromSmaliFiles("inner", "TestAnonymousClass14", "OuterCls");
		String code = clsNode.getCode().toString();

		assertThat(code, not(containsString("AnonymousClass1")));
		assertThat(code, not(containsString("synthetic")));
	}
}
