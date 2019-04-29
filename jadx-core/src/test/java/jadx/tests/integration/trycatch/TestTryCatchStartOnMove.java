package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchStartOnMove");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, containsOne("System.out.println(\"Failed call for \" + str"));
	}
}
