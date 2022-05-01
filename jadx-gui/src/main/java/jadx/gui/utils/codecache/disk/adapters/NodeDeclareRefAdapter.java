package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;

public class NodeDeclareRefAdapter implements DataAdapter<NodeDeclareRef> {
	private final CodeAnnotationAdapter refAdapter;

	public NodeDeclareRefAdapter(CodeAnnotationAdapter refAdapter) {
		this.refAdapter = refAdapter;
	}

	@Override
	public void write(DataOutput out, NodeDeclareRef value) throws IOException {
		refAdapter.write(out, value.getNode());
		out.writeInt(value.getDefPos());
	}

	@Override
	public NodeDeclareRef read(DataInput in) throws IOException {
		ICodeNodeRef ref = (ICodeNodeRef) refAdapter.read(in);
		int defPos = in.readInt();
		NodeDeclareRef nodeDeclareRef = new NodeDeclareRef(ref);
		nodeDeclareRef.setDefPos(defPos);
		// restore def position if loading metadata without actual decompilation
		ref.setDefPosition(defPos);
		return nodeDeclareRef;
	}
}
