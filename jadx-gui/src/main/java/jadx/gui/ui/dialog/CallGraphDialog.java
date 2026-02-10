package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.core.utils.DotGraphUtils;
import jadx.gui.treemodel.JMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

public class CallGraphDialog extends GraphDialog {

	private static final long serialVersionUID = -850803763322590708L;

	private static final Logger LOG = LoggerFactory.getLogger(CallGraphDialog.class);

	private static final String FONT = "fontname=\"Courier\" fontsize=12";
	private int callerDepthLimit = 3;
	private int calleeDepthLimit = 3;
	private int nextNodeID;
	private Map<JavaMethod, Integer> methodToNodeID;
	private Map<IMethodRef, Integer> unresolvedMethodToNodeID;
	private Set<Edge> edges;
	private JavaMethod javaMethod;
	private boolean longNames = false;

	public CallGraphDialog(MainWindow mainWindow, JavaMethod javaMethod) {
		super(mainWindow,
				String.format("%s: %s", NLS.str("graph_viewer.call_graph.title"), DotGraphUtils.methodFormatName(javaMethod, false)));
		this.javaMethod = javaMethod;
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

		// Calee spinner
		SpinnerNumberModel calleeDepthSpinnerModel = new SpinnerNumberModel(3, 0, 100, 1);
		JSpinner calleeDepthSpinner = new JSpinner(calleeDepthSpinnerModel);
		calleeDepthSpinner.addChangeListener(e -> {
			calleeDepthLimit = (int) calleeDepthSpinner.getValue();
			reload();
		});

		// Callee label
		JLabel calleeLbl = new JLabel(NLS.str("graph_viewer.callee_depth"));
		calleeLbl.setLabelFor(calleeDepthSpinner);
		calleeLbl.setHorizontalAlignment(SwingConstants.LEFT);

		// Assemble callee panel
		JPanel calleePanel = new JPanel();
		calleePanel.setOpaque(false);
		calleePanel.setLayout(new BoxLayout(calleePanel, BoxLayout.LINE_AXIS));
		calleePanel.add(calleeLbl);
		calleePanel.add(Box.createRigidArea(new Dimension(3, 0)));
		calleePanel.add(calleeDepthSpinner);

		// Caller spinner
		SpinnerNumberModel callerDepthSpinnerModel = new SpinnerNumberModel(3, 0, 100, 1);
		JSpinner callerDepthSpinner = new JSpinner(callerDepthSpinnerModel);
		callerDepthSpinner.addChangeListener(e -> {
			callerDepthLimit = (int) callerDepthSpinner.getValue();
			reload();
		});

		// Caller label
		JLabel callerLbl = new JLabel(NLS.str("graph_viewer.caller_depth"));
		callerLbl.setLabelFor(callerDepthSpinner);
		callerLbl.setHorizontalAlignment(SwingConstants.LEFT);

		// Assemble caller panel
		JPanel callerPanel = new JPanel();
		callerPanel.setOpaque(false);
		callerPanel.setLayout(new BoxLayout(callerPanel, BoxLayout.LINE_AXIS));
		callerPanel.add(callerLbl);
		callerPanel.add(Box.createRigidArea(new Dimension(3, 0)));
		callerPanel.add(callerDepthSpinner);

		// Assemble menubar panel
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		menuBarPanel.add(showLongNames, BorderLayout.PAGE_START);
		menuBarPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		menuBarPanel.add(calleePanel);
		menuBarPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		menuBarPanel.add(callerPanel);

		// Add menubar panel to menuBar
		menuBar.add(menuBarPanel);
		return menuBar;
	}

	public static void open(MainWindow window, JMethod method) {

		JavaMethod javaMethod = method.getJavaMethod();
		CallGraphDialog graphDialog = new CallGraphDialog(window, javaMethod);
		graphDialog.addMenuBar();

		graphDialog.setVisible(true);
		graphDialog.reload();
	}

	public void reload() {
		SwingUtilities.invokeLater(() -> {
			String graph = generateGraph(javaMethod);
			getPanel().setGraph(graph);

		});
	}

