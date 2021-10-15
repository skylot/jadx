package jadx.core.dex.instructions.invokedynamic;

import java.util.List;
import java.util.Objects;

import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.EncodedValueUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CustomStringConcat {

	public static boolean isStringConcat(List<EncodedValue> values) {
		if (values.size() < 4) {
			return false;
		}
		IMethodHandle methodHandle = (IMethodHandle) values.get(0).getValue();
		if (methodHandle.getType() != MethodHandleType.INVOKE_STATIC) {
			return false;
		}
		IMethodRef methodRef = methodHandle.getMethodRef();
		if (!methodRef.getName().equals("makeConcatWithConstants")) {
			return false;
		}
		if (!methodRef.getParentClassType().equals("Ljava/lang/invoke/StringConcatFactory;")) {
			return false;
		}
		if (!Objects.equals(values.get(1).getValue(), "makeConcatWithConstants")) {
			return false;
		}
		if (values.get(3).getType() != EncodedType.ENCODED_STRING) {
			return false;
		}
		return true;
	}

	public static InsnNode buildStringConcat(InsnData insn, boolean isRange, List<EncodedValue> values) {
		try {
			int argsCount = values.size() - 3 + insn.getRegsCount();
			InsnNode concat = new InsnNode(InsnType.STR_CONCAT, argsCount);
			String recipe = (String) values.get(3).getValue();
			processRecipe(recipe, concat, values, insn);
			int resReg = insn.getResultReg();
			if (resReg != -1) {
				concat.setResult(InsnArg.reg(resReg, ArgType.STRING));
			}
			return concat;
		} catch (Exception e) {
			InsnNode nop = new InsnNode(InsnType.NOP, 0);
			nop.add(AFlag.SYNTHETIC);
			nop.addAttr(AType.JADX_ERROR, new JadxError("Failed to process dynamic string concat: " + e.getMessage(), e));
			return nop;
		}
	}

	private static void processRecipe(String recipe, InsnNode concat, List<EncodedValue> values, InsnData insn) {
		int len = recipe.length();
		int offset = 0;
		int argNum = 0;
		int constNum = 4;
		StringBuilder sb = new StringBuilder(len);
		while (offset < len) {
			int cp = recipe.codePointAt(offset);
			offset += Character.charCount(cp);
			boolean argTag = cp == 1;
			boolean constTag = cp == 2;
			if (argTag || constTag) {
				if (sb.length() != 0) {
					concat.addArg(InsnArg.wrapArg(new ConstStringNode(sb.toString())));
					sb.setLength(0);
				}
				if (argTag) {
					concat.addArg(InsnArg.reg(insn, argNum++, ArgType.UNKNOWN));
				} else {
					InsnArg constArg = buildInsnArgFromEncodedValue(values.get(constNum++));
					concat.addArg(constArg);
				}
			} else {
				sb.appendCodePoint(cp);
			}
		}
		if (sb.length() != 0) {
			concat.addArg(InsnArg.wrapArg(new ConstStringNode(sb.toString())));
		}
	}

	private static InsnArg buildInsnArgFromEncodedValue(EncodedValue encodedValue) {
		Object value = EncodedValueUtils.convertToConstValue(encodedValue);
		if (value == null) {
			return InsnArg.lit(0, ArgType.UNKNOWN);
		}
		if (value instanceof LiteralArg) {
			return ((LiteralArg) value);
		}
		if (value instanceof ArgType) {
			return InsnArg.wrapArg(new ConstClassNode((ArgType) value));
		}
		if (value instanceof String) {
			return InsnArg.wrapArg(new ConstStringNode(((String) value)));
		}
		throw new JadxRuntimeException("Can't build insn arg from encoded value: " + encodedValue);
	}
}
