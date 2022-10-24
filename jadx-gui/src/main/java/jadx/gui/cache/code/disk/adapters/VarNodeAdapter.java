package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

import static jadx.gui.cache.code.disk.adapters.DataAdapterHelper.readNullableUTF;
import static jadx.gui.cache.code.disk.adapters.DataAdapterHelper.readUVInt;
import static jadx.gui.cache.code.disk.adapters.DataAdapterHelper.writeNullableUTF;
import static jadx.gui.cache.code.disk.adapters.DataAdapterHelper.writeUVInt;

public class VarNodeAdapter implements DataAdapter<VarNode> {
	private final MethodNodeAdapter mthAdapter;

	public VarNodeAdapter(MethodNodeAdapter mthAdapter) {
		this.mthAdapter = mthAdapter;
	}

	@Override
	public void write(DataOutput out, VarNode value) throws IOException {
		mthAdapter.write(out, value.getMth());
		writeUVInt(out, value.getReg());
		writeUVInt(out, value.getSsa());
		ArgTypeAdapter.INSTANCE.write(out, value.getType());
		writeNullableUTF(out, value.getName());
	}

	@Override
	public VarNode read(DataInput in) throws IOException {
		MethodNode mth = mthAdapter.read(in);
		int reg = readUVInt(in);
		int ssa = readUVInt(in);
		ArgType type = ArgTypeAdapter.INSTANCE.read(in);
		String name = readNullableUTF(in);
		return new VarNode(mth, reg, ssa, type, name);
	}
}
