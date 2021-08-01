package jadx.plugins.input.java.data.code;

import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.code.decoders.IJavaInsnDecoder;

public class JavaInsnInfo {
	private final int opcode;
	private final String name;
	private final int payloadSize;
	private final int regsCount;
	private final Opcode apiOpcode;
	private final InsnIndexType indexType;
	private final IJavaInsnDecoder decoder;

	public JavaInsnInfo(int opcode, String name, int payloadSize, int regsCount, Opcode apiOpcode,
			InsnIndexType indexType, IJavaInsnDecoder decoder) {
		this.opcode = opcode;
		this.name = name;
		this.payloadSize = payloadSize;
		this.regsCount = regsCount;
		this.apiOpcode = apiOpcode;
		this.indexType = indexType;
		this.decoder = decoder;
	}

	public int getOpcode() {
		return opcode;
	}

	public String getName() {
		return name;
	}

	public int getPayloadSize() {
		return payloadSize;
	}

	public int getRegsCount() {
		return regsCount;
	}

	public Opcode getApiOpcode() {
		return apiOpcode;
	}

	public InsnIndexType getIndexType() {
		return indexType;
	}

	public IJavaInsnDecoder getDecoder() {
		return decoder;
	}

	@Override
	public String toString() {
		return "0x" + Integer.toHexString(opcode) + ": " + name;
	}
}
