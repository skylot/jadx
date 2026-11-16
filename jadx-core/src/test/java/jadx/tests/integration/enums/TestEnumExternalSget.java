package jadx.tests.integration.enums;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumExternalSget extends SmaliTest {

	/**
	 * Enum constant constructors take an argument whose value is a static-field
	 * read (SGET) of an external enum constant (ExtEnum.FIRST/SECOND). The value
	 * is reused across constants, so R8 keeps it in a register instead of
	 * inlining it. Previously EnumVisitor failed to restore the enum with
	 * "Init of enum field '...' uses external variables" (see issue #2618).
	 */
	@Test
	public void test() {
		disableCompilation();
		List<ClassNode> classNodes = loadFromSmaliFiles();
		assertThat(searchCls(classNodes, "TestEnumExternalSget"))
				.code()
				.containsOne("enum TestEnumExternalSget")
				.containsOne("A(ExtEnum.FIRST)")
				.containsOne("B(ExtEnum.FIRST)")
				.containsOne("C(ExtEnum.SECOND)");
	}
}
