package jadx.core.codegen;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.CodegenException;

public class CodeGen {

	public static void generate(ClassNode cls) throws CodegenException {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			cls.setCode(CodeWriter.EMPTY);
		} else {
			ClassGen clsGen = new ClassGen(cls, cls.root().getArgs());
			cls.setCode(clsGen.makeClass());
		}
	}

	private CodeGen() {
	}
}
