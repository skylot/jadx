package jadx.analysis.callgraph;

import org.jetbrains.annotations.Nullable;

import jadx.analysis.callgraph.api.ICallGraphNode;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.MethodNode;

class CallGraphNode implements ICallGraphNode {
	private final int id;
	private final MethodInfo mthInfo;
	private final @Nullable MethodNode mthNode;
	private final CallGraphAttrNode attrNode;

	public CallGraphNode(int id, MethodInfo mthInfo) {
		this(id, mthInfo, null);
	}

	public CallGraphNode(int id, MethodNode mthNode) {
		this(id, mthNode.getMethodInfo(), mthNode);
	}

	public CallGraphNode(int id, MethodInfo mthInfo, @Nullable MethodNode mthNode) {
		this.id = id;
		this.mthInfo = mthInfo;
		this.mthNode = mthNode;
		this.attrNode = new CallGraphAttrNode();
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	@Override
	public @Nullable MethodNode getMethodNode() {
		return mthNode;
	}

	@Override
	public boolean isResolved() {
		return mthNode != null;
	}

	@Override
	public IAttributeNode attributes() {
		return attrNode;
	}

	@Override
	public String toString() {
		return mthInfo.getFullId();
	}
}
