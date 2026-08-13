package jadx.gui.ui.graphs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jadx.core.Consts;
import jadx.core.clsp.ClspClass;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.DotGraphUtils;
import jadx.core.utils.Pair;
import jadx.core.utils.StringUtils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.layout.WrapLayout;

import static jadx.core.utils.DotGraphUtils.classFormatName;
import static jadx.core.utils.DotGraphUtils.formatColor;
import static jadx.core.utils.DotGraphUtils.rawNameFormatName;
import static jadx.core.utils.DotGraphUtils.toDotNodeName;

public class ClassInheritanceGraphDialog extends GraphDialog {
	private static final long serialVersionUID = 938883901412562913L;

	private static final String FONT = "fontname=\"Courier\" fontsize=12";

	private final ClassNode cls;
	private boolean longNames = false;
	private boolean overrides = false;
	private boolean siblings = false;
	private int distanceLimit = 3;

	private Set<String> nodesToAdd;
	private Set<jadx.core.utils.Pair<String>> edgesToAdd;

	private Map<String, Integer> nameToNodeID;
	private int nextNodeID;

	public ClassInheritanceGraphDialog(MainWindow mainWindow, ClassNode cls) {
		super(mainWindow, String.format("%s: %s",
				NLS.str("graph_viewer.inheritance_graph.title"),
				classFormatName(cls, false)));
		this.cls = cls;
	}

	@Override
	public JMenuBar addMenuBar() {
		JMenuBar menuBar = super.addMenuBar();

		// Long names checkbox
		JCheckBox showLongNames = new JCheckBox(NLS.str("graph_viewer.long_names"));
		showLongNames.setSelected(false);
		showLongNames.addItemListener(e -> {
			longNames = showLongNames.isSelected();
			reload();
		});

		// Overrides checkbox
		JCheckBox showOverrides = new JCheckBox(NLS.str("graph_viewer.overrides"));
		showOverrides.setSelected(false);
		showOverrides.addItemListener(e -> {
			overrides = showOverrides.isSelected();
			reload();
		});

		// Siblings checkbox
		JCheckBox showSiblings = new JCheckBox(NLS.str("graph_viewer.inheritance_graph.siblings"));
		showSiblings.setSelected(false);
		showSiblings.addItemListener(e -> {
			siblings = showSiblings.isSelected();
			reload();
		});

		// Distance spinner
		SpinnerNumberModel distanceSpinnerModel = new SpinnerNumberModel(3, 0, 100, 1);
		JSpinner distanceSpinner = new JSpinner(distanceSpinnerModel);
		distanceSpinner.addChangeListener(e -> {
			distanceLimit = (int) distanceSpinner.getValue();
			reload();
		});

		// Distance label
		JLabel distanceLbl = new JLabel(NLS.str("graph_viewer.inheritance_graph.distance"));
		distanceLbl.setLabelFor(distanceSpinner);
		distanceLbl.setHorizontalAlignment(SwingConstants.LEFT);

		// Assemble distance panel
		JPanel distancePanel = new JPanel();
		distancePanel.setOpaque(false);
		distancePanel.setLayout(new BoxLayout(distancePanel, BoxLayout.LINE_AXIS));
		distancePanel.add(distanceSpinner);
		distancePanel.add(Box.createRigidArea(new Dimension(3, 0)));
		distancePanel.add(distanceLbl);

		// Assemble menubar panel
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		menuBarPanel.add(showLongNames, BorderLayout.PAGE_START);
		menuBarPanel.add(showOverrides, BorderLayout.PAGE_START);
		menuBarPanel.add(showSiblings, BorderLayout.PAGE_START);
		menuBarPanel.add(distancePanel, BorderLayout.PAGE_START);

		// Add menubar panel to menuBar
		menuBar.add(menuBarPanel);
		return menuBar;
	}

