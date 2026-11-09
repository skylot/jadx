package jadx.tests.integration.inline;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticClassInline2 extends SmaliTest {

	@Test
	public void test() {
		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(searchCls(classes, "husk.Caller"))
				.code()
				.containsOne("return sb.length() >= 5;");

		ClassNode trampoline = searchCls(classes, "husk.Trampoline");
		assertThat(trampoline.contains(AFlag.DONT_GENERATE)).isTrue();
		assertThat(trampoline.getCode().getCodeStr()).isEmpty();
	}
}
