package jadx.api.plugins.input.insns;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
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

	IMethodData getIndexAsMethod();

	@Nullable
	ICustomPayload getPayload();
}
