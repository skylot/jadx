package jadx.core.utils;

import jadx.core.dex.instructions.InsnType;
import jadx.core.utils.exceptions.JadxRuntimeException;

import com.android.dx.io.instructions.DecodedInstruction;

public class InsnUtils {

	public static int getArg(DecodedInstruction insn, int arg) {
		switch (arg) {
			case 0:
				return insn.getA();
			case 1:
				return insn.getB();
			case 2:
				return insn.getC();
			case 3:
				return insn.getD();
			case 4:
				return insn.getE();
		}
		throw new JadxRuntimeException("Wrong argument number: " + arg);
	}

	public static String formatOffset(int offset) {
		if (offset < 0) {
			return "?";
		} else {
			return String.format("0x%04x", offset);
		}
	}

	public static String insnTypeToString(InsnType type) {
		return insnTypeToString(type.toString());
	}

	public static String insnTypeToString(String str) {
		return String.format("%s  ", str);
	}

	public static String indexToString(Object index) {
		if (index == null) {
			return "";
		}
		if (index instanceof String) {
			return "\"" + index + "\"";
		} else {
			return " " + index.toString();
		}
	}
}
