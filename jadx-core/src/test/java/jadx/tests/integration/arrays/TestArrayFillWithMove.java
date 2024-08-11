package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayFillWithMove extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("TestCls"))
				.code()
				.doesNotContain("// fill-array-data instruction")
				.doesNotContain("arr[0] = 0;")
				.contains("return new long[]{0, 1}");
	}
}
