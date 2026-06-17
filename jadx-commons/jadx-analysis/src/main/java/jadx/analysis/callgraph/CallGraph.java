package jadx.analysis.callgraph;

import java.nio.file.Path;
import java.util.List;

import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.api.JadxArgs;

class CallGraph implements ICallGraph {

	private final JadxArgs args;
	private final List<ICallGraphEdge> edges;

	public CallGraph(JadxArgs args, List<ICallGraphEdge> edges) {
		this.args = args;
		this.edges = edges;
	}

	@Override
	public List<ICallGraphEdge> edges() {
		return edges;
	}

	@Override
	public void writeDot(Path path) {
		new CallGraphExportDot(args, this).writeTo(path);
	}

	@Override
	public void writeJson(Path path) {
		new CallGraphExportJson(this).writeTo(path);
	}
}
