package jadx.tests.integration.others;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public static final URL A;"));
		assertThat(code, containsOne("A = new URL(\"http://www.example.com/\");"));
		assertThat(code, containsLines(2,
				"try {",
				indent(1) + "A = new URL(\"http://www.example.com/\");",
				"} catch (MalformedURLException e) {"));
	}

	@Test
	public void test2() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"try {",
				indent(1) + "A = new URL[]{new URL(\"http://www.example.com/\")};",
				"} catch (MalformedURLException e) {"));
	}

	@Test
	public void test3() {
		ClassNode cls = getClassNode(TestCls3.class);
		String code = cls.getCode().toString();

		// don't move code from try/catch
		assertThat(code, containsOne("public static final String[] A;"));
		assertThat(code, containsOne("A = new String[]{\"a\"};"));
	}
}
