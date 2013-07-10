package jadx.core.dex.visitors;

import jadx.api.IJadxArgs;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.CodegenException;

import java.io.File;

public class SaveCode extends AbstractVisitor {

	private final File dir;
	private final IJadxArgs args;

	public SaveCode(IJadxArgs args) {
		this.args = args;
		this.dir = args.getOutDir();
	}

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		CodeWriter clsCode = cls.getCode();

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
