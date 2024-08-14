package jadx.tests.integration.trycatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryAfterDeclaration extends IntegrationTest {

	static class TestClass {
		public static void consume() throws IOException {
			InputStream bis = null;
			try {
				bis = new FileInputStream("1.txt");
				while (bis != null) {
					System.out.println("c");
				}
			} catch (final IOException ignore) {
			}
		}
	}

	/**
	 * Issue #62.
	 */
	@Test
	@NotYetImplemented
	public void test62() {
		JadxAssertions.assertThat(getClassNode(TestClass.class))
				.code()
				.containsOne("try {");
	}
}
