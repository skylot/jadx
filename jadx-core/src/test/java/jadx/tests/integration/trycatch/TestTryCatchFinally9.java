package jadx.tests.integration.trycatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("JADX INFO: finally extract failed")));
		assertThat(code, not(containsString(indent() + "throw ")));
		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne("if (input != null) {"));
		assertThat(code, containsOne("input.close();"));
	}
}
