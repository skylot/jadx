package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatchFinally4 extends IntegrationTest {

	public static class TestCls {
		public void test() throws IOException {
			File file = File.createTempFile("test", "txt");
			OutputStream outputStream = new FileOutputStream(file);
			try {
				outputStream.write(1);
			} finally {
				try {
					outputStream.close();
					file.delete();
				} catch (IOException ignored) {
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("File file = File.createTempFile(\"test\", \"txt\");")
				.containsOne("OutputStream outputStream = new FileOutputStream(file);")
				.containsOne("outputStream.write(1);")
				.containsOne("} finally {")
				.containsOne("outputStream.close();")
				.containsOne("} catch (IOException e) {");
	}
}
