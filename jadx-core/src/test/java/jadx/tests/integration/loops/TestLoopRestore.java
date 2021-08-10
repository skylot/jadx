package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopRestore extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("try {")
				.containsOne("for (byte b : digest) {");
	}
}
