package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTypeInheritance extends IntegrationTest {

	public static class TestCls {

		public interface IRoot {
		}

		public interface IBase extends IRoot {
		}

		public static class A implements IBase {
		}

		public static class B implements IBase {
			public void b() {
			}
		}

		public static void test(boolean z) {
			IBase impl;
			if (z) {
				impl = new A();
			} else {
				B b = new B();
				b.b();
				impl = b; // this move is removed in no-debug byte-code
			}
			useBase(impl);
			useRoot(impl);
		}

		private static void useRoot(IRoot root) {
		}

		private static void useBase(IBase base) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("IBase impl;"));
		assertThat(code, containsOne("impl = new A();"));
		assertThat(code, containsOne("B b = new B();"));
		assertThat(code, containsOne("impl = b;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
