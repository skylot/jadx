package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class TestMissingExceptions extends SmaliTest {

	@Test
	void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.countString(6, "FileNotFoundException");
	}
}
