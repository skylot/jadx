package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestEnums3 extends IntegrationTest {

	public static class TestCls {

		private static int three = 3;

		public enum Numbers {
			ONE(1), TWO(2), THREE(three), FOUR(three + 1);

			private final int num;

			private Numbers(int n) {
				this.num = n;
			}

			public int getNum() {
				return num;
			}
		}

		public void check() {
			assertTrue(Numbers.ONE.getNum() == 1);
			assertTrue(Numbers.THREE.getNum() == 3);
			assertTrue(Numbers.FOUR.getNum() == 4);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("ONE(1)"));
//		assertThat(code, containsOne("THREE(three)"));
//		assertThat(code, containsOne("assertTrue(Numbers.ONE.getNum() == 1);"));
		assertThat(code, containsOne("private Numbers(int n) {"));
	}
}
