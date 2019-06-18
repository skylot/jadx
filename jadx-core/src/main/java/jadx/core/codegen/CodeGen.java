package jadx.core.codegen;

import java.util.concurrent.Callable;

import jadx.api.JadxArgs;
import jadx.core.codegen.json.JsonCodeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CodeGen {

	public static void generate(ClassNode cls) {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			cls.setCode(CodeWriter.EMPTY);
		} else {
			JadxArgs args = cls.root().getArgs();
			switch (args.getOutputFormat()) {
				case JAVA:
					generateJavaCode(cls, args);
					break;

				case JSON:
					generateJson(cls);
					break;
			}
		}
	}

	private static void generateJavaCode(ClassNode cls, JadxArgs args) {
		ClassGen clsGen = new ClassGen(cls, args);
		CodeWriter code = wrapCodeGen(cls, clsGen::makeClass);
		cls.setCode(code);
	}

	private static void generateJson(ClassNode cls) {
		JsonCodeGen codeGen = new JsonCodeGen(cls);
		String clsJson = wrapCodeGen(cls, codeGen::process);
		cls.setCode(new CodeWriter(clsJson));
	}

	private static <R> R wrapCodeGen(ClassNode cls, Callable<R> codeGenFunc) {
		try {
			return codeGenFunc.call();
		} catch (Exception e) {
			if (cls.contains(AFlag.RESTART_CODEGEN)) {
				cls.remove(AFlag.RESTART_CODEGEN);
				try {
					return codeGenFunc.call();
				} catch (Exception ex) {
					throw new JadxRuntimeException("Code generation error after restart", ex);
				}
			} else {
				throw new JadxRuntimeException("Code generation error", e);
			}
		}
	}

	private CodeGen() {
	}
}
