package jadx.plugins.input.java.data.code.decoders;

import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.code.CodeDecodeState;
import jadx.plugins.input.java.data.code.JavaInsnData;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class WideDecoder implements IJavaInsnDecoder {
	private static final int IINC = 0x84;

	@Override
	public void decode(CodeDecodeState state) {
		DataReader reader = state.reader();
		JavaInsnData insn = state.insn();
		int opcode = reader.readU1();
		if (opcode == IINC) {
			int varNum = reader.readU2();
			int constValue = reader.readS2();
			state.local(0, varNum).local(1, varNum).lit(constValue);
			insn.setPayloadSize(5);
			insn.setRegsCount(2);
			insn.setOpcode(Opcode.ADD_INT_LIT);
			return;
		}
		int index = reader.readU2();
		switch (opcode) {
			case 0x15: // iload,
			case 0x17: // fload
			case 0x19: // aload
				state.local(1, index).push(0);
				break;

			case 0x16: // lload
			case 0x18: // dload
				state.local(1, index).pushWide(0);
				break;

			case 0x36:
			case 0x37:
			case 0x38:
			case 0x39:
			case 0x3a:
				// *store
				state.pop(1).local(0, index);
				break;

			default:
				throw new JavaClassParseException("Unexpected opcode in 'wide': 0x" + Integer.toHexString(opcode));
		}
		insn.setPayloadSize(3);
		insn.setRegsCount(2);
		insn.setOpcode(Opcode.MOVE);
	}

	@Override
	public void skip(CodeDecodeState state) {
		DataReader reader = state.reader();
		JavaInsnData insn = state.insn();
		int opcode = reader.readU1();
		if (opcode == IINC) {
			reader.skip(4);
			insn.setPayloadSize(5);
		} else {
			reader.skip(2);
			insn.setPayloadSize(3);
		}
	}
}
