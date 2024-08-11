package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLocalVarCollideWithPackage extends SmaliTest {
	//@formatter:off
	/*
		-----------------------------------------------------------
		package first;

		import pkg.Second;

		public class A {
			public String test() {
				Second second = new Second();
				second.A.call(); // collision
				return second.str;
			}
		}
		-----------------------------------------------------------
		package pkg;

		public class Second {
			public String str;
		}
		-----------------------------------------------------------
		package second;

		public class A {
		}
		-----------------------------------------------------------
	*/
	//@formatter:on

	@Test
	public void test() {
		List<ClassNode> clsList = loadFromSmaliFiles();
		ClassNode firstA = searchCls(clsList, "first.A");
		String code = firstA.getCode().toString();

		assertThat(code)
				.contains("second.A.call();")
				.doesNotContain("Second second = new Second();");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		loadFromSmaliFiles();
	}

	@Test
	public void testWithoutImports() {
		getArgs().setUseImports(false);
		loadFromSmaliFiles();
	}

	@Test
	public void testWithDeobfuscation() {
		enableDeobfuscation();
		loadFromSmaliFiles();
	}
}
