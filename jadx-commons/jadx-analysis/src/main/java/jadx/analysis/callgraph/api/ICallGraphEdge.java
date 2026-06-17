package jadx.analysis.callgraph.api;

import jadx.core.dex.attributes.IAttributeNode;

public interface ICallGraphEdge {

	ICallGraphNode from();

	ICallGraphNode to();

	boolean isResolved();

	IAttributeNode attributes();
}
