package jadx.tests.integration.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenerics3 extends IntegrationTest {

	public static class TestCls {
		public static void test() {
			List<String> classes = getClasses();
			Collections.sort(classes);
			int passed = 0;
			for (String cls : classes) {
				if (runTest(cls)) {
					passed++;
				}
			}
			int failed = classes.size() - passed;
			System.out.println("failed: " + failed);
		}

		private static boolean runTest(String clsName) {
			return false;
		}

		private static List<String> getClasses() {
			return new ArrayList<>();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("List<String> classes"));
		assertThat(code, containsOne("for (String cls : classes) {"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("List<String> classes"));
	}
}
