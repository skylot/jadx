package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Static flag removed from 'InnerClass' annotation (R8),
 * but constructor don't have outer class instance arg, so class is static.
 */
public class TestInnerClassStaticFlagStripped extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("a"))
				.code()
				.containsOne("public static class b {")
				.containsOne("return new b();");
	}
}
