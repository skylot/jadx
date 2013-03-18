package jadx.codegen;

import jadx.JadxArgs;
import jadx.dex.nodes.ClassNode;
import jadx.dex.visitors.AbstractVisitor;
import jadx.utils.exceptions.CodegenException;

import java.io.File;

public class CodeGen extends AbstractVisitor {

	private final File dir;
	private final JadxArgs args;

	public CodeGen(JadxArgs args) {
		this.args = args;
		this.dir = args.getOutDir();
	}

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		ClassGen clsGen = new ClassGen(cls, null, isFallbackMode());
		CodeWriter clsCode = clsGen.makeClass();
		String fileName = cls.getClassInfo().getFullPath() + ".java";
		if (isFallbackMode())
			fileName += ".jadx";
		clsCode.save(dir, fileName);
		return false;
	}

	public boolean isFallbackMode() {
		return args.isFallbackMode();
	}

}
