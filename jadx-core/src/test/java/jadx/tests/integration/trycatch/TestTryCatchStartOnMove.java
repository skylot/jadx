package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchStartOnMove extends SmaliTest {
	// @formatter:off
	/*
		private static void test(String s) {
			try {
				call(s);
			} catch (Exception unused) {
				System.out.println("Failed call for " + s);
			}
		}

		private static void call(String s) {}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchStartOnMove"))
				.code()
				.containsOne("try {")
				.containsOne("} catch (Exception e) {")
				.containsOne("System.out.println(\"Failed call for \" + str");
	}
}
