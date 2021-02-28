package jadx.api.plugins.input.insns;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.*;
import jadx.api.plugins.input.insns.custom.ICustomPayload;

public interface InsnData {

	void decode();

	int getOffset(); // offset within method

	int getFileOffset(); // offset within dex file

	Opcode getOpcode();

	byte[] getByteCode();

	InsnIndexType getIndexType();

	int getRawOpcodeUnit();

	int getRegsCount();

	int getReg(int argNum);

	long getLiteral();

	int getTarget();

	int getIndex();

	String getIndexAsString();

	String getIndexAsType();

	IFieldData getIndexAsField();

	IMethodRef getIndexAsMethod();

	ICallSite getIndexAsCallSite();

	IMethodProto getIndexAsProto(int protoIndex);

	IMethodHandle getIndexAsMethodHandle();

	@Nullable
	ICustomPayload getPayload();
}
