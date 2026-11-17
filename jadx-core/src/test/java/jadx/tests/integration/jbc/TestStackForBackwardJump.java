package jadx.tests.integration.jbc;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Stack for code reachable only by backward jump (no StackMapTable)
 */
public class TestStackForBackwardJump extends RaungTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("return str + \"!\";")
				.containsOne("return str + i;");
	}
}
