package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

public class ClassNodeAdapter implements DataAdapter<ClassNode> {
	private final RootNode root;

	public ClassNodeAdapter(RootNode root) {
		this.root = root;
	}

	@Override
	public void write(DataOutput out, ClassNode value) throws IOException {
		out.writeUTF(value.getClassInfo().getRawName());
	}

	@Override
	public ClassNode read(DataInput in) throws IOException {
		return root.resolveClass(in.readUTF());
	}
}
