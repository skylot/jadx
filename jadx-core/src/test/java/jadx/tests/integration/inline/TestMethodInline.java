package jadx.tests.integration.inline;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestMethodInline extends SmaliTest {
	// @formatter:off
	/*
		package inline;

		public class A {
			public static void useMth() {
				inline.other.B.bridgeMth(); // after inline 'inline.other.C.test()' is not accessible
			}
		}
		-----------------------------------------------------------
		package inline.other;

		public class B {
			public static bridge synthetic void bridgeMth() {
				inline.other.C.test();
			}
		}
		----------------------------------------------------------
		package inline.other;

		class C {
			public static void test() {
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		List<ClassNode> classes = loadFromSmaliFiles();
		ClassNode aCls = searchCls(classes, "inline.A");
		ClassNode bCls = searchCls(classes, "inline.other.B");
		ClassNode cCls = searchCls(classes, "inline.other.C");

		assertThat(bCls).code().doesNotContain("bridgeMth()");
		assertThat(aCls).code().containsOne("C.test()");
		assertThat(cCls).code().containsOne("public class C {");

		// TODO: update dependencies?
		// assertThat(aCls.getDependencies()).contains(cCls);
		// assertThat(cCls.getUsedIn()).contains(aCls);
	}
}
