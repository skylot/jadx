package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.VarRef;

public class VarRefAdapter implements DataAdapter<VarRef> {

	public static final VarRefAdapter INSTANCE = new VarRefAdapter();

	@Override
	public void write(DataOutput out, VarRef value) throws IOException {
		int refPos = value.getRefPos();
		DataAdapterHelper.writeUVInt(out, refPos);
	}

	@Override
	public VarRef read(DataInput in) throws IOException {
		int refPos = DataAdapterHelper.readUVInt(in);
		return VarRef.fromPos(refPos);
	}
}
