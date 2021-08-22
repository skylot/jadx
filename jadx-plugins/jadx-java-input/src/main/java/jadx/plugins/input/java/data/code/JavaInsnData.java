package jadx.plugins.input.java.data.code;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.Opcode;
import jadx.api.plugins.input.insns.custom.ICustomPayload;
import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.code.decoders.IJavaInsnDecoder;

public class JavaInsnData implements InsnData {

	private final CodeDecodeState state;

	private JavaInsnInfo insnInfo;
	private Opcode opcode;
	private boolean decoded;
	private int opcodeUnit;
	private int payloadSize;
	private int insnStart;
	private int offset;
	private int regsCount;
	private int[] argsReg = new int[16];
	private int resultReg;
	private long literal;
	private int target;
	private int index;
	@Nullable
	private ICustomPayload payload;

	public JavaInsnData(CodeDecodeState state) {
		this.state = state;
	}

	@Override
	public void decode() {
		IJavaInsnDecoder decoder = insnInfo.getDecoder();
		if (decoder != null) {
			decoder.decode(state);
			state.decoded();
		}
		decoded = true;
	}

	public void skip() {
		IJavaInsnDecoder decoder = insnInfo.getDecoder();
		if (decoder != null) {
			decoder.skip(state);
		}
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public int getFileOffset() {
		return insnStart;
	}

	@Override
	public Opcode getOpcode() {
		return opcode;
	}

	public void setOpcode(Opcode opcode) {
		this.opcode = opcode;
	}

	@Override
	public String getOpcodeMnemonic() {
		return insnInfo.getName();
	}

	@Override
	public byte[] getByteCode() {
		DataReader reader = state.reader();
		int startOffset = reader.getOffset();
		try {
			reader.absPos(insnStart);
			return reader.readBytes(1 + payloadSize);
		} finally {
			reader.absPos(startOffset);
		}
	}

	@Override
	public InsnIndexType getIndexType() {
		return insnInfo.getIndexType();
	}

	@Override
	public int getRawOpcodeUnit() {
		return opcodeUnit;
	}

	@Override
	public int getRegsCount() {
		return regsCount;
	}

	@Override
	public int getReg(int argNum) {
		return argsReg[argNum];
	}

	@Override
	public int getResultReg() {
		return resultReg;
	}

	public void setResultReg(int resultReg) {
		this.resultReg = resultReg;
	}

	@Override
	public long getLiteral() {
		return literal;
	}

	@Override
	public int getTarget() {
		return target;
	}

	@Override
	public int getIndex() {
		return index;
	}

	public int getPayloadSize() {
		return payloadSize;
	}

	@Override
	public String getIndexAsString() {
		return constPoolReader().getUtf8(index);
	}

	@Override
	public String getIndexAsType() {
		if (insnInfo.getOpcode() == 0xbc) { // newarray
			return ArrayType.byValue(index);
		}
		return constPoolReader().getClass(index);
	}

	@Override
	public IFieldRef getIndexAsField() {
		return constPoolReader().getFieldRef(index);
	}

	@Override
	public IMethodRef getIndexAsMethod() {
		return constPoolReader().getMethodRef(index);
	}

	@Override
	public ICallSite getIndexAsCallSite() {
		return constPoolReader().getCallSite(index);
	}

	@Override
	public IMethodProto getIndexAsProto(int protoIndex) {
		return null;
	}

	@Override
	public IMethodHandle getIndexAsMethodHandle() {
		return null;
	}

	@Override
	public @Nullable ICustomPayload getPayload() {
		return payload;
	}

	public void setInsnInfo(JavaInsnInfo insnInfo) {
		this.insnInfo = insnInfo;
	}

	public boolean isDecoded() {
		return decoded;
	}

	public void setDecoded(boolean decoded) {
		this.decoded = decoded;
	}

	public void setOpcodeUnit(int opcodeUnit) {
		this.opcodeUnit = opcodeUnit;
	}

	public void setPayloadSize(int payloadSize) {
		this.payloadSize = payloadSize;
	}

	public void setInsnStart(int insnStart) {
		this.insnStart = insnStart;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setArgReg(int arg, int reg) {
		this.argsReg[arg] = reg;
	}

	public void setRegsCount(int regsCount) {
		this.regsCount = regsCount;
		if (argsReg.length < regsCount) {
			argsReg = new int[regsCount];
		}
	}

	public int[] getRegsArray() {
		return argsReg;
	}

	public void setLiteral(long literal) {
		this.literal = literal;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setPayload(ICustomPayload payload) {
		this.payload = payload;
	}

	public ConstPoolReader constPoolReader() {
		return state.clsData().getConstPoolReader();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("0x%04X", offset));
		sb.append(": ").append(getOpcode());
		if (insnInfo == null) {
			sb.append(String.format("(0x%04X)", opcodeUnit));
		} else {
			int regsCount = getRegsCount();
			if (isDecoded()) {
				sb.append(' ');
				for (int i = 0; i < regsCount; i++) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append("r").append(argsReg[i]);
				}
			}
		}
		return sb.toString();
	}
}
