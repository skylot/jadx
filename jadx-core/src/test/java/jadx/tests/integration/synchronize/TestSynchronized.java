package jadx.tests.integration.synchronize;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestSynchronized extends IntegrationTest {

	public static class TestCls {
		public boolean f = false;
		public final Object o = new Object();
		public int i = 7;

		public synchronized boolean test1() {
			return this.f;
		}

		public int test2() {
			synchronized (this.o) {
				return i;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, not(containsString("synchronized (this) {")));
		assertThat(code, containsString("public synchronized boolean test1() {"));
		assertThat(code, containsString("return this.f"));
		assertThat(code, containsString("synchronized (this.o) {"));

		assertThat(code, not(containsString(indent(3) + ";")));
	}
}
