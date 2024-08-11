package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized5 extends SmaliTest {
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.contains("1 != 0")
				.contains("System.gc();");
	}
}
