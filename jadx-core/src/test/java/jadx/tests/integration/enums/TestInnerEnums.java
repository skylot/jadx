package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInnerEnums extends IntegrationTest {

	public static class TestCls {

		public enum Numbers {
			ONE((byte) 1, NumString.ONE), TWO((byte) 2, NumString.TWO);

			private final byte num;
			private final NumString str;

			public enum NumString {
				ONE("one"), TWO("two");

				private final String name;

				NumString(String name) {
					this.name = name;
				}

				public String getName() {
					return name;
				}
			}

			Numbers(byte n, NumString str) {
				this.num = n;
				this.str = str;
			}

			public int getNum() {
				return num;
			}

			public NumString getNumStr() {
				return str;
			}

			public String getName() {
				return str.getName();
			}
		}

		public void check() {
			assertEquals(1, Numbers.ONE.getNum());
			assertEquals(Numbers.NumString.ONE, Numbers.ONE.getNumStr());
			assertEquals("one", Numbers.ONE.getName());
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("ONE((byte) 1, NumString.ONE)"));
		assertThat(code, containsOne("ONE(\"one\")"));
	}
}
