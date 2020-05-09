package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstructorWithMoves extends SmaliTest {
	// @formatter:off
	/*
		public boolean test() {
				java.lang.Boolean r5 = new java.lang.Boolean
				r8 = r5
				r5 = r8
				r6 = r8
				java.lang.String r7 = "test"
				r6.<init>(r7)
				java.lang.Boolean r5 = (java.lang.Boolean) r5
				boolean r5 = r5.booleanValue()
				r3 = r5
				return r3
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("return new Boolean(\"test\").booleanValue();");
	}
}
