package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestMultiEntryLoop3 extends SmaliTest {

	@Test
	public void test() {
		allowWarnInCode();
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali();
		assertThat(cls).code().contains("Cycle in region maker at block:");
	}
}
