package jadx.tests.integration.trycatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopInTryCatch2 extends IntegrationTest {

	public static class TestCls {
		public String test(Reader in) throws IOException {
			StringBuilder sb = new StringBuilder();
			try {
				BufferedReader bufferedReader = new BufferedReader(in);
				while (true) {
					String line = bufferedReader.readLine();
					if (line == null) {
						break;
					}
					sb.append(line);
				}
				bufferedReader.close();
			} catch (IOException e) {
				doSomething();
			}
			return sb.toString();
		}

		private void doSomething() {
		}

		public void check() throws IOException {
			String text = "line1\nline2";
			assertThat(test(new StringReader(text))).isEqualTo(text.replace("\n", ""));
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} catch (IOException e) {");
	}
}
