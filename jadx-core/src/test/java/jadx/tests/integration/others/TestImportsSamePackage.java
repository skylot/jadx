package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestImportsSamePackage extends IntegrationTest {

	@Test
	public void test() {
		args.setUseImports(false);
		ClassNode cls = getClassNode(TestImportsSamePackage1.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("others.TestImportsSamePackage2")));
	}
}
