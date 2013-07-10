package jadx.api;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;

public final class JavaClass {

	private final Decompiler decompiler;
	private final ClassNode cls;

	JavaClass(Decompiler decompiler, ClassNode classNode) {
		this.decompiler = decompiler;
		this.cls = classNode;
	}

	public String getCode() {
		CodeWriter code = cls.getCode();
		if(code == null) {
			decompiler.processClass(cls);
			code = cls.getCode();
		}
		return code != null ? code.toString() : "error processing class";
	}

	public String getFullName() {
		return cls.getFullName();
	}

	public String getShortName() {
		return cls.getShortName();
	}

	public String getPackage() {
		return cls.getPackage();
	}

	@Override
	public String toString() {
		return getFullName();
	}
}
