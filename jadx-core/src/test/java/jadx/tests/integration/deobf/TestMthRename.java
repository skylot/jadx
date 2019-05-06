package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestMthRename extends IntegrationTest {

	public static class TestCls {

		public abstract static class TestAbstractCls {
			public abstract void a();
		}

		public void test(TestAbstractCls a) {
			a.a();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		enableDeobfuscation();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public abstract void a();")));
		assertThat(code, not(containsString(".a();")));
	}
}
