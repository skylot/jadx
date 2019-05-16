package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFillArrayData extends SmaliTest {

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliFiles("TestCls");
		String code = cls.getCode().toString();

		assertThat(code, containsString("jArr[0] = 1;"));
		assertThat(code, containsString("jArr[1] = 2;"));
	}
}
