package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TestFloatValue extends IntegrationTest {

	public static class TestCls {
		public float[] test() {
			float[] fa = { 0.55f };
			fa[0] /= 2;
			return fa;
		}

		public void check() {
			assertThat(test()[0]).isCloseTo(0.275f, within(0.0001f));
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("1073741824")
				.containsOne("0.55f")
				.containsOne("fa[0] = fa[0] / 2.0f;");
	}
}
