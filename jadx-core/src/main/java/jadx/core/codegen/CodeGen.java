package jadx.core.codegen;

import java.util.concurrent.Callable;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.codegen.json.JsonCodeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CodeGen {

	public static ICodeInfo generate(ClassNode cls) {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			return ICodeInfo.EMPTY;
		}
		JadxArgs args = cls.root().getArgs();
		switch (args.getOutputFormat()) {
			case JAVA:
				return generateJavaCode(cls, args);

			case JSON:
				return generateJson(cls);

			default:
				throw new JadxRuntimeException("Unknown output format");
		}
	}

	private static ICodeInfo generateJavaCode(ClassNode cls, JadxArgs args) {
		ClassGen clsGen = new ClassGen(cls, args);
		return wrapCodeGen(cls, clsGen::makeClass);
	}

	private static ICodeInfo generateJson(ClassNode cls) {
		JsonCodeGen codeGen = new JsonCodeGen(cls);
		String clsJson = wrapCodeGen(cls, codeGen::process);
		return new SimpleCodeInfo(clsJson);
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
