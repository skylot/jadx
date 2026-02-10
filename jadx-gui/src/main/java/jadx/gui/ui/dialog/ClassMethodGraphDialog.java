package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DotGraphUtils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

public class ClassMethodGraphDialog extends GraphDialog {

	private static final long serialVersionUID = -850803763322590708L;

	private static final String FONT = "fontname=\"Courier\" fontsize=12";
	private int callerDepthLimit = 10;
	private int nextNodeID = 0;
	private Map<JavaMethod, Integer> methodToNodeID;
	private Set<Edge> edges;
	private List<JavaMethod> javaMethods = Collections.emptyList();
	private ClassNode cls;
	private boolean longNames = false;

	public ClassMethodGraphDialog(MainWindow mainWindow, ClassNode cls) {
		super(mainWindow, String.format("%s: %s", NLS.str("graph_viewer.method_graph.title"), DotGraphUtils.classFormatName(cls, false)));
		this.cls = cls;
	}

	public JMenuBar addMenuBar() {
		JMenuBar menuBar = super.addMenuBar();

		// Long names checkbox
		JCheckBox showLongNames = new JCheckBox(NLS.str("graph_viewer.long_names"));
		showLongNames.setSelected(false);
		showLongNames.addItemListener(e -> {
			longNames = showLongNames.isSelected();
			reload();
		});

		// Assemble menubar panel
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		menuBarPanel.add(showLongNames, BorderLayout.PAGE_START);

		// Add menubar panel to menuBar
		menuBar.add(menuBarPanel);
		return menuBar;
	}

	public static void open(MainWindow window, JClass node) {

		ClassNode cls = node.getCls().getClassNode();
		ClassMethodGraphDialog graphDialog = new ClassMethodGraphDialog(window, cls);
		graphDialog.addMenuBar();

		graphDialog.setVisible(true);
		graphDialog.reload();
	}

	public void reload() {
		SwingUtilities.invokeLater(() -> {
			String graph = generateGraph(cls);
			getPanel().setGraph(graph);
		});
	}

	private String generateGraph(ClassNode classNode) {
		StringBuilder sb = new StringBuilder();

		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Label.foreground");
		Color themeShade = UIManager.getColor("TextArea.background");

		String bgColor =
				String.format("bgcolor=\"#%02x%02x%02x\"", themeBackground.getRed(), themeBackground.getGreen(),
						themeBackground.getBlue());
		String lineColor =
				String.format("color=\"#%02x%02x%02x\"", themeForeground.getRed(), themeForeground.getGreen(), themeForeground.getBlue());
		String fontColor =
				String.format("fontcolor=\"#%02x%02x%02x\"", themeForeground.getRed(), themeForeground.getGreen(),
						themeForeground.getBlue());
		String shadeColor = String.format("fillcolor=\"#%02x%02x%02x\"", themeShade.getRed(), themeShade.getGreen(), themeShade.getBlue());

		try (Formatter f = new Formatter(sb)) {

			// graph header
			f.format("digraph G {\n");
			f.format("%s\n", bgColor);
			f.format("node[shape=\"record\" style=\"filled\" %s %s %s %s]\n", FONT, fontColor, lineColor, shadeColor);
			f.format("edge[arrowtail=\"onormal\" arrowhead=\"onormal\" %s %s %s]\n", FONT, fontColor, lineColor);

			nextNodeID = 0;
			methodToNodeID = new HashMap<>();
			edges = new HashSet<>();

			List<MethodNode> methods = classNode.getMethods();
			javaMethods = methods.stream().map(method -> method.getJavaNode()).collect(Collectors.toList());

			for (JavaMethod javaMethod : javaMethods) {

				addNode(f, javaMethod);

				// add caller relationships
				addCallers(0, f, javaMethod);
			}

			// close graph
			f.format("}");

			return f.toString();
		}
	}

	private void addCallers(int depth, Formatter f, JavaMethod javaMethod) {
		if (depth >= callerDepthLimit) {
			return;
		}

		List<JavaNode> uses = javaMethod.getUseIn();

		// add "calls" relationships
		for (JavaNode node : uses) {
			if (!(node instanceof JavaMethod)) {
				continue;
			}
			JavaMethod caller = (JavaMethod) node;

			// Do not process callers that are not methods from the class
			if (!javaMethods.contains(node)) {
				continue;
			}

			int nodeID = addNode(f, caller);
			addEdge(f, nodeID, methodToNodeID.get(javaMethod));

			addCallers(depth + 1, f, caller);
		}
	}

	// Add a node representing method to the graph in f. Returns the ID of the new node
	private int addNode(Formatter f, JavaMethod method) {
		int nodeID;
		if (methodToNodeID.containsKey(method)) {
			nodeID = methodToNodeID.get(method);
		} else {
			nodeID = nextNodeID;
			nextNodeID++;
			methodToNodeID.put(method, nodeID);
		}

		String name = DotGraphUtils.methodFormatName(method, longNames);
		f.format("Node_%d [ label=\"{%s}\"]\n", nodeID, UiUtils.toDotNodeName(name));

		if (method.callsSelf()) {
			addEdge(f, nodeID, nodeID);
		}

		return nodeID;
	}

	// Add an edge between sourceID and destID to the graph in f
	private void addEdge(Formatter f, int sourceID, int destID) {
		Edge edge = new Edge(sourceID, destID);
		if (!edges.contains(edge)) {
			f.format("Node_%d -> Node_%d\n", sourceID, destID);
			edges.add(edge);
		}
	}

	private static class Edge {
		public int source;
		public int dest;

		public Edge(int source, int dest) {
			this.source = source;
			this.dest = dest;
		}

		@Override
		public boolean equals(Object otherObject) {
			if (!(otherObject instanceof Edge)) {
				return false;
			}
			Edge other = (Edge) otherObject;
			return (this.source == other.source) && (this.dest == other.dest);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, dest);
		}
	}
}
