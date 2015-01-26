package jadx.tests.smali;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestInlineVarArg extends SmaliTest {

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNodeFromSmali("TestInlineVarArg");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("f(\"a\", \"b\", \"c\");"));
	}
}
