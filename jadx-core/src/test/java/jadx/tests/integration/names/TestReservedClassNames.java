package jadx.tests.integration.names;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestReservedClassNames extends SmaliTest {
	/*
	 * public class do {
	 * }
	 */

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali("names" + File.separatorChar + "TestReservedClassNames", "do"))
				.code()
				.doesNotContain("public class do");
	}
}
