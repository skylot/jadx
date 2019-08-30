package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestAllNops extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsLines(1, "private boolean test() {", "}"));
		assertThat(code, containsLines(1, "private boolean testWithTryCatch() {", "}"));
	}
}
