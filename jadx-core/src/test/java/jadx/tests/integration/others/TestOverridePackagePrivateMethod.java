package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverridePackagePrivateMethod extends SmaliTest {
	// @formatter:off
	/*
		-----------------------------------------------------------
		package test;

		public class A {
			void a() { // package-private
			}
		}
		-----------------------------------------------------------
		package test;

		public class B extends A {
			@Override // test.A
			public void a() {
			}
		}
		-----------------------------------------------------------
		package other;

		import test.A;

		public class C extends A {
			// No @Override here
			public void a() {
			}
		}
		-----------------------------------------------------------
	*/
	// @formatter:on

	@Test
	public void test() {
		commonChecks();
	}

	@Test
	public void testDontChangeAccFlags() {
		getArgs().setRespectBytecodeAccModifiers(true);
		commonChecks();
	}

	private void commonChecks() {
		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(searchCls(classes, "test.A"))
				.code()
				.doesNotContain("/* access modifiers changed")
				.containsLine(1, "void a() {");

		assertThat(searchCls(classes, "test.B")).code().containsOne("@Override");
		assertThat(searchCls(classes, "other.C")).code().doesNotContain("@Override");
	}
}
