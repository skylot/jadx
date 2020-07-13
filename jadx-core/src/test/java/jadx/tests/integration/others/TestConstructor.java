package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstructor extends SmaliTest {
	// @formatter:off
	/*
		private SomeObject test(double r23, double r25, SomeObject r27) {
			SomeObject r17 = new SomeObject
			r0 = r17
			r1 = r27
			r0.<init>(r1)
			return r17
		}
	 */
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("new SomeObject(arg3);")
				.doesNotContain("= someObject");
	}
}
