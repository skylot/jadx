package jadx.tests.internal.generics;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestGenerics3 extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("mthExtendsArray(List<? extends byte[]> list)"));
		assertThat(code, containsString("mthSuperArray(List<? super int[]> list)"));
		assertThat(code, containsString("mthSuperInteger(List<? super Integer> list)"));
		assertThat(code, containsString("mthExtendsString(List<? super String> list)"));
	}
}
