package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.NodeEnd;

public class NodeEndAdapter implements DataAdapter<NodeEnd> {

	@Override
	public void write(DataOutput out, NodeEnd value) throws IOException {
	}

	@Override
	public NodeEnd read(DataInput in) throws IOException {
		return NodeEnd.VALUE;
	}
}
