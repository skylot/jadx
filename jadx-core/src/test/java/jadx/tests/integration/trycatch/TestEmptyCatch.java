package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Empty catch blocks in enum switch remap building
 */
public class TestEmptyCatch extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.countString(5, "try {")
				.countString(5, "} catch (NoSuchFieldError unused");
	}
}
