package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestJavaDup2x2 extends RaungTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("dArr[0] = 127.5d;");
	}
}
