package jadx.tests.integration.names;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDefPkgRename extends SmaliTest {

	@Test
	public void test() {
		List<ClassNode> clsList = loadFromSmaliFiles();
		// class A moved to 'defpackage'
		assertThat(searchCls(clsList, "A"))
				.code()
				.containsOne("package defpackage;");
		assertThat(searchCls(clsList, "pkg.B"))
				.code()
				.containsOne("import defpackage.A;")
				.containsOne("public A a;");
	}

	@Test
	public void testNoImports() {
		args.setUseImports(false);
		List<ClassNode> clsList = loadFromSmaliFiles();
		// class A moved to 'defpackage', but use full names
		assertThat(searchCls(clsList, "A"))
				.code()
				.containsOne("package defpackage;");
		assertThat(searchCls(clsList, "pkg.B"))
				.code()
				.doesNotContain("import")
				.containsOne("public defpackage.A a;");
	}

	@Test
	public void testDeobf() {
		enableDeobfuscation();
		List<ClassNode> clsList = loadFromSmaliFiles();
		// package for class A deobfuscated
		assertThat(searchCls(clsList, "pkg.B"))
				.code()
				.containsOne("import p000.C0000A;")
				.containsOne("public C0000A f0a;");
	}

	@Test
	public void testRenameDisabled() {
		disableCompilation();
		args.setRenameFlags(Collections.emptySet());
		List<ClassNode> clsList = loadFromSmaliFiles();
		// no renaming, code will not compile
		assertThat(searchCls(clsList, "A"))
				.code()
				.containsOne("// default package");
		assertThat(searchCls(clsList, "pkg.B"))
				.code()
				.doesNotContain("import") // omit import
				.containsOne("public A a;");
	}
}
