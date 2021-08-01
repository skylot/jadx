package jadx.plugins.input.java.data.code.decoders;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.code.CodeDecodeState;
import jadx.plugins.input.java.data.code.JavaInsnData;

public class InvokeDecoder implements IJavaInsnDecoder {
	private final int payloadSize;
	private final Opcode apiOpcode;

	public InvokeDecoder(int payloadSize, Opcode apiOpcode) {
		this.payloadSize = payloadSize;
		this.apiOpcode = apiOpcode;
	}

	@Override
	public void decode(CodeDecodeState state) {
		DataReader reader = state.reader();
		int mthIdx = reader.readS2();
		if (payloadSize == 4) {
			reader.skip(2);
		}
		JavaInsnData insn = state.insn();
		insn.setIndex(mthIdx);
		boolean instanceCall;
		IMethodProto mthProto;
		if (apiOpcode == Opcode.INVOKE_CUSTOM) {
			ICallSite callSite = insn.getIndexAsCallSite();
			insn.setPayload(callSite);
			mthProto = (IMethodProto) callSite.getValues().get(2).getValue();
			instanceCall = false; // 'this' arg already included in proto args
		} else {
			IMethodRef mthRef = insn.getIndexAsMethod();
			mthRef.load();
			insn.setPayload(mthRef);
			mthProto = mthRef;
			instanceCall = apiOpcode != Opcode.INVOKE_STATIC;
		}

		int argsCount = mthProto.getArgTypes().size();
		if (instanceCall) {
			argsCount++;
		}
		insn.setRegsCount(argsCount * 2); // allocate twice of the size for worst case
		int[] regs = insn.getRegsArray();

		// calculate actual count of registers
		// set '1' in regs to be filled with stack values later, '0' for skip
		int regsCount = 0;
		if (instanceCall) {
			regs[regsCount++] = 1;
		}
		for (String type : mthProto.getArgTypes()) {
			int size = getRegsCountForType(type);
			regs[regsCount++] = 1;
			if (size == 2) {
				regs[regsCount++] = 0;
			}
		}
		insn.setRegsCount(regsCount);
		for (int i = regsCount - 1; i >= 0; i--) {
			if (regs[i] == 1) {
				state.pop(i);
			}
		}
		String returnType = mthProto.getReturnType();
		if (!returnType.equals("V")) {
			insn.setResultReg(state.push(returnType));
		} else {
			insn.setResultReg(-1);
		}
	}

	private int getRegsCountForType(String type) {
		char c = type.charAt(0);
		if (c == 'J' || c == 'D') {
			return 2;
		}
		return 1;
	}
}
