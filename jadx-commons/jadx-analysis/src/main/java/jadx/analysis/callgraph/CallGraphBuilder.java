package jadx.analysis.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphBuilder;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.api.JadxDecompiler;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;

class CallGraphBuilder implements ICallGraphBuilder {
	private final JadxDecompiler decompiler;
	private boolean resolvedOnly = false;
	private boolean useAlias = false;
	private final Set<String> includePackages = new LinkedHashSet<>();
	private final Set<String> excludePackages = new LinkedHashSet<>();

	public CallGraphBuilder(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	@Override
	public ICallGraphBuilder resolvedOnly(boolean resolved) {
		this.resolvedOnly = resolved;
		return this;
	}

	@Override
	public ICallGraphBuilder useAlias(boolean useAlias) {
		this.useAlias = useAlias;
		return this;
	}

	@Override
	public ICallGraphBuilder includePackages(String pkgFilter) {
		addPkgPrefix(includePackages, pkgFilter);
		return this;
	}

	@Override
	public ICallGraphBuilder includePackages(Collection<String> pkgFilters) {
		if (pkgFilters != null) {
			for (String pkgFilter : pkgFilters) {
				addPkgPrefix(includePackages, pkgFilter);
			}
		}
		return this;
	}

	@Override
	public ICallGraphBuilder excludePackages(String pkgFilter) {
		addPkgPrefix(excludePackages, pkgFilter);
		return this;
	}

	@Override
	public ICallGraphBuilder excludePackages(Collection<String> pkgFilters) {
		if (pkgFilters != null) {
			for (String pkgFilter : pkgFilters) {
				addPkgPrefix(excludePackages, pkgFilter);
			}
		}
		return this;
	}

	@Override
	public ICallGraph build() {
		return new CallGraph(decompiler.getArgs(), collectEdges(), useAlias);
	}

	private List<ICallGraphEdge> collectEdges() {
		AtomicInteger nodeId = new AtomicInteger();
		Map<MethodInfo, CallGraphNode> nodes = new HashMap<>();
		List<ICallGraphEdge> edges = new ArrayList<>();

		for (ClassNode cls : decompiler.getRoot().getClasses(true)) {
			if (ignorePkg(cls.getClassInfo())) {
				continue;
			}
			for (MethodNode mth : cls.getMethods()) {
				CallGraphNode thisNode = getCallGraphNode(mth, nodes, nodeId);
				for (MethodNode use : mth.getUseIn()) {
					if (ignorePkg(use.getDeclaringClass().getClassInfo())) {
						continue;
					}
					CallGraphNode useInNode = getCallGraphNode(use, nodes, nodeId);
					edges.add(new CallGraphEdge(useInNode, thisNode));
				}
				if (!resolvedOnly) {
					for (MethodInfo used : mth.getUnresolvedUsed()) {
						if (ignorePkg(used.getDeclClass())) {
							continue;
						}
						CallGraphNode usedNode = getCallGraphNode(used, nodes, nodeId);
						edges.add(new CallGraphEdge(thisNode, usedNode));
					}
				}
			}
		}
		return edges;
	}

	private boolean ignorePkg(ClassInfo cls) {
		String fullName = cls.getFullName();
		if (!includePackages.isEmpty() && !matchesAny(fullName, includePackages)) {
			return true;
		}
		if (!excludePackages.isEmpty() && matchesAny(fullName, excludePackages)) {
			return true;
		}
		return false;
	}

	private static boolean matchesAny(String fullName, Set<String> prefixes) {
		for (String prefix : prefixes) {
			if (fullName.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static void addPkgPrefix(Set<String> target, @Nullable String pkgFilter) {
		String normalized = normalizePkgPrefix(pkgFilter);
		if (normalized != null) {
			target.add(normalized);
		}
	}

	private static @Nullable String normalizePkgPrefix(@Nullable String pkgFilter) {
		if (pkgFilter == null) {
			return null;
		}
		String trimmed = pkgFilter.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.endsWith(".*")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.endsWith(".") ? trimmed : trimmed + '.';
	}

	private static CallGraphNode getCallGraphNode(MethodNode mth, Map<MethodInfo, CallGraphNode> nodes, AtomicInteger nodeId) {
		return nodes.computeIfAbsent(mth.getMethodInfo(), i -> new CallGraphNode(nodeId.incrementAndGet(), mth));
	}

	private static CallGraphNode getCallGraphNode(MethodInfo mth, Map<MethodInfo, CallGraphNode> nodes, AtomicInteger nodeId) {
		return nodes.computeIfAbsent(mth, i -> new CallGraphNode(nodeId.incrementAndGet(), mth));
	}
}
