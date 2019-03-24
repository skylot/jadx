package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenerics3 extends IntegrationTest {

	public static class TestCls {

		public static void mthExtendsArray(List<? extends byte[]> list) {
		}

		public static void mthSuperArray(List<? super int[]> list) {
		}

		public static void mthSuperInteger(List<? super Integer> list) {
		}

		public static void mthExtendsString(List<? super String> list) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("mthExtendsArray(List<? extends byte[]> list)"));
		assertThat(code, containsString("mthSuperArray(List<? super int[]> list)"));
		assertThat(code, containsString("mthSuperInteger(List<? super Integer> list)"));
		assertThat(code, containsString("mthExtendsString(List<? super String> list)"));
	}
}
