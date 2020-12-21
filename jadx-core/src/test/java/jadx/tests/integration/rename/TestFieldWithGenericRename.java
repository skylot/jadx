package jadx.tests.integration.rename;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldWithGenericRename extends IntegrationTest {

	public static class TestCls {
		List<String> list;
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOnlyOnce("List<String> list;");

		cls.searchFieldByName("list").getFieldInfo().setAlias("listFieldRenamed");

		assertThat(cls).reloadCode(this)
				.containsOnlyOnce("List<String> listFieldRenamed;");
	}
}
