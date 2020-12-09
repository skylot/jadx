package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestDeadBlockReferencesStart extends SmaliTest {
	@Test
	public void test() {
		String code = getClassNodeFromSmali().getCode().getCodeStr();
		assertThat(code, countString(0, "throw"));
	}
}
