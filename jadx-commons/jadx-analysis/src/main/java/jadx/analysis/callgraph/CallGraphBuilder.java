package jadx.analysis.callgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private @Nullable String pkgFilter;

	public CallGraphBuilder(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	@Override
	public ICallGraphBuilder resolvedOnly(boolean resolved) {
		this.resolvedOnly = resolved;
		return this;
	}

	@Override
	public ICallGraphBuilder includePackages(String pkgFilter) {
		this.pkgFilter = pkgFilter.endsWith(".") ? pkgFilter : pkgFilter + '.';
		return this;
	}

	@Override
	public ICallGraph build() {
		return new CallGraph(decompiler.getArgs(), collectEdges());
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
		if (pkgFilter == null) {
			return false;
		}
		return !cls.getFullName().startsWith(pkgFilter);
	}

	private static CallGraphNode getCallGraphNode(MethodNode mth, Map<MethodInfo, CallGraphNode> nodes, AtomicInteger nodeId) {
		return nodes.computeIfAbsent(mth.getMethodInfo(), i -> new CallGraphNode(nodeId.incrementAndGet(), mth));
	}

	private static CallGraphNode getCallGraphNode(MethodInfo mth, Map<MethodInfo, CallGraphNode> nodes, AtomicInteger nodeId) {
		return nodes.computeIfAbsent(mth, i -> new CallGraphNode(nodeId.incrementAndGet(), mth));
	}
}
