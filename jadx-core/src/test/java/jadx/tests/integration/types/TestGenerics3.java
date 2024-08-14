package jadx.tests.integration.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("List<String> classes")
				.containsOne("for (String cls : classes) {");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("List<String> classes");
	}
}
