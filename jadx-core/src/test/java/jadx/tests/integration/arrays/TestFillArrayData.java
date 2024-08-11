package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFillArrayData extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("TestCls"))
				.code()
				.contains("jArr[0] = 1;")
				.contains("jArr[1] = 2;");
	}
}
