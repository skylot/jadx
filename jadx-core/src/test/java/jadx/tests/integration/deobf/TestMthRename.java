package jadx.tests.integration.deobf;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestMthRename extends IntegrationTest {

	public static class TestCls {

		public static abstract class TestAbstractCls {
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

		assertThat(code, containsString("public abstract void mo1a();"));
		assertThat(code, not(containsString("public abstract void a();")));

		assertThat(code, containsString(".mo1a();"));
		assertThat(code, not(containsString(".a();")));
	}
}
