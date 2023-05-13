package jadx.tests.integration.loops;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIterableForEach4 extends IntegrationTest {

	public static class TestCls {
		public void test(List<Object> objects) {
			for (Object o : objects) {
				if (o.hashCode() != 42 || o.hashCode() != 1) {
					break;
				}
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("while (");
	}
}
