package jadx.tests.integration.enums;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
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

			// custom values method
			// should be kept and renamed to avoid collision to enum 'values()' method
			public static int values() {
				return new TestEnumObfuscated[0];
			}

			// usage of renamed 'values()' method, should be renamed back to 'values'
			public static int valuesCount() {
				return vs().length;
			}

			// usage of renamed '$VALUES' field, should be replaced with 'values()' method call
			public static int valuesFieldUse() {
				return $VLS.length;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		getArgs().setRenameFlags(Collections.emptySet());
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("$VLS")
				.doesNotContain("vo(")
				.doesNotContain("vs(")
				.containsOne("int getNum() {");
	}
}
