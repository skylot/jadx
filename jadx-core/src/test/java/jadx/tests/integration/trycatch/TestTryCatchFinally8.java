package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchFinally8 extends IntegrationTest {

	public static class TestCls {
		public Object test(Object obj) {
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
	@NotYetImplemented("Fix merged catch blocks (shared code between catches)")
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("} catch (IOException e) {"));
		assertThat(code, containsString("} finally {"));
		assertThat(code, containsString("file.delete();"));
	}

	@Test
	public void test2() {
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("output = new FileOutputStream(file);"));
		assertThat(code, containsString("} catch (IOException e) {"));
	}
}
