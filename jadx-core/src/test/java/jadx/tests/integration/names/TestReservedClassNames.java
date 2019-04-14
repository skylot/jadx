package jadx.tests.integration.names;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestReservedClassNames extends SmaliTest {
	/*
	 * public class do {
	 * }
	 */

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali("names" + File.separatorChar + "TestReservedClassNames", "do");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public class do")));
	}
}
