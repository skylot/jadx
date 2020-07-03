package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumObfuscated extends SmaliTest {
	// @formatter:off
	/*
		public enum TestEnumObfuscated {
			private static final synthetic TestEnumObfuscated[] $VLS = {ONE, TWO};
			public static final TestEnumObfuscated ONE = new TestEnumObfuscated("ONE", 0, 1);
			public static final TestEnumObfuscated TWO = new TestEnumObfuscated("TWO", 1, 2);
			private final int num;

			private TestEnumObfuscated(String str, int i, int i2) {
				super(str, i);
				this.num = i2;
			}

			public static TestEnumObfuscated vo(String str) {
				return (TestEnumObfuscated) Enum.valueOf(TestEnumObfuscated.class, str);
			}

			public static TestEnumObfuscated[] vs() {
				return (TestEnumObfuscated[]) $VLS.clone();
			}

			public synthetic int getNum() {
				return this.num;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		assertThat(cls)
				.code()
				.doesNotContain("$VLS")
				.doesNotContain("vo(")
				.doesNotContain("vs(")
				.containsOne("int getNum() {");
	}
}