	public static void open(MainWindow window, JClass node) {
		ClassNode cls = node.getCls().getClassNode();
		ClassInheritanceGraphDialog graphDialog = new ClassInheritanceGraphDialog(window, cls);
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

	private String generateGraph(ClassNode rootClass) {
		// Reset state
		this.nameToNodeID = new HashMap<>();
		this.nextNodeID = 0;
		this.nodesToAdd = new HashSet<>();
		this.edgesToAdd = new HashSet<>();

		// Build the graph
		StringBuilder sb = new StringBuilder();
		try (Formatter f = new Formatter(sb)) {
			// Graph header
			addGraphHeader(f);

			RootNode root = rootClass.root();
			String rootClassName = rootClass.getClassInfo().getType().getObject();
			ClspGraph classGraph = root.getClsp();

			// Collect nodes and edges within distanceLimit into nodesToAdd and edgesToAdd
			visitClass(classGraph, rootClassName, distanceLimit);

			// Add nodes to the graph
			addNodes(f, root, classGraph, rootClassName);

			// Add edges to the graph
			addEdges(f);

			// Close graph
			f.format("}");
			return f.toString();
		}
	}

	/**
	 * Walk graph from rawName to find all nodes and edges within distanceLimit steps.
	 * If siblings is false, only the direct hierarchy will be shown e.g. parents of parents and
	 * children of children.
	 * If siblings is true, the whole graph will be shown e.g. other parents of children and other
	 * children of parents.
	 *
	 * @param graph         a ClspGraph storing parent and children relationships
	 * @param classNode     the current class
	 * @param distanceLimit the distance to display from the current class
	 */
	private void visitClass(ClspGraph graph, String rawName, int distanceLimit) {
		visitClass(graph, rawName, distanceLimit, true, true);
	}

	/**
	 * Walk graph from rawName to find all nodes and edges within distanceLimit steps
	 *
	 * @param graph         a ClspGraph storing parent and children relationships
	 * @param classNode     the current class
	 * @param distanceLimit the distance to display from the current class
	 * @param visitChildren whether to visit children of this class
	 * @param visitParents  whether to visit parents of this class
	 */
	private void visitClass(ClspGraph graph, String rawName, int distanceLimit, boolean visitChildren, boolean visitParents) {
		// Don't process this class again if it has already been visited
		if (nodesToAdd.contains(rawName)) {
			return;
		}

		// Add a graph node for the current class
		nodesToAdd.add(rawName);

		// Stop searching if the distance limit has been reached
		if (distanceLimit <= 0) {
			return;
		}

		// Get the names of children and parents in the ClspGraph
		List<String> children = graph.getChildren(rawName);
		Set<String> parents = graph.getParents(rawName);

		// Visit parents
		if (visitParents) {
			for (String parent : parents) {
				// Don't display the java.lang.Object class
				if (parent == Consts.CLASS_OBJECT) {
					continue;
				}

				Pair<String> edge = new Pair<>(parent, rawName);

				if (edgesToAdd.contains(edge)) {
					// If the egde has already been added, both sides have already been visited and don't need to be
					// visited again
					continue;
				}

				// Add an edge from the parent to the current class
				edgesToAdd.add(edge);

				// Process the parent - adds a node representing the parent
				visitClass(graph, parent, distanceLimit - 1, siblings, true);
			}
		}

		// Visit children
		if (visitChildren) {
			for (String child : children) {
				Pair<String> edge = new Pair<>(rawName, child);

				if (edgesToAdd.contains(edge)) {
					// If the egde has already been added, both sides have already been visited and don't need to be
					// visited again
					continue;
				}

				// Add an edge from the current class to the child
				edgesToAdd.add(edge);

				// Process the child - adds a node representing the child
				visitClass(graph, child, distanceLimit - 1, true, siblings);
			}
		}
	}

	/**
	 * Add the header specifying the format of nodes and edges to the graph
	 *
	 * @param f the string formatter to contain the graph
	 */
	private void addGraphHeader(Formatter f) {
		// Construct colour strings
		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Label.foreground");
		Color themeShade = UIManager.getColor("TextArea.background");

		String bgColor = "bgcolor=" + formatColor(themeBackground);
		String lineColor = "color=" + formatColor(themeForeground);
		String fontColor = "fontcolor=" + formatColor(themeForeground);
		String shadeColor = "fillcolor=" + formatColor(themeShade);

		f.format("digraph G {\n");
		f.format("%s\n", bgColor);
		f.format("node[shape=\"record\" style=\"filled\" %s %s %s %s]\n", FONT, fontColor, lineColor, shadeColor);
		f.format("edge[arrowtail=\"onormal\" arrowhead=\"onormal\" %s %s %s]\n", FONT, fontColor, lineColor);
	}

	/**
	 * Add all edges from edgesToAdd to the graph
	 *
	 * @param f the string formatter to contain the graph
	 */
	private void addEdges(Formatter f) {
		for (Pair<String> edge : edgesToAdd) {

			int firstID;
			int secondID;

			// Get the node ID for the source
			if (nameToNodeID.containsKey(edge.getFirst())) {
				firstID = nameToNodeID.get(edge.getFirst());
			} else {
				// If the source can't be resolved, ignore the edge
				continue;
			}

			// Get the node ID for the destination
			if (nameToNodeID.containsKey(edge.getSecond())) {
				secondID = nameToNodeID.get(edge.getSecond());
			} else {
				// If the destination can't be resolved, ignore the edge
				continue;
			}

			f.format("Node_%d -> Node_%d\n", firstID, secondID);
		}
	}

	/**
	 * Add all nodes from nodesToAdd to the graph
	 *
	 * @param f             the string formatter to contain the graph
	 * @param root          the RootNode used to resolve node names to ClassNodes
	 * @param classGraph    the ClspGraph used to resolve node names to ClspClasses
	 * @param rootClassName the name of the class to highlight in the graph
	 */
	private void addNodes(Formatter f, RootNode root, ClspGraph classGraph, String rootClassName) {
		// Construct colour details
		Color themeHighlight = UIManager.getColor("Component.focusedBorderColor");
		Color themeOutOfFocus = UIManager.getColor("Component.disabledBorderColor");

		String highlightColor = "color=" + formatColor(themeHighlight);
		String outOfFocus = "color=" + formatColor(themeOutOfFocus);

		for (String node : nodesToAdd) {

			// Attempt to resolve full ClassNode information
			ClassNode classNode = root.resolveClass(node);

			if (classNode == null) {
				// If resolving the ClassNode failes, attempt to resolve partial ClspClass information
				ClspClass clspClass = classGraph.getClsDetails(node);

				if (clspClass == null) {
					// Display an out of focus node with no additional information
					addNode(f, node, outOfFocus);
				} else {
					// Display an out of focus node with interface/class information
					addNode(f, clspClass, outOfFocus);
				}
			} else {
				// Highlight the root class
				String extra;
				if (node == rootClassName) {
					extra = highlightColor;
				} else {
					extra = "";
				}

				// Display a node with full information
				addNode(f, classNode, extra);
			}
		}
	}

	/**
	 * Add a node to the graph representing a ClassNode
	 *
	 * @param f     a Formatter to build the graph into
	 * @param cls   the class to add a node for
	 * @param extra extra formatting to append to the end of the current class's
	 *              node in the graph e.g. a highlight colour
	 * @return the node id of the node created
	 */
	private int addNode(Formatter f, ClassNode cls, String extra) {
		String rawName = cls.getClassInfo().getType().getObject();
		if (nameToNodeID.containsKey(rawName)) {
			return nameToNodeID.get(rawName);
		}

		int nodeID = nextNodeID++;
		nameToNodeID.put(rawName, nodeID);

		// Make dashed if the class is an interface
		if (cls.getAccessFlags().isInterface()) {
			extra += " style=\"dashed, filled\"";
		}

		// Get the name to display the class as
		String name = classFormatName(cls, longNames);

		// Add the start of the graph node with the class name
		f.format("Node_%d [ label=\"{%s\\ ", nodeID, toDotNodeName(name));

		// Add any override information
		if (overrides) {
			// Start a new section
			f.format("|");

			// Construct a table of method name to method details
			List<Pair<String>> table = new ArrayList<>();
			for (MethodNode method : cls.getMethods()) {
				// Parse each overridden method
				MethodOverrideAttr ovrdAttr = method.get(AType.METHOD_OVERRIDE);
				if (ovrdAttr != null) {
					if (!ovrdAttr.getOverrideList().isEmpty()) {
						// Get the method name
						String methodName = DotGraphUtils.methodFormatName(method, longNames);
						// Start the details string
						Formatter details = new Formatter();
						details.format(" overrides ");
						// Mark each base class that this method overrides from
						for (IMethodDetails baseMthDetails : ovrdAttr.getOverrideList()) {
							String baseClassName = classFormatName(baseMthDetails.getMethodInfo().getDeclClass(),
									longNames);
							details.format("%s, ", baseClassName);
						}
						// Remove any trailing commas
						String detailsString = StringUtils.removeSuffix(details.toString(), ", ");
						// Add the method name and details to the table
						table.add(new Pair<>(methodName, detailsString));
						details.close();
					}
				}
			}

			// Format the table
			if (!table.isEmpty()) {
				int longestLength = table.stream().map(Pair::getFirst).map(String::length).max((a, b) -> a - b).get();
				for (Pair<String> entry : table) {
					f.format("%-" + longestLength + "s %s\\l", entry.getFirst(), entry.getSecond());
				}
			} else {
				f.format("No overrides.");
			}
		}

		// Close the graph node
		f.format("}\" %s]\n", extra);
		return nodeID;
	}

	/**
	 * Add a node to the graph representing a raw name that could be resolved to a ClspClass but not a
	 * ClassNode
	 *
	 * @param f     a Formatter to build the graph into
	 * @param cls   the class to add a node for
	 * @param extra extra formatting to append to the end of the current class's
	 *              node in the graph e.g. a highlight colour
	 * @return the node id of the node created
	 */
	private int addNode(Formatter f, ClspClass cls, String extra) {
		String rawName = cls.getName();
		if (nameToNodeID.containsKey(rawName)) {
			return nameToNodeID.get(rawName);
		}

		int nodeID = nextNodeID++;
		nameToNodeID.put(rawName, nodeID);

		// Make dashed if the class is an interface
		if (cls.isInterface()) {
			extra += " style=\"dashed, filled\"";
		}

		// Get the name to display the class as
		String name = rawNameFormatName(rawName, this.cls.root(), longNames);

		// Add the start of the graph node with the class name
		f.format("Node_%d [ label=\"{%s\\ ", nodeID, toDotNodeName(name));

		// TODO: can we retrieve overrides for classes that resolve to a ClspClass but not a ClassNode?

		// Close the graph node
		f.format("}\" %s]\n", extra);
		return nodeID;
	}

	/**
	 * Add a node to the graph representing a raw name that couldn't be resolved to
	 * a ClassNode
	 *
	 * @param f       a Formatter to build the graph into
	 * @param rawName the class to add a node for
	 * @param extra   extra formatting to append to the end of the current class's
	 *                node in the graph e.g. a highlight colour
	 * @return the node id of the node created
	 */
	private int addNode(Formatter f, String rawName, String extra) {
		if (nameToNodeID.containsKey(rawName)) {
			return nameToNodeID.get(rawName);
		}

		int nodeID = nextNodeID++;
		nameToNodeID.put(rawName, nodeID);

		// Construct name details
		String name = DotGraphUtils.rawNameFormatName(rawName, cls.root(), longNames);

		// Add the node with name and colour details
		f.format("Node_%d [ label=\"{%s}\" %s]\n", nodeID, toDotNodeName(name), extra);
		return nodeID;
	}
}
