package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryWithResources extends SmaliTest {

	public static class TestCls {

		public static void writeFully(File file, byte[] data) throws IOException {
			try (OutputStream out = new FileOutputStream(file)) {
				out.write(data);
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try (")
				.doesNotContain("close()");
	}
}
