package jadx.tests.integration.others;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestImportsSamePackage extends IntegrationTest {

	@Test
	public void test() {
		args.setUseImports(false);
		ClassNode cls = getClassNode(TestImportsSamePackage1.class, true);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("others.TestImportsSamePackage2")));
	}
}

class TestImportsSamePackage1 {
	TestImportsSamePackage2 package2;
}

class TestImportsSamePackage2 {
}
