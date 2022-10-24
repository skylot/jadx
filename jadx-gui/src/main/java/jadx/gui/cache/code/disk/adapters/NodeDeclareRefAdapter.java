package jadx.gui.cache.code.disk.adapters;

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
		ICodeNodeRef node = value.getNode();
		if (node == null) {
			throw new RuntimeException("Null node in NodeDeclareRef");
		}
		refAdapter.write(out, node);
		DataAdapterHelper.writeUVInt(out, value.getDefPos());
	}

	@Override
	public NodeDeclareRef read(DataInput in) throws IOException {
		ICodeNodeRef ref = (ICodeNodeRef) refAdapter.read(in);
		int defPos = DataAdapterHelper.readUVInt(in);
		NodeDeclareRef nodeDeclareRef = new NodeDeclareRef(ref);
		nodeDeclareRef.setDefPos(defPos);
		// restore def position if loading metadata without actual decompilation
		ref.setDefPosition(defPos);
		return nodeDeclareRef;
	}
}
