package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synchronized (this) {")
				.containsOne("public synchronized boolean test1() {")
				.containsOne("return this.f")
				.containsOne("synchronized (this.o) {")
				.doesNotContain(indent(3) + ';')
				.doesNotContain("try {")
				.doesNotContain("} catch (Throwable th) {")
				.doesNotContain("throw th;");
	}
}
