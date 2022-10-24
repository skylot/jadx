package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.core.dex.info.MethodInfo;
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
		return root.resolveDirectMethod(cls, sign);
	}
}
