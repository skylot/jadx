package jadx.analysis.callgraph;

import java.nio.file.Path;
import java.util.List;

import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.analysis.callgraph.api.ICallGraphNode;
import jadx.api.JadxArgs;
import jadx.core.dex.info.MethodInfo;

class CallGraph implements ICallGraph {

	private final JadxArgs args;
	private final List<ICallGraphEdge> edges;
	private final boolean useAlias;

	public CallGraph(JadxArgs args, List<ICallGraphEdge> edges) {
		this(args, edges, false);
	}

	public CallGraph(JadxArgs args, List<ICallGraphEdge> edges, boolean useAlias) {
		this.args = args;
		this.edges = edges;
		this.useAlias = useAlias;
	}

	@Override
	public List<ICallGraphEdge> edges() {
		return edges;
	}

	@Override
	public boolean isUseAlias() {
		return useAlias;
	}

	@Override
	public void writeDot(Path path) {
		new CallGraphExportDot(args, this, useAlias).writeTo(path);
	}

	@Override
	public void writeJson(Path path) {
		new CallGraphExportJson(this, useAlias).writeTo(path);
	}

	static String formatMethodId(ICallGraphNode cgNode, boolean useAlias) {
		MethodInfo mthInfo = cgNode.getMethodInfo();
		if (!useAlias) {
			return mthInfo.getRawFullId();
		}
		return mthInfo.getDeclClass().getAliasFullName() + '.' + mthInfo.makeSignature(true, true);
	}
}
