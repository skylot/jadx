package jadx.tests.integration.others;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestStringConstructor extends IntegrationTest {

	public static class TestCls {
		public String tag = new String(new byte[] { 'a', 'b', 'c' });
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("abc");
	}

	public static class TestCls2 {
		public String tag = new String(new byte[] { 'a', 'b', 'c' }, StandardCharsets.UTF_8);
	}

	@Test
	public void test2() {
		JadxAssertions.assertThat(getClassNode(TestCls2.class))
				.code()
				.containsOne("new String(\"abc\".getBytes(), StandardCharsets.UTF_8)");
	}

	public static class TestCls3 {
		public String tag = new String(new byte[] { 1, 2, 3, 'a', 'b', 'c' });
	}

	@Test
	public void test3() {
		JadxAssertions.assertThat(getClassNode(TestCls3.class))
				.code()
				.containsOne("\\u0001\\u0002\\u0003abc");
	}

	public static class TestCls4 {
		public String tag = new String(new char[] { 1, 2, 3, 'a', 'b', 'c' });
	}

	@Test
	public void test4() {
		JadxAssertions.assertThat(getClassNode(TestCls4.class))
				.code()
				.containsOne("\\u0001\\u0002\\u0003abc");
	}

	public static class TestCls5 {
		public String tag = new String(new char[] { 1, 2, 3, 'a', 'b' });
	}

	@Test
	public void test5() {
		JadxAssertions.assertThat(getClassNode(TestCls5.class))
				.code()
				.containsOne("{1, 2, 3, 'a', 'b'}");
	}

	public static class TestClsNegative {
		public String tag = new String();
	}

	@Test
	public void testNegative() {
		JadxAssertions.assertThat(getClassNode(TestClsNegative.class))
				.code()
				.containsOne("tag = new String();");
	}

	public static class TestClsNegative2 {
		public byte b = 32;
		public String tag = new String(new byte[] { 31, b });
	}

	@Test
	public void testNegative2() {
		JadxAssertions.assertThat(getClassNode(TestClsNegative2.class))
				.code()
				.containsOne("tag = new String(new byte[]{31, this.b});");
	}
}
