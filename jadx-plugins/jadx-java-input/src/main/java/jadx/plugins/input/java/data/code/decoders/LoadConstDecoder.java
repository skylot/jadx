package jadx.plugins.input.java.data.code.decoders;

import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.ConstantType;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.code.CodeDecodeState;
import jadx.plugins.input.java.data.code.JavaInsnData;
import jadx.plugins.input.java.data.code.StackState.SVType;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class LoadConstDecoder implements IJavaInsnDecoder {
	private final boolean wide;

	public LoadConstDecoder(boolean wide) {
		this.wide = wide;
	}

	@Override
	public void decode(CodeDecodeState state) {
		DataReader reader = state.reader();
		JavaInsnData insn = state.insn();
		int index;
		if (wide) {
			index = reader.readU2();
		} else {
			index = reader.readU1();
		}
		ConstPoolReader constPoolReader = insn.constPoolReader();
		ConstantType constType = constPoolReader.jumpToConst(index);
		switch (constType) {
			case INTEGER:
			case FLOAT:
				insn.setLiteral(constPoolReader.readU4());
				insn.setOpcode(Opcode.CONST);
				state.push(0, SVType.NARROW);
				break;

			case LONG:
			case DOUBLE:
				insn.setLiteral(constPoolReader.readU8());
				insn.setOpcode(Opcode.CONST_WIDE);
				state.push(0, SVType.WIDE);
				break;

			case STRING:
				insn.setIndex(constPoolReader.readU2());
				insn.setOpcode(Opcode.CONST_STRING);
				state.push(0, SVType.NARROW);
				break;

			case UTF8:
				insn.setIndex(index);
				insn.setOpcode(Opcode.CONST_STRING);
				state.push(0, SVType.NARROW);
				break;

			case CLASS:
				insn.setIndex(index);
				insn.setOpcode(Opcode.CONST_CLASS);
				state.push(0, SVType.NARROW);
				break;

			default:
				throw new JavaClassParseException("Unsupported constant type: " + constType);
		}
	}
}
