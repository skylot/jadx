package jadx.tests.integration.variables;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestEnhancedFor.class))
				.code()
				.containsLine(2, "synchronized (this) {")
				.containsLine(3, "list = new ArrayList<>");
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
		assertThat(getClassNode(TestEnhancedFor.class))
				.code()
				.containsLine(2, "synchronized (this) {")
				.containsLine(3, "list = new ArrayList<>");
	}
}
