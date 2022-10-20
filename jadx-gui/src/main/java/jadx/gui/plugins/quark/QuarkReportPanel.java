package jadx.gui.plugins.quark;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.core.utils.Utils;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.ui.NodeLabel;

public class QuarkReportPanel extends ContentPanel {
	private static final long serialVersionUID = -242266836695889206L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkReportPanel.class);

	private final QuarkReportData data;
	private final JNodeCache nodeCache;

	private JEditorPane header;
	private JTree tree;
	private DefaultMutableTreeNode treeRoot;
	private Font font;
	private Font boldFont;
	private CachingTreeCellRenderer cellRenderer;

	protected QuarkReportPanel(TabbedPane panel, QuarkReportNode node, QuarkReportData data) {
		super(panel, node);
		this.data = data;
		this.nodeCache = panel.getMainWindow().getCacheObject().getNodeCache();
		prepareData();
		initUI();
		loadSettings();
	}

	private void prepareData() {
		data.crimes.sort(Comparator.comparingInt(c -> -c.parseConfidence()));
	}

	private void initUI() {
		setLayout(new BorderLayout());

		header = new JEditorPane();
		header.setContentType("text/html");
		header.setEditable(false);
		header.setText(buildHeader());

		cellRenderer = new CachingTreeCellRenderer();
		treeRoot = new TextTreeNode("Potential Malicious Activities:").bold();
		tree = buildTree();
		for (QuarkReportData.Crime crime : data.crimes) {
			treeRoot.add(new CrimeTreeNode(crime));
		}
		tree.expandRow(0);
		tree.expandRow(1);

		JScrollPane tableScroll = new JScrollPane(tree);
		tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(header, BorderLayout.PAGE_START);
		mainPanel.add(tableScroll, BorderLayout.CENTER);

		add(mainPanel);
	}

	private JTree buildTree() {
		JTree tree = new JTree(treeRoot);
		tree.setLayout(new BorderLayout());
		tree.setBorder(BorderFactory.createEmptyBorder());
		tree.setShowsRootHandles(false);
		tree.setScrollsOnExpand(false);
		tree.setSelectionModel(null);
		tree.setCellRenderer(cellRenderer);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (SwingUtilities.isLeftMouseButton(event)) {
					Object node = getNodeUnderMouse(tree, event);
					if (node instanceof MethodTreeNode) {
						JMethod method = ((MethodTreeNode) node).getJMethod();
						tabbedPane.codeJump(method);
					}
				}
			}
		});
		tree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				Object leaf = path.getLastPathComponent();
				if (leaf instanceof CrimeTreeNode) {
					CrimeTreeNode node = (CrimeTreeNode) leaf;
					Enumeration<TreeNode> children = node.children();
					while (children.hasMoreElements()) {
						TreeNode child = children.nextElement();
						tree.expandPath(path.pathByAddingChild(child));
					}
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
			}
		});
		return tree;
	}

	private String buildHeader() {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		builder.append("<h1>Quark Analysis Report</h1>");
		builder.append("<h3>");
		builder.append("File: ").append(data.apk_filename);
		builder.append("<br>");
		builder.append("Treat level: ").append(data.threat_level);
		builder.append("<br>");
		builder.append("Total score: ").append(Integer.toString(data.total_score));
		builder.append("</h3>");
		return builder.toString();
	}

	@Override
	public void loadSettings() {
		Font settingsFont = getTabbedPane().getMainWindow().getSettings().getFont();
		this.font = settingsFont.deriveFont(settingsFont.getSize2D() + 1.f);
		this.boldFont = font.deriveFont(Font.BOLD);
		header.setFont(font);
		tree.setFont(font);
		cellRenderer.clearCache();
	}

	private static Object getNodeUnderMouse(JTree tree, MouseEvent mouseEvent) {
		TreePath path = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
		return path != null ? path.getLastPathComponent() : null;
	}

	private static class CachingTreeCellRenderer implements TreeCellRenderer {
		private final Map<BaseTreeNode, Component> cache = new IdentityHashMap<>();

		@Override
		public Component getTreeCellRendererComponent(JTree tr, Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean focus) {
			return cache.computeIfAbsent((BaseTreeNode) value, BaseTreeNode::render);
		}

		public void clearCache() {
			cache.clear();
		}
	}

	private abstract static class BaseTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 7197501219150495889L;

		public BaseTreeNode(Object userObject) {
			super(userObject);
		}

		public abstract Component render();
	}

	private class TextTreeNode extends BaseTreeNode {
		private static final long serialVersionUID = 6763410122501083453L;

		private boolean bold;

		public TextTreeNode(String text) {
			super(text);
		}

		public TextTreeNode bold() {
			bold = true;
			return this;
		}

		@Override
		public Component render() {
			JLabel label = new NodeLabel(((String) getUserObject()));
			label.setFont(bold ? boldFont : font);
			label.setIcon(null);
			label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			return label;
		}
	}

	private class CrimeTreeNode extends TextTreeNode {
		private static final long serialVersionUID = -1464310215237483911L;

		private final QuarkReportData.Crime crime;

		public CrimeTreeNode(QuarkReportData.Crime crime) {
			super(crime.crime);
			this.crime = crime;
			bold();
			addDetails();
		}

		private void addDetails() {
			add(new TextTreeNode("Confidence: " + crime.confidence));
			if (Utils.notEmpty(crime.permissions)) {
				add(new TextTreeNode("Permissions: " + Strings.join(", ", crime.permissions)));
			}
			if (Utils.notEmpty(crime.native_api)) {
				TextTreeNode node = new TextTreeNode("Native API");
				for (QuarkReportData.Method method : crime.native_api) {
					node.add(new TextTreeNode(method.toString()));
				}
				add(node);
			}
			List<JsonElement> combination = crime.combination;
			if (Utils.notEmpty(combination) && combination.get(0) instanceof JsonArray) {
				TextTreeNode node = new TextTreeNode("Combination");
				int size = combination.size();
				for (int i = 0; i < size; i++) {
					TextTreeNode set = new TextTreeNode("Set " + i);
					JsonArray array = (JsonArray) combination.get(i);
					for (JsonElement ele : array) {
						String mth = ele.getAsString();
						set.add(resolveMethod(mth));
					}
					node.add(set);
				}
				add(node);
			}
			if (Utils.notEmpty(crime.register)) {
				TextTreeNode node = new TextTreeNode("Invocations");
				for (Map<String, QuarkReportData.InvokePlace> invokeMap : crime.register) {
					invokeMap.forEach((key, value) -> node.add(resolveMethod(key)));
				}
				add(node);
			}
		}

		@Override
		public String toString() {
			return crime.crime;
		}
	}

	public MutableTreeNode resolveMethod(String descr) {
		try {
			String[] parts = removeQuotes(descr).split(" ", 3);
			String cls = Utils.cleanObjectName(parts[0].replace('$', '.'));
			String mth = parts[1] + parts[2].replace(" ", "");
			MainWindow mainWindow = getTabbedPane().getMainWindow();
			JadxWrapper wrapper = mainWindow.getWrapper();
			JavaClass javaClass = wrapper.searchJavaClassByRawName(cls);
			if (javaClass == null) {
				return new TextTreeNode(cls + "." + mth);
			}
			JavaMethod javaMethod = javaClass.searchMethodByShortId(mth);
			if (javaMethod == null) {
				return new TextTreeNode(javaClass.getFullName() + "." + mth);
			}
			return new MethodTreeNode(javaMethod);
		} catch (Exception e) {
			LOG.error("Failed to parse method descriptor string: {}", descr, e);
			return new TextTreeNode(descr);
		}
	}

	private static String removeQuotes(String descr) {
		if (descr.charAt(0) == '\'') {
			return descr.substring(1, descr.length() - 1);
		}
		return descr;
	}

	private class MethodTreeNode extends BaseTreeNode {
		private static final long serialVersionUID = 4350343915220068508L;

		private final JavaMethod mth;
		private final JMethod jnode;

		public MethodTreeNode(JavaMethod mth) {
			super(mth);
			this.mth = mth;
			this.jnode = (JMethod) nodeCache.makeFrom(mth);
		}

		public JMethod getJMethod() {
			return jnode;
		}

		@Override
		public Component render() {
			JLabel label = new NodeLabel(mth.toString());
			label.setFont(font);
			label.setIcon(jnode.getIcon());
			label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			return label;
		}
	}
}
