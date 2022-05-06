package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class MethodNodeAdapter implements DataAdapter<MethodNode> {
	private final RootNode root;

	public MethodNodeAdapter(RootNode root) {
		this.root = root;
	}

	@Override
	public void write(DataOutput out, MethodNode value) throws IOException {
		MethodInfo methodInfo = value.getMethodInfo();
		out.writeUTF(methodInfo.getDeclClass().getRawName());
		out.writeUTF(methodInfo.getShortId());
	}

	@Override
	public MethodNode read(DataInput in) throws IOException {
		String cls = in.readUTF();
		String sign = in.readUTF();
		ClassNode clsNode = root.resolveClass(cls);
		if (clsNode == null) {
			throw new RuntimeException("Class not found: " + cls);
		}
		MethodNode methodNode = clsNode.searchMethodByShortId(sign);
		if (methodNode == null) {
			throw new RuntimeException("Method not found: " + cls + "." + sign);
		}
		return methodNode;
	}
}
