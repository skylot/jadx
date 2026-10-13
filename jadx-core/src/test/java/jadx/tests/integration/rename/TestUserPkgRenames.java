package jadx.tests.integration.rename;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUserPkgRenames extends SmaliTest {

	@Test
	public void testMovePkg() {
		addPkgRename("a.b", "com.foo");
		List<ClassNode> classes = loadFromSmaliFiles();

		ClassNode first = searchCls(classes, "a.b.First");
		assertThat(first).code().containsOne("package com.foo;");
		assertThat(first.getClassInfo().getAliasPkg()).isEqualTo("com.foo");

		jadxDecompiler.reloadCodeData();
		assertThat(first.getClassInfo().getAliasPkg()).isEqualTo("com.foo");
	}

	private void addPkgRename(String pkg, String newName) {
		getCodeData().getRenames().add(new JadxCodeRename(JadxNodeRef.forPkg(pkg), newName));
	}
}
