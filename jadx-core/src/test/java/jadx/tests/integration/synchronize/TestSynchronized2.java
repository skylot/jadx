package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSynchronized2 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("unused")
		private static synchronized boolean test(Object obj) {
			return obj.toString() != null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("private static synchronized boolean test(Object obj) {"));
		assertThat(code, containsString("obj.toString() != null;"));
	}

	@Test
	@NotYetImplemented
	public void test2() {
		useDexInput(); // java bytecode don't add exception handlers

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return obj.toString() != null;"));
		assertThat(code, not(containsString("synchronized (")));
	}
}
