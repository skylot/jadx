package jadx.analysis.callgraph;

import jadx.analysis.callgraph.api.ICallGraphBuilder;
import jadx.api.JadxDecompiler;

public class JadxCallGraph {

	public static ICallGraphBuilder builder(JadxDecompiler decompiler) {
		return new CallGraphBuilder(decompiler);
	}
}
