package jadx.analysis.callgraph.api;

import java.util.Collection;

public interface ICallGraphBuilder {

	ICallGraphBuilder includePackages(String pkgFilter);

	ICallGraphBuilder includePackages(Collection<String> pkgFilters);

	ICallGraphBuilder excludePackages(String pkgFilter);

	ICallGraphBuilder excludePackages(Collection<String> pkgFilters);

	ICallGraphBuilder resolvedOnly(boolean resolved);

	ICallGraphBuilder useAlias(boolean useAlias);

	ICallGraph build();
}
