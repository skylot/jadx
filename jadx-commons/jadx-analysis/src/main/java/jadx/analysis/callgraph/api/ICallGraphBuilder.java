package jadx.analysis.callgraph.api;

public interface ICallGraphBuilder {

	ICallGraphBuilder includePackages(String pkgFilter);

	ICallGraphBuilder resolvedOnly(boolean resolved);

	ICallGraph build();
}
