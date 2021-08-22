package jadx.api.plugins.input.insns;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.custom.ICustomPayload;

public interface InsnData {

	void decode();

	int getOffset(); // offset within method

	int getFileOffset(); // offset within dex file

	Opcode getOpcode();

	String getOpcodeMnemonic();

	byte[] getByteCode();

	InsnIndexType getIndexType();

	int getRawOpcodeUnit();

	int getRegsCount();

	int getReg(int argNum);

	/**
	 * Workaround to set result reg without additional move-result insn
	 *
	 * @return result reg number or -1 if not needed
	 */
	int getResultReg();

	long getLiteral();

	int getTarget();

	int getIndex();

	String getIndexAsString();

	String getIndexAsType();

	IFieldRef getIndexAsField();

	IMethodRef getIndexAsMethod();

	ICallSite getIndexAsCallSite();

	IMethodProto getIndexAsProto(int protoIndex);

	IMethodHandle getIndexAsMethodHandle();

	ICustomPayload getPayload();
}
