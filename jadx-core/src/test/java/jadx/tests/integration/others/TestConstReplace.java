package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstReplace extends IntegrationTest {

	public static class TestCls {
		public static final String CONST_VALUE = "string";

		public String test() {
			return CONST_VALUE;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code().containsOne("return CONST_VALUE;");
		MethodNode testMth = cls.searchMethodByShortName("test");
		assertThat(testMth).isNotNull();

		FieldNode constField = cls.searchFieldByName("CONST_VALUE");
		assertThat(constField).isNotNull();
		assertThat(constField.getUseIn()).containsExactly(testMth);
	}

	@Test
	public void testWithoutReplace() {
		getArgs().setReplaceConsts(false);
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code().containsOne("return \"string\";");

		FieldNode constField = cls.searchFieldByName("CONST_VALUE");
		assertThat(constField).isNotNull();
		assertThat(constField.getUseIn()).isEmpty();
	}
}
