package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.data.annotations.VarDeclareRef;
import jadx.api.data.annotations.VarRef;

public class VarDeclareRefAdapter implements DataAdapter<VarDeclareRef> {
	private final VarRefAdapter varRefAdapter;

	public VarDeclareRefAdapter(VarRefAdapter varRefAdapter) {
		this.varRefAdapter = varRefAdapter;
	}

	@Override
	public void write(DataOutput out, VarDeclareRef value) throws IOException {
		varRefAdapter.write(out, value);
		out.writeInt(value.getSourceLine());
		out.writeInt(value.getDecompiledLine());
		out.writeInt(value.getDefPosition());
	}

	@Override
	public VarDeclareRef read(DataInput in) throws IOException {
		VarRef ref = varRefAdapter.read(in);
		int src = in.readInt();
		int dcl = in.readInt();
		int def = in.readInt();

		VarDeclareRef varDeclareRef = new VarDeclareRef(ref);
		varDeclareRef.setSourceLine(src);
		varDeclareRef.setDecompiledLine(dcl);
		varDeclareRef.setDefPosition(def);
		return varDeclareRef;
	}
}
