package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("try {")
				.contains("} catch (IOException e) {")
				.contains("} finally {")
				.contains("file.delete();");
	}

	@Test
	public void test2() {
		disableCompilation();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("output = new FileOutputStream(file);")
				.contains("} catch (IOException e) {");
	}
}
