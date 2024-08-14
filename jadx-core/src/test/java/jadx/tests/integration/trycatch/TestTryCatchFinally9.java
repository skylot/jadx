package jadx.tests.integration.trycatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatchFinally9 extends IntegrationTest {

	public static class TestCls {
		public String test() throws IOException {
			InputStream input = null;
			try {
				input = this.getClass().getResourceAsStream("resource");
				Scanner scanner = new Scanner(input).useDelimiter("\\A");
				return scanner.hasNext() ? scanner.next() : "";
			} finally {
				if (input != null) {
					input.close();
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("JADX INFO: finally extract failed")
				.doesNotContain(indent() + "throw ")
				.containsOne("} finally {")
				.containsOne("if (input != null) {")
				.containsOne("input.close();");
	}
}
