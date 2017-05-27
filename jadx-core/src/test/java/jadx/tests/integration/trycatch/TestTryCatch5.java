package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestTryCatch5 extends IntegrationTest {

	public static class TestCls {
		private Object test(Object obj) {
			File file = new File("r");
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
				if (obj.equals("a")) {
					return new Object();
				} else {
					return null;
				}
			} catch (IOException e) {
				System.out.println("Exception");
				return null;
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						// Ignored
					}
				}
				file.delete();
			}
		}
	}

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		// TODO:
//		assertThat(code, containsString("output = new FileOutputStream(file);"));
//		assertThat(code, containsString("} catch (IOException e) {"));
		assertThat(code, containsString("file.delete();"));
	}
}
