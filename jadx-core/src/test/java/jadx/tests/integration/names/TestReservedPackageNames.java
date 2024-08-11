package jadx.tests.integration.names;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestReservedPackageNames extends SmaliTest {

	// @formatter:off
	/*
		package do.if;

		public class A {}
	*/
	// @formatter:on

	@Test
	public void test() {
		List<ClassNode> clsList = loadFromSmaliFiles();
		for (ClassNode cls : clsList) {
			assertThat(cls).code().doesNotContain("package do.if;");
		}
	}

	@Test
	public void testDeobf() {
		enableDeobfuscation();
		List<ClassNode> clsList = loadFromSmaliFiles();
		for (ClassNode cls : clsList) {
			assertThat(cls).code().doesNotContain("package do.if;");
		}
	}

	@Test
	public void testRenameDisabled() {
		disableCompilation();
		args.setRenameFlags(Collections.emptySet());
		for (ClassNode cls : loadFromSmaliFiles()) {
			if (cls.getAlias().equals("A")) {
				assertThat(cls).code().contains("package do.if;");
			}
		}
	}
}
