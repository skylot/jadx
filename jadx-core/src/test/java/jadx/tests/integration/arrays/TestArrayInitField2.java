package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayInitField2 extends SmaliTest {
	@Test
	public void test() {
		forceDecompiledCheck();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("static long[] myArr = {1282979400, 0, 0};");
	}
}
