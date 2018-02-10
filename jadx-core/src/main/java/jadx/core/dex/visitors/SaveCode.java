package jadx.core.dex.visitors;

import java.io.File;

import jadx.api.JadxArgs;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;

public class SaveCode {

	private SaveCode() {}

	public static void save(File dir, JadxArgs args, ClassNode cls) {
		CodeWriter clsCode = cls.getCode();
		String fileName = cls.getClassInfo().getFullPath() + ".java";
		if (args.isFallbackMode()) {
			fileName += ".jadx";
		}
		clsCode.save(dir, fileName);
	}
}
