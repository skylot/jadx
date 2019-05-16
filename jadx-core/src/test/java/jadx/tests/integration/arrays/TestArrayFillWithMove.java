package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TestArrayFillWithMove extends SmaliTest {

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliFiles("TestCls");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("// fill-array-data instruction")));
		assertThat(code, not(containsString("arr[0] = 0;")));

		assertThat(code, containsString("return new long[]{0, 1}"));
	}
}
