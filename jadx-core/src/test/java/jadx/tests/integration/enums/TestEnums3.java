package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEnums3 extends IntegrationTest {

	public static class TestCls {

		private static int three = 3;

		public enum Numbers {
			ONE(1), TWO(2), THREE(three), FOUR(three + 1);

			private final int num;

			Numbers(int n) {
				this.num = n;
			}

			public int getNum() {
				return num;
			}
		}

		public void check() {
			assertEquals(1, Numbers.ONE.getNum());
			assertEquals(3, Numbers.THREE.getNum());
			assertEquals(4, Numbers.FOUR.getNum());
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("ONE(1)"));
		// assertThat(code, containsOne("THREE(three)"));
		// assertThat(code, containsOne("assertTrue(Numbers.ONE.getNum() == 1);"));
		assertThat(code, containsOne("Numbers(int n) {"));
	}
}
