package jadx.tests.integration.variables;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestVariablesUsageWithLoops extends IntegrationTest {

	public static class TestEnhancedFor {

		public void test() {
			List<Object> list;
			synchronized (this) {
				list = new ArrayList<>();
			}
			for (Object o : list) {
				System.out.println(o);
			}
		}
	}

	@Test
	public void testEnhancedFor() {
		ClassNode cls = getClassNode(TestEnhancedFor.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("     list = new ArrayList<>"));
	}

	public static class TestForLoop {

		@SuppressWarnings("rawtypes")
		public void test() {
			List<Object> list;
			synchronized (this) {
				list = new ArrayList<>();
			}
			for (int i = 0; i < list.size(); i++) {
				System.out.println(i);
			}
		}
	}

	@Test
	public void testForLoop() {
		ClassNode cls = getClassNode(TestForLoop.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("     list = new ArrayList<>"));
	}
}
