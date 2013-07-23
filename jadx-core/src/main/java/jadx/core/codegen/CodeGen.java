package jadx.core.codegen;

import jadx.api.IJadxArgs;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.CodegenException;

import java.io.File;

public class CodeGen extends AbstractVisitor {

	private final File dir;
	private final IJadxArgs args;

	public CodeGen(IJadxArgs args) {
		this.args = args;
		this.dir = args.getOutDir();
	}

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		ClassGen clsGen = new ClassGen(cls, null, isFallbackMode());
		CodeWriter clsCode = clsGen.makeClass();
		clsCode.finish();
		cls.setCode(clsCode);
		return false;
	}

	public boolean isFallbackMode() {
		return args.isFallbackMode();
	}

}
