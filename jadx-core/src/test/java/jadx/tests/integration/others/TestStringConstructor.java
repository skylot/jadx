package jadx.tests.integration.others;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
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
	@NotYetImplemented
	public void test530() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("new String(\"abc\".getBytes(), StandardCharsets.UTF_8)"));
	}


	public static class TestCls3 {
		public String tag = new String(new byte[] {1, 2, 3});
	}

	@Test
	public void test3() {
		ClassNode cls = getClassNode(TestCls3.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("\\u0001\\u0002\\u0003"));
	}

	public static class TestCls4 {
		public String tag = new String(new byte[] {0, 1, 2});
	}

	@Test
	@NotYetImplemented("Due to byte array construction")
	public void test4() {
		ClassNode cls = getClassNode(TestCls4.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("\\u0000\\u0001\\u0002"));
	}

}
