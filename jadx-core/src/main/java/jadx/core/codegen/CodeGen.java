package jadx.core.codegen;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.CodegenException;

public class CodeGen extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		ClassGen clsGen = new ClassGen(cls, cls.root().getArgs());
		CodeWriter clsCode = clsGen.makeClass();
		clsCode.finish();
		cls.setCode(clsCode);
		return false;
	}
}
