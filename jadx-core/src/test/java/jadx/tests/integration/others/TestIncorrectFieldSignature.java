package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIncorrectFieldSignature extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("public static boolean A;")
				.containsOne("public static Boolean B;")
				.countString(2, "/* JADX INFO: Incorrect field signature:");
	}
}
