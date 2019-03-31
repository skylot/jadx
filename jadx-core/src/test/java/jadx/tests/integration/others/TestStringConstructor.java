package jadx.tests.integration.others;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestStringConstructor extends IntegrationTest {

	public static class TestCls {
		public String tag = new String(new byte[] {'a', 'b', 'c'});
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("abc"));
	}

	public static class TestCls2 {
		public String tag = new String(new byte[] {'a', 'b', 'c'}, StandardCharsets.UTF_8);
	}

	@Test
	public void test2() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("new String(\"abc\".getBytes(), StandardCharsets.UTF_8)"));
	}

	public static class TestCls3 {
		public String tag = new String(new byte[] {1, 2, 3, 'a', 'b', 'c'});
	}

	@Test
	public void test3() {
		ClassNode cls = getClassNode(TestCls3.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("\\u0001\\u0002\\u0003abc"));
	}

	public static class TestCls4 {
		public String tag = new String(new char[] {1, 2, 3, 'a', 'b', 'c'});
	}

	@Test
	public void test4() {
		ClassNode cls = getClassNode(TestCls4.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("\\u0001\\u0002\\u0003abc"));
	}

	public static class TestCls5 {
		public String tag = new String(new char[] {1, 2, 3, 'a', 'b'});
	}

	@Test
	public void test5() {
		ClassNode cls = getClassNode(TestCls5.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("{1, 2, 3, 'a', 'b'}"));
	}

	public static class TestClsNegative {
		public String tag = new String();
	}

	@Test
	public void testNegative() {
		ClassNode cls = getClassNode(TestClsNegative.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("tag = new String();"));
	}
}
