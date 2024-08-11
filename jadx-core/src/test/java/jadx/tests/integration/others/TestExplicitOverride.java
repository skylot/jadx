package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestExplicitOverride extends SmaliTest {
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.countString(1, "@Override");
	}
}
