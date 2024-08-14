package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver10 extends SmaliTest {

	/*
	 * Method argument assigned with different types in separate branches
	 */

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code().containsOne("Object test(String str, String str2)")
				.doesNotContain("Object obj2 = 0;");
	}
}
