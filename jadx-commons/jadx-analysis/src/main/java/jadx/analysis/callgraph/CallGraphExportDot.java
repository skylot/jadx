package jadx.analysis.callgraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.analysis.callgraph.api.ICallGraphNode;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.utils.DotGraphUtils;
import jadx.core.utils.files.FileUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class CallGraphExportDot {
	private final JadxArgs args;
	private final ICallGraph callGraph;

	public CallGraphExportDot(JadxArgs args, ICallGraph callGraph) {
		this.args = args;
		this.callGraph = callGraph;
	}

	public void writeTo(Path path) {
		try {
			FileUtils.makeDirsForFile(path);
			Files.writeString(path, writeToString(), StandardCharsets.UTF_8,
					WRITE, TRUNCATE_EXISTING, CREATE);
		} catch (IOException e) {
			throw new RuntimeException("Failed to save JSON file: " + path, e);
		}
	}

	public String writeToString() {
		// collect nodes
		Map<Integer, Node> nodeMap = new HashMap<>();
		for (ICallGraphEdge edge : callGraph.edges()) {
			addNode(edge.from(), nodeMap);
			addNode(edge.to(), nodeMap);
		}
		List<Node> nodes = new ArrayList<>(nodeMap.values());
		nodes.sort(Comparator.comparingInt(o -> o.id));

		SimpleCodeWriter cw = new SimpleCodeWriter(args);
		cw.add("digraph CallGraph {");
		for (Node node : nodes) {
			cw.startLine();
			addNodeName(cw, node.id);
			cw.add("[shape=record,label=\"{");
			cw.add(DotGraphUtils.escape(node.method));
			cw.add("}\"];");
		}
		for (ICallGraphEdge edge : callGraph.edges()) {
			cw.startLine();
			addNodeName(cw, edge.from().getId());
			cw.add(" -> ");
			addNodeName(cw, edge.to().getId());
			cw.add(';');
		}
		cw.startLine('}');
		return cw.getCodeStr();
	}

	private void addNodeName(ICodeWriter cw, int id) {
		cw.add('N').add(Integer.toString(id));
	}

	private void addNode(ICallGraphNode cgNode, Map<Integer, Node> nodeMap) {
		nodeMap.computeIfAbsent(cgNode.getId(), id -> {
			Node node = new Node();
			node.id = id;
			node.method = cgNode.getMethodInfo().getRawFullId();
			return node;
		});
	}

	static final class Node {
		int id;
		String method;
	}
}
