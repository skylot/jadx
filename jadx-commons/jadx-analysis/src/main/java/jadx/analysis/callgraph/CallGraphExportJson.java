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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.analysis.callgraph.api.ICallGraphNode;
import jadx.core.utils.files.FileUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class CallGraphExportJson {
	private final ICallGraph callGraph;
	private final Gson gson;

	public CallGraphExportJson(ICallGraph callGraph) {
		this.callGraph = callGraph;
		this.gson = new GsonBuilder()
				.disableJdkUnsafe()
				.disableInnerClassSerialization()
				.setStrictness(Strictness.STRICT)
				// .setPrettyPrinting() // TODO: add option for pretty print?
				.create();
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
		List<Edge> edges = new ArrayList<>();
		Map<Integer, Node> nodeMap = new HashMap<>();
		for (ICallGraphEdge edge : callGraph.edges()) {
			ICallGraphNode from = edge.from();
			ICallGraphNode to = edge.to();
			addNode(from, nodeMap);
			addNode(to, nodeMap);
			Edge jsonEdge = new Edge();
			jsonEdge.from = from.getId();
			jsonEdge.to = to.getId();
			jsonEdge.resolved = edge.isResolved();
			edges.add(jsonEdge);
		}
		List<Node> nodes = new ArrayList<>(nodeMap.values());
		nodes.sort(Comparator.comparingInt(o -> o.id));

		RootNode rootNode = new RootNode();
		rootNode.nodes = nodes;
		rootNode.edges = edges;
		return gson.toJson(rootNode);
	}

	private void addNode(ICallGraphNode cgNode, Map<Integer, Node> nodeMap) {
		nodeMap.computeIfAbsent(cgNode.getId(), id -> {
			Node node = new Node();
			node.id = id;
			node.method = cgNode.getMethodInfo().getRawFullId();
			return node;
		});
	}

	static final class RootNode {
		List<Node> nodes;
		List<Edge> edges;
	}

	static final class Node {
		int id;
		String method;
	}

	static final class Edge {
		int from;
		int to;
		boolean resolved;
	}
}
