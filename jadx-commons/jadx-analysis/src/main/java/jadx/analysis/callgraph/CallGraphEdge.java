package jadx.analysis.callgraph;

import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.analysis.callgraph.api.ICallGraphNode;
import jadx.core.dex.attributes.IAttributeNode;

class CallGraphEdge implements ICallGraphEdge {
	private final ICallGraphNode from;
	private final ICallGraphNode to;
	private final CallGraphAttrNode attrNode = new CallGraphAttrNode();

	public CallGraphEdge(ICallGraphNode from, ICallGraphNode to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public ICallGraphNode from() {
		return from;
	}

	@Override
	public ICallGraphNode to() {
		return to;
	}

	@Override
	public boolean isResolved() {
		return to.isResolved();
	}

	@Override
	public IAttributeNode attributes() {
		return attrNode;
	}

	@Override
	public String toString() {
		return "CallGraphEdge{from=" + from + ", to=" + to + '}';
	}
}
