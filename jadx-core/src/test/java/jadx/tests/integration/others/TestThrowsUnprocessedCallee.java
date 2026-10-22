package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * 'A' and 'B' depend on each other, so 'B' is processed as a dependency of 'A'
 * and visits 'A.check' before 'A' is loaded.
 * That must not leave 'A.check' without its 'throws' declaration.
 */
public class TestThrowsUnprocessedCallee extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmaliFiles("A"))
				.code()
				.containsOne("public static void check(String str) throws IOException {");
	}
}
