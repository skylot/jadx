package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchLastInsn extends SmaliTest {

	// @formatter:off
	/*
		public Exception test() {
			? r1 = "result"; // String
			try {
				r1 = call(); // Exception
			} catch(Exception e) {
				System.out.println(r1); // String
				r1 = e;
			}
			return r1;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return call();"));
	}
}
