package jadx.analysis.callgraph.api;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.MethodNode;

public interface ICallGraphNode {

	int getId();

	MethodInfo getMethodInfo();

	@Nullable
	MethodNode getMethodNode();

	boolean isResolved();

	IAttributeNode attributes();
}
