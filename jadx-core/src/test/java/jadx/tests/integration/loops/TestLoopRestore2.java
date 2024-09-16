package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopRestore2 extends RaungTest {

	@Test
	public void test() {
		disableCompilation(); // unreachable statement
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("while (1 == 0) {");
	}
}
