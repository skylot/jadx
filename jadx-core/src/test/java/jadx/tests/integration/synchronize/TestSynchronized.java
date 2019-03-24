package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
				return this.i;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("synchronized (this) {")));
		assertThat(code, containsOne("public synchronized boolean test1() {"));
		assertThat(code, containsOne("return this.f"));
		assertThat(code, containsOne("synchronized (this.o) {"));

		assertThat(code, not(containsString(indent(3) + ';')));
		assertThat(code, not(containsString("try {")));
		assertThat(code, not(containsString("} catch (Throwable th) {")));
		assertThat(code, not(containsString("throw th;")));
	}
}
