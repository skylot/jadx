package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSameMethodsNames extends IntegrationTest {

	public static class TestCls<V> {

		public static void test() {
			new Bug().Bug();
		}

		public static class Bug {
			public Bug() {
				System.out.println("constructor");
			}

			@SuppressWarnings({ "MethodName", "MethodNameSameAsClassName" })
			void Bug() {
				System.out.println("Bug");
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("new Bug().Bug();"));
	}
}
