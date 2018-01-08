package jadx.tests.smali;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestN21 extends SmaliTest {

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali("TestN21");
		String code = cls.getCode().toString();
		System.out.println(code);
	}
}
