package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;

public class FieldNodeAdapter implements DataAdapter<FieldNode> {
	private final RootNode root;

	public FieldNodeAdapter(RootNode root) {
		this.root = root;
	}

	@Override
	public void write(DataOutput out, FieldNode value) throws IOException {
		FieldInfo fieldInfo = value.getFieldInfo();
		out.writeUTF(fieldInfo.getDeclClass().getRawName());
		out.writeUTF(fieldInfo.getShortId());
	}

	@Override
	public FieldNode read(DataInput in) throws IOException {
		String cls = in.readUTF();
		String sign = in.readUTF();
		ClassNode clsNode = root.resolveClass(cls);
		if (clsNode == null) {
			throw new RuntimeException("Class not found: " + cls);
		}
		FieldNode fieldNode = clsNode.searchFieldByShortId(sign);
		if (fieldNode == null) {
			throw new RuntimeException("Field not found: " + cls + "." + sign);
		}
		return fieldNode;
	}
}
