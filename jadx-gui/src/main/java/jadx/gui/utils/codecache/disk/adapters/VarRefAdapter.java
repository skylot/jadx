package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.VarRef;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

public class VarRefAdapter extends BaseDataAdapter<VarRef> {
	private final MethodNodeAdapter mthAdapter;

	public VarRefAdapter(MethodNodeAdapter mthAdapter) {
		this.mthAdapter = mthAdapter;
	}

	@Override
	public void write(DataOutput out, VarRef value) throws IOException {
		mthAdapter.write(out, value.getMth());
		out.writeInt(value.getReg());
		out.writeInt(value.getSsa());
		ArgTypeAdapter.INSTANCE.write(out, value.getType());
		writeNullableUTF(out, value.getName());
	}

	@Override
	public VarRef read(DataInput in) throws IOException {
		MethodNode mth = mthAdapter.read(in);
		int reg = in.readInt();
		int ssa = in.readInt();
		ArgType type = ArgTypeAdapter.INSTANCE.read(in);
		String name = readNullableUTF(in);
		return new VarRef(mth, reg, ssa, type, name);
	}
}
