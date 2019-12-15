package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedSynchronize extends SmaliTest {

	// @formatter:off
	/*
		public final void test() {
			Object obj = null;
			Object obj2 = null;
			synchronized (obj) {
				synchronized (obj2) {
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.countString(2, "synchronized");
	}
}
