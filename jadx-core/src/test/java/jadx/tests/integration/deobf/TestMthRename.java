package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestCls.class)).code()
				.doesNotContain("public abstract void a();")
				.doesNotContain(".a();");
	}
}
