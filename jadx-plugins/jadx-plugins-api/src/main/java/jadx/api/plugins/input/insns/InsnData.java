package jadx.api.plugins.input.insns;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.custom.ICustomPayload;

public interface InsnData {

	void decode();

	int getOffset();

	Opcode getOpcode();

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

	@Nullable
	ICustomPayload getPayload();
}
