package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenerics extends IntegrationTest {

	public static class TestCls {
		class A {
		}

		public static void mthWildcard(List<?> list) {
		}

		public static void mthExtends(List<? extends A> list) {
		}

		public static void mthSuper(List<? super A> list) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("mthWildcard(List<?> list)"));
		assertThat(code, containsString("mthExtends(List<? extends A> list)"));
		assertThat(code, containsString("mthSuper(List<? super A> list)"));
	}
}
