package jadx.tests.integration.types;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver26 extends IntegrationTest {

	@SuppressWarnings({ "rawtypes", "unchecked", "checkstyle:IllegalType" })
	public static class TestCls {
		final ArrayList<String> target = new ArrayList<>();
		final ArrayList source = new ArrayList();

		public void test() {
			((ArrayList) target).add(source.get(0)); // cast removed in bytecode
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("type inference failed")
				.containsOne("this.target.add((String) this.source.get(0));");
	}
}
