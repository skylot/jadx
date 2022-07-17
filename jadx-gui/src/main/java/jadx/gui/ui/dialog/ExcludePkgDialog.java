package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jadx.api.JavaPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class ExcludePkgDialog extends JDialog {
	private static final long serialVersionUID = -1111111202104151030L;

	private final transient MainWindow mainWindow;
	private transient JTree tree;
	private transient DefaultMutableTreeNode treeRoot;
	private final transient List<PkgNode> roots = new ArrayList<>();

	public ExcludePkgDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		initUI();
		UiUtils.addEscapeShortCutToDispose(this);
		initPackageList();
	}

	private void initUI() {
		setTitle(NLS.str("exclude_dialog.title"));
		tree = new JTree();
		tree.setRowHeight(-1);
		treeRoot = new DefaultMutableTreeNode("Packages");
		DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
		tree.setModel(treeModel);
		tree.setCellRenderer(new PkgListCellRenderer());
		JScrollPane listPanel = new JScrollPane(tree);
		listPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					PkgNode node = (PkgNode) path.getLastPathComponent();
					node.toggle();
					repaint();
				}
			}
		});

		JPanel actionPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(actionPanel, BoxLayout.LINE_AXIS);
		actionPanel.setLayout(boxLayout);
		actionPanel.add(new Label(" "));
		JButton btnOk = new JButton(NLS.str("exclude_dialog.ok"));
		JButton btnAll = new JButton(NLS.str("exclude_dialog.select_all"));
		JButton btnInvert = new JButton(NLS.str("exclude_dialog.invert"));
		JButton btnDeselect = new JButton(NLS.str("exclude_dialog.deselect"));
		actionPanel.add(btnDeselect);
		actionPanel.add(btnInvert);
		actionPanel.add(btnAll);
		actionPanel.add(new Label(" "));
		actionPanel.add(btnOk);

		JPanel mainPane = new JPanel(new BorderLayout(5, 5));
		mainPane.add(listPanel, BorderLayout.CENTER);
		mainPane.add(actionPanel, BorderLayout.SOUTH);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		getContentPane().add(mainPane);
		pack();
		setSize(600, 700);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);

		btnOk.addActionListener(e -> {
			mainWindow.getWrapper().setExcludedPackages(getExcludes());
			mainWindow.reopen();
			dispose();
		});
		btnAll.addActionListener(e -> {
			roots.forEach(p -> p.setSelected(true));
			tree.updateUI();
		});
		btnDeselect.addActionListener(e -> {
			roots.forEach(p -> p.setSelected(false));
			tree.updateUI();
		});
		btnInvert.addActionListener(e -> {
			roots.forEach(PkgNode::toggle);
			tree.updateUI();
		});
	}

	private void initPackageList() {
		List<String> pkgs = mainWindow.getWrapper().getPackages()
				.stream()
				.map(JavaPackage::getFullName)
				.collect(Collectors.toList());
		getPackageTree(pkgs).forEach(treeRoot::add);
		initCheckbox();
		tree.expandPath(new TreePath(treeRoot.getPath()));
	}

	private List<PkgNode> getPackageTree(List<String> names) {
		List<PkgNode> roots = new ArrayList<>();
		Set<String> nameSet = new HashSet<>();
		Map<String, List<PkgNode>> childMap = new HashMap<>();
		for (String name : names) {
			String parent = "";
			int last = 0;
			do {
				int pos = name.indexOf(".", last);
				if (pos == -1) {
					pos = name.length();
				}
				String fullName = name.substring(0, pos);
				if (!nameSet.contains(fullName)) {
					nameSet.add(fullName);
					PkgNode node = new PkgNode(fullName, name.substring(last, pos));
					if (!parent.isEmpty()) {
						childMap.computeIfAbsent(parent, k -> new ArrayList<>())
								.add(node);
					} else {
						roots.add(node);
					}
				}
				parent = fullName;
				last = pos + 1;
			} while (last < name.length());
		}
		addToParent(null, roots, childMap);
		return this.roots;
	}

	private PkgNode addToParent(PkgNode parent, List<PkgNode> roots, Map<String, List<PkgNode>> childMap) {
		for (PkgNode root : roots) {
			String tempFullName = root.getFullName();
			do {
				List<PkgNode> children = childMap.get(tempFullName);
				if (children != null) {
					if (children.size() == 1) {
						PkgNode next = children.get(0);
						next.name = root.name + "." + next.name;
						tempFullName = next.fullName;
						next.fullName = root.fullName;
						root = next;
						continue;
					} else {
						addToParent(root, children, childMap);
					}
				}
				if (parent == null) {
					this.roots.add(root);
				} else {
					parent.add(root);
				}
				break;
			} while (true);
		}
		return parent;
	}

	private List<String> getExcludes() {
		List<String> excludes = new ArrayList<>();
		walkTree(true, p -> excludes.add(p.getFullName()));
		return excludes;
	}

	private void initCheckbox() {
		Font tmp = mainWindow.getSettings().getFont();
		Font font = tmp.deriveFont(tmp.getSize() + 1.f);
		Set<String> excluded = new HashSet<>(mainWindow.getWrapper().getExcludedPackages());
		walkTree(false, p -> p.initCheckbox(excluded.contains(p.getFullName()), font));
	}

	private void walkTree(boolean findSelected, Consumer<PkgNode> consumer) {
		List<PkgNode> queue = new ArrayList<>(roots);
		for (int i = 0; i < queue.size(); i++) {
			PkgNode node = queue.get(i);
			if (findSelected && node.isSelected()) {
				consumer.accept(node);
			} else {
				if (!findSelected) {
					consumer.accept(node);
				}
				for (int j = 0; j < node.getChildCount(); j++) {
					queue.add((PkgNode) node.getChildAt(j));
				}
			}
		}
	}

	private static class PkgNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = -1111111202104151430L;

		String name;
		String fullName;
		JCheckBox checkbox;

		PkgNode(String fullName, String name) {
			this.name = name;
			this.fullName = fullName;
		}

		void initCheckbox(boolean select, Font font) {
			if (!select) {
				if (getParent() instanceof PkgNode) {
					select = ((PkgNode) getParent()).isSelected();
				}
			}
			checkbox = new JCheckBox(name, select);
			checkbox.setFont(font);
		}

		boolean toggle() {
			boolean selected = !checkbox.isSelected();
			setSelected(selected);
			toggleParents(selected);
			return selected;
		}

		void toggleParents(boolean select) {
			if (getParent() instanceof PkgNode) {
				PkgNode p = ((PkgNode) getParent());
				if (select) {
					select = p.isChildrenAllSelected();
					if (select) {
						p.checkbox.setSelected(true);
						p.toggleParents(true);
					}
				} else {
					p.checkbox.setSelected(false);
					p.toggleParents(false);
				}
			}
		}

		void setSelected(boolean select) {
			checkbox.setSelected(select);
			for (int i = 0; i < getChildCount(); i++) {
				((PkgNode) getChildAt(i)).setSelected(select);
			}
		}

		boolean isSelected() {
			return checkbox.isSelected();
		}

		String getFullName() {
			return fullName;
		}

		String getDisplayName() {
			return name;
		}

		boolean isChildrenAllSelected() {
			for (int i = 0; i < getChildCount(); i++) {
				if (!((PkgNode) getChildAt(i)).isSelected()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class PkgListCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = -1111111202104151235L;

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			if (value instanceof PkgNode) {
				PkgNode node = (PkgNode) value;
				node.checkbox.setBackground(tree.getBackground());
				node.checkbox.setForeground(tree.getForeground());
				return node.checkbox;
			}
			Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			setIcon(Icons.PACKAGE);
			return c;
		}
	}
}
