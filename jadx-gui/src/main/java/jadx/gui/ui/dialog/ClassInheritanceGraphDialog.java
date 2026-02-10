package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.apksig.internal.util.Pair;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DotGraphUtils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

public class ClassInheritanceGraphDialog extends GraphDialog {

	private static final long serialVersionUID = 938883901412562913L;

	private static final Logger LOG = LoggerFactory.getLogger(ClassInheritanceGraphDialog.class);

	private static final String FONT = "fontname=\"Courier\" fontsize=12";
	private ClassNode cls;
	private boolean longNames = false;
	private boolean overrides = false;

	private Map<Object, Integer> objectToNodeID = new HashMap<>();
	private int nextNodeID = 0;

	public ClassInheritanceGraphDialog(MainWindow mainWindow, ClassNode cls) {
		super(mainWindow,
				String.format("%s: %s", NLS.str("graph_viewer.inheritance_graph.title"), DotGraphUtils.classFormatName(cls, false)));
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

		// Overrides checkbox
		JCheckBox showOverrides = new JCheckBox(NLS.str("graph_viewer.overrides"));
		showOverrides.setSelected(false);
		showOverrides.addItemListener(e -> {
			overrides = showOverrides.isSelected();
			reload();
		});

		// Assemble menubar panel
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		menuBarPanel.add(showLongNames, BorderLayout.PAGE_START);
		menuBarPanel.add(showOverrides, BorderLayout.PAGE_START);

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
		StringBuilder sb = new StringBuilder();

		ClassNode cls = rootClass;

		objectToNodeID = new HashMap<>();

		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Label.foreground");
		Color themeHighlight = UIManager.getColor("Component.focusedBorderColor");
		Color themeShade = UIManager.getColor("TextArea.background");

		String bgColor =
				String.format("bgcolor=\"#%02x%02x%02x\"", themeBackground.getRed(), themeBackground.getGreen(), themeBackground.getBlue());
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

			// add nodes
			processClass(f, cls, highlightColor);

			// close graph
			f.format("}");

			return f.toString();
		}
	}

	private int processClass(Formatter f, ClassNode cls) {
		return processClass(f, cls, "");
	}

	private int processClass(Formatter f, ClassNode cls, String extra) {
		if (objectToNodeID.containsKey(cls)) {
			// Don't process a class that has been processed before
			return objectToNodeID.get(cls);
		}
		int classID = addNode(f, cls, extra);

		// add interface relationships
		List<ArgType> ifaces = cls.getInterfaces();
		for (int i = 0; i < ifaces.size(); i++) {
			ArgType iface = ifaces.get(i);

			int ifaceID;
			ClassNode ifaceNode = cls.root().resolveClass(iface);
			if (ifaceNode != null) {
				ifaceID = processClass(f, ifaceNode);
				objectToNodeID.put(iface, ifaceID);
			} else {
				ifaceID = addNode(f, iface);
			}
			// Classes implement interfaces, interfaces extend interfaces
			String edgeLabel = cls.getAccessFlags().isInterface() ? "extends" : "implements";
			f.format("Node_%d -> Node_%d [label=\"%s\" style=\"dashed\" ]\n", classID, ifaceID, edgeLabel);
		}

		// add superclass relationship
		ArgType superClass = cls.getSuperClass();

		if (superClass != ArgType.OBJECT) {
			int superClsID;
			cls = cls.root().resolveClass(superClass);
			if (cls != null) {
				superClsID = processClass(f, cls);
				objectToNodeID.put(superClass, superClsID);
			} else {
				superClsID = addNode(f, superClass);
			}

			f.format("Node_%d -> Node_%d [label=\"extends\" ]\n", classID, superClsID);
		}
		return classID;
	}

	// Add a node for a class
	private int addNode(Formatter f, ClassNode cls) {
		return addNode(f, cls, "");
	}

	private int addNode(Formatter f, ClassNode cls, String extra) {
		int nodeID;
		if (objectToNodeID.containsKey(cls)) {
			nodeID = objectToNodeID.get(cls);
		} else {
			nodeID = nextNodeID;
			nextNodeID++;
			objectToNodeID.put(cls, nodeID);
		}

		if (cls.getAccessFlags().isInterface()) {
			extra += " style=\"dashed, filled\"";
		}

		String name = DotGraphUtils.classFormatName(cls, longNames);
		f.format("Node_%d [ label=\"{%s\\ ", nodeID, UiUtils.toDotNodeName(name));

		if (overrides) {
			f.format("|");
			List<Pair<String, String>> table = new ArrayList<>();
			for (MethodNode method : cls.getMethods()) {
				MethodOverrideAttr ovrdAttr = method.get(AType.METHOD_OVERRIDE);
				if (ovrdAttr != null) {
					if (!ovrdAttr.getOverrideList().isEmpty()) {
						String methodName = DotGraphUtils.methodFormatName(method, longNames);
						Formatter details = new Formatter();
						details.format(" overrides ");
						for (IMethodDetails baseMthDetails : ovrdAttr.getOverrideList()) {
							String baseClassName = DotGraphUtils.classFormatName(baseMthDetails.getMethodInfo().getDeclClass(), longNames);
							details.format("%s, ", baseClassName);
						}

						String detailsString = details.toString();

						// Remove trailing ', '
						detailsString = detailsString.substring(0, detailsString.length() - 2);

						table.add(Pair.of(methodName, detailsString));
						details.close();
					}
				}
			}

			if (!table.isEmpty()) {
				int longestLength = table.stream().map(Pair::getFirst).map(String::length).max((a, b) -> a - b).get();
				for (Pair<String, String> entry : table) {
					f.format("%-" + longestLength + "s %s\\l", entry.getFirst(), entry.getSecond());
				}

			} else {
				f.format("No overrides.");
			}
		}
		f.format("}\" %s]\n", extra);

		return nodeID;
	}

	// Add a node for an unresolved argtype
	private int addNode(Formatter f, ArgType argType) {
		return addNode(f, argType, "");
	}

	private int addNode(Formatter f, ArgType argType, String extra) {
		int nodeID;
		if (objectToNodeID.containsKey(argType)) {
			nodeID = objectToNodeID.get(argType);
		} else {
			nodeID = nextNodeID;
			nextNodeID++;
			objectToNodeID.put(argType, nodeID);
		}

		Color themeOutOfFocus = UIManager.getColor("Component.disabledBorderColor");
		String outOfFocus =
				String.format("color=\"#%02x%02x%02x\"", themeOutOfFocus.getRed(), themeOutOfFocus.getGreen(), themeOutOfFocus.getBlue());

		String name = DotGraphUtils.interfaceFormatName(argType, cls, longNames);
		f.format("Node_%d [ label=\"{%s}\" %s %s]\n", nodeID, UiUtils.toDotNodeName(name), outOfFocus, extra);

		return nodeID;
	}

}
