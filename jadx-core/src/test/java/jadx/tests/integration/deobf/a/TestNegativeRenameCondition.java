package jadx.tests.integration.deobf.a;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNegativeRenameCondition extends IntegrationTest {

	public static class TestCls {

		@SuppressWarnings("checkstyle:TypeName")
		public interface a {

			@SuppressWarnings("checkstyle:MethodName")
			void a();
		}

		public void test(a a) {
			a.a();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		enableDeobfuscation();
		// disable rename by length
		args.setDeobfuscationMinLength(0);
		args.setDeobfuscationMaxLength(999);
		// disable all renaming options
		args.setRenameFlags(Collections.emptySet());

		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("renamed from")
				.containsOne("package jadx.tests.integration.deobf.a;")
				.containsOne("public interface a {");
	}
}
