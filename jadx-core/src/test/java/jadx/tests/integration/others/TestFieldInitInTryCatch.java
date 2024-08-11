package jadx.tests.integration.others;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitInTryCatch extends IntegrationTest {

	public static class TestCls {
		public static final URL A;

		static {
			try {
				A = new URL("http://www.example.com/");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class TestCls2 {
		public static final URL[] A;

		static {
			try {
				A = new URL[] { new URL("http://www.example.com/") };
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class TestCls3 {
		public static final String[] A;

		static {
			try {
				A = new String[] { "a" };
				// Note: follow code will not be extracted:
				// a = new String[]{new String("a")};
				new URL("http://www.example.com/");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public static final URL A;")
				.containsOne("A = new URL(\"http://www.example.com/\");")
				.containsLines(2,
						"try {",
						indent(1) + "A = new URL(\"http://www.example.com/\");",
						"} catch (MalformedURLException e) {");
	}

	@Test
	public void test2() {
		assertThat(getClassNode(TestCls2.class))
				.code()
				.containsLines(2,
						"try {",
						indent(1) + "A = new URL[]{new URL(\"http://www.example.com/\")};",
						"} catch (MalformedURLException e) {");
	}

	@Test
	public void test3() {
		assertThat(getClassNode(TestCls3.class))
				.code()
				// don't move code from try/catch
				.containsOne("public static final String[] A;")
				.containsOne("A = new String[]{\"a\"};");
	}
}
