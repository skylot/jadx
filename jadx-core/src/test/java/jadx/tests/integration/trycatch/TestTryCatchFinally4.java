package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("File file = File.createTempFile(\"test\", \"txt\");"));
		assertThat(code, containsOne("OutputStream outputStream = new FileOutputStream(file);"));
		assertThat(code, containsOne("outputStream.write(1);"));
		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne("outputStream.close();"));
		assertThat(code, containsOne("} catch (IOException e) {"));
	}
}
