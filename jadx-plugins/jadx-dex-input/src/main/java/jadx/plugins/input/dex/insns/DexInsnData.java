package jadx.plugins.input.dex.insns;

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
import jadx.plugins.input.dex.sections.DexCodeReader;
import jadx.plugins.input.dex.sections.SectionReader;

public class DexInsnData implements InsnData {
	private final DexCodeReader codeData;
	private final SectionReader externalReader;
	private final SectionReader secondExtReader;

	private DexInsnInfo insnInfo;
	private boolean decoded;
	private int opcodeUnit;
	private int length;
	private int insnStart;

	private int offset;
	private int[] argsReg = new int[5];
	private int regsCount;
	private long literal;
	private int target;
	private int index;
	@Nullable
	private ICustomPayload payload;

	public DexInsnData(DexCodeReader codeData, SectionReader externalReader) {
		this.codeData = codeData;
		this.externalReader = externalReader;
		this.secondExtReader = externalReader.copy();
	}

	@Override
	public void decode() {
		if (insnInfo != null && !decoded) {
			codeData.decode(this);
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
		DexInsnInfo info = this.insnInfo;
		if (info == null) {
			return Opcode.UNKNOWN;
		}
		return info.getApiOpcode();
	}

	@Override
	public String getOpcodeMnemonic() {
		return DexInsnMnemonics.get(opcodeUnit);
	}

	@Override
	public byte[] getByteCode() {
		return externalReader.getByteCode(insnStart, length * 2); // a unit is 2 bytes
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
		return -1;
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

	@Override
	public InsnIndexType getIndexType() {
		return insnInfo.getIndexType();
	}

	@Override
	public String getIndexAsString() {
		return externalReader.getString(index);
	}

	@Override
	public String getIndexAsType() {
		return externalReader.getType(index);
	}

	@Override
	public IFieldRef getIndexAsField() {
		return externalReader.getFieldRef(index);
	}

	@Override
	public IMethodRef getIndexAsMethod() {
		return externalReader.getMethodRef(index);
	}

	@Override
	public ICallSite getIndexAsCallSite() {
		return externalReader.getCallSite(index, secondExtReader);
	}

	/**
	 * Currently, protoIndex is either being stored at index or target, index for const-method-type,
	 * target for invoke-polymorphic(/range)
	 */
	@Override
	public IMethodProto getIndexAsProto(int protoIndex) {
		return externalReader.getMethodProto(protoIndex);
	}

	@Override
	public IMethodHandle getIndexAsMethodHandle() {
		return externalReader.getMethodHandle(index);
	}

	@Nullable
	@Override
	public ICustomPayload getPayload() {
		return payload;
	}

	public int[] getArgsReg() {
		return argsReg;
	}

	public void setArgsReg(int[] argsReg) {
		this.argsReg = argsReg;
	}

	public void setRegsCount(int regsCount) {
		this.regsCount = regsCount;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setInsnStart(int start) {
		this.insnStart = start;
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

	public boolean isDecoded() {
		return decoded;
	}

	public void setDecoded(boolean decoded) {
		this.decoded = decoded;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public DexInsnInfo getInsnInfo() {
		return insnInfo;
	}

	public void setInsnInfo(DexInsnInfo insnInfo) {
		this.insnInfo = insnInfo;
	}

	public DexCodeReader getCodeData() {
		return codeData;
	}

	public int getOpcodeUnit() {
		return opcodeUnit;
	}

	public void setOpcodeUnit(int opcodeUnit) {
		this.opcodeUnit = opcodeUnit;
	}

	public void setPayload(ICustomPayload payload) {
		this.payload = payload;
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
