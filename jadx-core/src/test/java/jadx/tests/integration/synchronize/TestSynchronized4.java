package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized4 extends SmaliTest {

	// @formatter:off
	/*
		public boolean test(int i) {
			synchronized (this.obj) {
				if (isZero(i)) {
					return call(obj, i);
				}
				System.out.println();
				return getField() == null;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("synchronized (this.obj) {")
				.containsOne("return call(this.obj, i);")
				.containsOne("return getField() == null;");
	}
}