	private String generateGraph(JavaMethod javaMethod) {
		StringBuilder sb = new StringBuilder();

		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Label.foreground");
		Color themeHighlight = UIManager.getColor("Component.focusedBorderColor");
		Color themeShade = UIManager.getColor("TextArea.background");

		String bgColor =
				String.format("bgcolor=\"#%02x%02x%02x\"", themeBackground.getRed(), themeBackground.getGreen(),
						themeBackground.getBlue());
		String lineColor =
				String.format("color=\"#%02x%02x%02x\"", themeForeground.getRed(), themeForeground.getGreen(), themeForeground.getBlue());
		String fontColor =
				String.format("fontcolor=\"#%02x%02x%02x\"", themeForeground.getRed(), themeForeground.getGreen(),
						themeForeground.getBlue());
		String highlightColor =
				String.format("color=\"#%02x%02x%02x\"", themeHighlight.getRed(), themeHighlight.getGreen(),
						themeHighlight.getBlue());
		String shadeColor = String.format("fillcolor=\"#%02x%02x%02x\"", themeShade.getRed(), themeShade.getGreen(), themeShade.getBlue());

		try (Formatter f = new Formatter(sb)) {

			// graph header
			f.format("digraph G {\n");
			f.format("%s\n", bgColor);
			f.format("node[shape=\"record\" style=\"filled\" %s %s %s %s]\n", FONT, fontColor, lineColor, shadeColor);
			f.format("edge[arrowtail=\"onormal\" arrowhead=\"onormal\" %s %s %s]\n", FONT, fontColor, lineColor);

			nextNodeID = 0;
			methodToNodeID = new HashMap<>();
			unresolvedMethodToNodeID = new HashMap<>();
			edges = new HashSet<>();

			addNode(f, javaMethod, highlightColor);

			// add caller relationships
			addCallers(0, f, javaMethod);

			// add calee relationships
			addCallees(0, f, javaMethod);

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

			int nodeID = addNode(f, caller);
			addEdge(f, nodeID, methodToNodeID.get(javaMethod));

			addCallers(depth + 1, f, caller);
		}
	}

	private void addCallees(int depth, Formatter f, JavaMethod javaMethod) {
		if (depth >= calleeDepthLimit) {
			return;
		}

		List<JavaNode> used = javaMethod.getUsed();

		// add "calls" relationships
		for (JavaNode node : used) {
			if (!(node instanceof JavaMethod)) {
				continue;
			}
			JavaMethod callee = (JavaMethod) node;

			int nodeID = addNode(f, callee);
			addEdge(f, methodToNodeID.get(javaMethod), nodeID);

			addCallees(depth + 1, f, callee);
		}
		addUnresolvedCallees(depth, f, javaMethod);
	}

	private void addUnresolvedCallees(int depth, Formatter f, JavaMethod javaMethod) {
		if (depth >= calleeDepthLimit) {
			return;
		}

		List<IMethodRef> used = javaMethod.getUnresolvedUsed();

		// add "calls" relationships
		for (IMethodRef callee : used) {
			String name = callee.getName();
			if (name == null) {
				continue;
			}

			int nodeID = addNode(f, callee);
			addEdge(f, methodToNodeID.get(javaMethod), nodeID);
		}
	}

	private int addNode(Formatter f, JavaMethod method) {
		return addNode(f, method, "");
	}

	// Add a node representing method to the graph in f. Returns the ID of the new node
	private int addNode(Formatter f, JavaMethod method, String extra) {
		int nodeID;
		if (methodToNodeID.containsKey(method)) {
			nodeID = methodToNodeID.get(method);
		} else {
			nodeID = nextNodeID;
			nextNodeID++;
			methodToNodeID.put(method, nodeID);
		}

		String name = DotGraphUtils.methodFormatName(method, longNames);
		f.format("Node_%d [ label=\"{%s}\" %s]\n", nodeID, UiUtils.toDotNodeName(name), extra);

		if (javaMethod.callsSelf()) {
			addEdge(f, nodeID, nodeID);
		}

		return nodeID;
	}

	private int addNode(Formatter f, IMethodRef method) {
		return addNode(f, method, "");
	}

	// Add a node representing an unresolved method to the graph in f. Returns the ID of the new node
	private int addNode(Formatter f, IMethodRef method, String extra) {
		int nodeID;
		if (unresolvedMethodToNodeID.containsKey(method)) {
			nodeID = unresolvedMethodToNodeID.get(method);
		} else {
			nodeID = nextNodeID;
			nextNodeID++;
			unresolvedMethodToNodeID.put(method, nodeID);
		}

		String name = DotGraphUtils.unresolvedMethodFormatName(method, longNames);

		Color themeOutOfFocus = UIManager.getColor("Component.disabledBorderColor");
		String outOfFocus =
				String.format("color=\"#%02x%02x%02x\"", themeOutOfFocus.getRed(), themeOutOfFocus.getGreen(),
						themeOutOfFocus.getBlue());

		f.format("Node_%d [ label=\"{%s}\" style=dashed %s %s]\n", nodeID, UiUtils.toDotNodeName(name), outOfFocus, extra);
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
