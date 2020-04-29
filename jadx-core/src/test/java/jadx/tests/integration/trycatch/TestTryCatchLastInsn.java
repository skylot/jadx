package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchLastInsn extends SmaliTest {

	// @formatter:off
	/*
		public Exception test() {
			? r1 = "result"; // String
			try {
				r1 = call(); // Exception
			} catch (Exception e) {
				System.out.println(r1); // String
				r1 = e;
			}
			return r1;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("return call();")
				.containsOne("} catch (Exception e) {");
	}
}
