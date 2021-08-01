package jadx.plugins.input.dex.smali;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.plugins.input.dex.sections.DexMethodData;
import jadx.plugins.input.dex.sections.DexMethodRef;

import static jadx.api.plugins.input.data.AccessFlagsScope.METHOD;

// TODO: not finished
public class SmaliPrinter {

	public static String printMethod(DexMethodData mth) {
		SmaliCodeWriter codeWriter = new SmaliCodeWriter();
		codeWriter.startLine(".method ");
		codeWriter.add(AccessFlags.format(mth.getAccessFlags(), METHOD));

		DexMethodRef methodRef = mth.getMethodRef();
		methodRef.load();
		codeWriter.add(methodRef.getName());
		codeWriter.add('(').addArgs(methodRef.getArgTypes()).add(')');
		codeWriter.add(methodRef.getReturnType());
		codeWriter.incIndent();

		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader != null) {
			codeWriter.startLine(".registers ").add(codeReader.getRegistersCount());
			SmaliInsnFormat insnFormat = SmaliInsnFormat.getInstance();
			InsnFormatterInfo formatterInfo = new InsnFormatterInfo(codeWriter, mth);
			codeReader.visitInstructions(insn -> {
				codeWriter.startLine();
				formatterInfo.setInsn(insn);
				insnFormat.format(formatterInfo);
			});
			codeWriter.decIndent();
		}
		codeWriter.startLine(".end method");
		return codeWriter.getCode();
	}
}
