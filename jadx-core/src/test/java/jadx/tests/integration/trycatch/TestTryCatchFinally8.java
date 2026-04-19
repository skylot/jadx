package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally8 extends IntegrationTest {

	@SuppressWarnings({ "ResultOfMethodCallIgnored", "TryFinallyCanBeTryWithResources", "DataFlowIssue" })
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
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("FileOutputStream output = null;")
				.countString(2, "try {")
				.countString(2, "} catch (IOException e")
				.containsOne("} finally {")
				.containsOne("file.delete();");
	}
}
