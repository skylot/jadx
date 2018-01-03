package jadx.api;

import java.util.List;

import jadx.core.dex.nodes.ClassNode;

public class Factory {

	public static JavaPackage newPackage(String name, List<JavaClass> classes) {
		return new JavaPackage(name, classes);
	}

	public static JavaClass newClass(JadxDecompiler decompiler, ClassNode classNode) {
		return new JavaClass(classNode, decompiler);
	}
}
