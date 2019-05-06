package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestFieldCollideWithPackage extends SmaliTest {
	//@formatter:off
	/*
		-----------------------------------------------------------
		package first;

		public class A {
			public A first;
			public second.A second;

			public String test() {
				return second.A.call(); // compiler treat 'second' as field name
			}
		}
		-----------------------------------------------------------
		package second;

		public class A {
			public static String call() {
				return null;
			}
		}
		-----------------------------------------------------------
	*/
	//@formatter:on

	@Test
	public void test() {
		List<ClassNode> clsList = loadFromSmaliFiles();
		ClassNode firstA = searchCls(clsList, "first.A");
		String code = firstA.getCode().toString();

		assertThat(code, containsString("second.A"));
		// expect field to be renamed
		assertThat(code, not(containsString("public second.A second;")));
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
