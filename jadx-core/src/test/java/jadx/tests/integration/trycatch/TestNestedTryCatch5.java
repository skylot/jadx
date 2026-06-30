package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedTryCatch5 extends SmaliTest {

	@Test
	@NotYetImplemented("Extracting finally on loop advancement")
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("?? ")
				.containsOne("} finally")
				.containsOne("endTransaction")
				.countString(1, "throw "); // 1 real throws, 1 implicit throw on finally handler and 1 implicit throw on empty ALL handler
	}

	@Test
	public void testNoFinally() {
		args.setExtractFinally(false);
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("?? ")
				.countString(3, "throw ");
	}
}
