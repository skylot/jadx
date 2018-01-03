package jadx.tests.smali;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestArithConst extends SmaliTest {

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNodeFromSmali("TestArithConst");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return i + CONST_INT;"));
	}
}
