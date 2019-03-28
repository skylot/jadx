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

}
