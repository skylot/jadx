package jadx.tests.integration.others;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestFieldInitInTryCatch extends IntegrationTest {

	public static class TestCls {
		private static final URL a;

		static {
			try {
				a = new URL("http://www.example.com/");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class TestCls2 {
		private static final URL[] a;

		static {
			try {
				a = new URL[]{new URL("http://www.example.com/")};
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class TestCls3 {
		private static final String[] a;

		static {
			try {
				a = new String[]{"a"};
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

		assertThat(code, containsOne("private static final URL a;"));
		assertThat(code, containsOne("a = new URL(\"http://www.example.com/\");"));
		assertThat(code, containsLines(2,
				"try {",
				indent(1) + "a = new URL(\"http://www.example.com/\");",
				"} catch (MalformedURLException e) {"));
	}

	@Test
	public void test2() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"try {",
				indent(1) + "a = new URL[]{new URL(\"http://www.example.com/\")};",
				"} catch (MalformedURLException e) {"));
	}

	@Test
	public void test3() {
		ClassNode cls = getClassNode(TestCls3.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("private static final String[] a = new String[]{\"a\"};"));
	}
}
