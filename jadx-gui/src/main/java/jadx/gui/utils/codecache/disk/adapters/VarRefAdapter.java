package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.VarRef;

public class VarRefAdapter extends BaseDataAdapter<VarRef> {

	public static final VarRefAdapter INSTANCE = new VarRefAdapter();

	@Override
	public void write(DataOutput out, VarRef value) throws IOException {
		int refPos = value.getRefPos();
		if (refPos == 0) {
			throw new RuntimeException("Variable refPos is zero: " + value);
		}
		out.writeShort(refPos);
	}

	@Override
	public VarRef read(DataInput in) throws IOException {
		return VarRef.fromPos(in.readShort());
	}
}
