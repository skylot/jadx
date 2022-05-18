package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

public class VarNodeAdapter extends BaseDataAdapter<VarNode> {
	private final MethodNodeAdapter mthAdapter;

	public VarNodeAdapter(MethodNodeAdapter mthAdapter) {
		this.mthAdapter = mthAdapter;
	}

	@Override
	public void write(DataOutput out, VarNode value) throws IOException {
		mthAdapter.write(out, value.getMth());
		out.writeShort(value.getReg());
		out.writeShort(value.getSsa());
		ArgTypeAdapter.INSTANCE.write(out, value.getType());
		writeNullableUTF(out, value.getName());
	}

	@Override
	public VarNode read(DataInput in) throws IOException {
		MethodNode mth = mthAdapter.read(in);
		int reg = in.readShort();
		int ssa = in.readShort();
		ArgType type = ArgTypeAdapter.INSTANCE.read(in);
		String name = readNullableUTF(in);
		return new VarNode(mth, reg, ssa, type, name);
	}
}
