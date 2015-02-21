package jadx.core.dex.visitors;

import jadx.api.IJadxArgs;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.CodegenException;

import java.io.File;

public class SaveCode extends AbstractVisitor {
	private final File dir;
	private final IJadxArgs args;

	public SaveCode(File dir, IJadxArgs args) {
		this.args = args;
		this.dir = dir;
	}

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		save(dir, args, cls);
		return false;
	}

	public static void save(File dir, IJadxArgs args, ClassNode cls) {
		CodeWriter clsCode = cls.getCode();
		String fileName = cls.getClassInfo().getFullPath() + ".java";
		if (args.isFallbackMode()) {
			fileName += ".jadx";
		}
		clsCode.save(dir, fileName);
	}
}
