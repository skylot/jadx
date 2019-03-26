package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TestTypeResolver5 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();

		ClassNode cls = getClassNodeFromSmaliWithPath("types", "TestTypeResolver5");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("Object string2")));
		assertThat(code, not(containsString("r1v2")));
	}
}
