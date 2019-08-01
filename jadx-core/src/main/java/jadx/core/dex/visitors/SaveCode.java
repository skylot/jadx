package jadx.core.dex.visitors;

import java.io.File;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SaveCode {

	private SaveCode() {
	}

	public static void save(File dir, ClassNode cls, ICodeInfo code) {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		if (code == null) {
			throw new JadxRuntimeException("Code not generated for class " + cls.getFullName());
		}
		if (code == CodeWriter.EMPTY) {
			return;
		}
		CodeWriter clsCode;
		if (code instanceof CodeWriter) {
			clsCode = (CodeWriter) code;
		} else {
			// TODO: move 'save' method from CodeWriter
			clsCode = new CodeWriter(code.getCodeStr());
		}
		String fileName = cls.getClassInfo().getAliasFullPath() + getFileExtension(cls);
		clsCode.save(dir, fileName);
	}

	private static String getFileExtension(ClassNode cls) {
		JadxArgs.OutputFormatEnum outputFormat = cls.root().getArgs().getOutputFormat();
		switch (outputFormat) {
			case JAVA:
				return ".java";

			case JSON:
				return ".json";

			default:
				throw new JadxRuntimeException("Unknown output format: " + outputFormat);
		}
	}
}
