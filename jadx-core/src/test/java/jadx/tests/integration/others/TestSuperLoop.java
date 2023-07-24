package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestSuperLoop extends SmaliTest {
	// @formatter:off
	/*
		public class A extends B {
			public int a;
		}

		public class B extends A {
			public int b;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		allowWarnInCode();
		disableCompilation();

		List<ClassNode> clsList = loadFromSmaliFiles();
		assertThat(searchCls(clsList, "A"))
				.code()
				.containsOne("public class A extends B {");

		assertThat(searchCls(clsList, "B"))
				.code()
				.containsOne("public class B extends A {");
	}
}
