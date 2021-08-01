package jadx.plugins.input.java.data.code.decoders;

import jadx.api.plugins.input.insns.custom.impl.SwitchPayload;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.code.CodeDecodeState;
import jadx.plugins.input.java.data.code.JavaInsnData;

public class TableSwitchDecoder implements IJavaInsnDecoder {

	@Override
	public void decode(CodeDecodeState state) {
		read(state, false);
	}

	@Override
	public void skip(CodeDecodeState state) {
		read(state, true);
	}

	private static void read(CodeDecodeState state, boolean skip) {
		DataReader reader = state.reader();
		JavaInsnData insn = state.insn();
		int dataOffset = reader.getOffset();
		int insnOffset = insn.getOffset();
		reader.skip(3 - insnOffset % 4);
		int defTarget = insnOffset + reader.readS4();
		int low = reader.readS4();
		int high = reader.readS4();
		int count = high - low + 1;
		if (skip) {
			reader.skip(count * 4);
		} else {
			state.pop(0);
			int[] keys = new int[count];
			int[] targets = new int[count];
			for (int i = 0; i < count; i++) {
				int target = insnOffset + reader.readS4();
				keys[i] = low + i;
				targets[i] = target;
				state.registerJump(target);
			}
			insn.setTarget(defTarget);
			state.registerJump(defTarget);
			insn.setPayload(new SwitchPayload(count, keys, targets));
		}
		insn.setPayloadSize(reader.getOffset() - dataOffset);
	}
}
