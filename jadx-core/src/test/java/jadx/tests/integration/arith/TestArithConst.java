package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArithConst extends SmaliTest {

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNodeFromSmaliWithPath("arith", "TestArithConst");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return i + CONST_INT;"));
	}
}
