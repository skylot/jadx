package jadx.core.codegen;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CodeGen {

	public static void generate(ClassNode cls) throws CodegenException {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			cls.setCode(CodeWriter.EMPTY);
		} else {
			ClassGen clsGen = new ClassGen(cls, cls.root().getArgs());
			CodeWriter code;
			try {
				code = clsGen.makeClass();
			} catch (Exception e) {
				if (cls.contains(AFlag.RESTART_CODEGEN)) {
					cls.remove(AFlag.RESTART_CODEGEN);
					code = clsGen.makeClass();
				} else {
					throw new JadxRuntimeException("Code generation error", e);
				}
			}
			cls.setCode(code);
		}
	}

	private CodeGen() {
	}
}
