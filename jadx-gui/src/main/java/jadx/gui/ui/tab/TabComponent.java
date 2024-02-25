package jadx.gui.ui.tab;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

import jadx.core.utils.ListUtils;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.dnd.TabDndGestureListener;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public class TabComponent extends JPanel {
	private static final long serialVersionUID = -8147035487543610321L;

	private final TabbedPane tabbedPane;
	private final ContentPanel contentPanel;

	private JLabel label;

	public TabComponent(TabbedPane tabbedPane, ContentPanel contentPanel) {
		this.tabbedPane = tabbedPane;
		this.contentPanel = contentPanel;

		init();
	}

	public void loadSettings() {
		label.setFont(getLabelFont());
		if (tabbedPane.getDnd() != null) {
			tabbedPane.getDnd().loadSettings();
		}
	}

	private Font getLabelFont() {
		return tabbedPane.getMainWindow().getSettings().getFont().deriveFont(Font.BOLD);
	}

	private void init() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setOpaque(false);

		JNode node = contentPanel.getNode();
		label = new NodeLabel(buildTabTitle(node), node.disableHtml());
		label.setFont(getLabelFont());
		String toolTip = contentPanel.getTabTooltip();
		if (toolTip != null) {
			setToolTipText(toolTip);
		}
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		label.setIcon(node.getIcon());
		if (node instanceof JEditableNode) {
			((JEditableNode) node).addChangeListener(c -> label.setText(buildTabTitle(node)));
		}

		final JButton closeBtn = new JButton();
		closeBtn.setIcon(Icons.CLOSE_INACTIVE);
		closeBtn.setRolloverIcon(Icons.CLOSE);
		closeBtn.setRolloverEnabled(true);
		closeBtn.setOpaque(false);
		closeBtn.setUI(new BasicButtonUI());
		closeBtn.setContentAreaFilled(false);
		closeBtn.setFocusable(false);
		closeBtn.setBorder(null);
		closeBtn.setBorderPainted(false);
		closeBtn.addActionListener(e -> tabbedPane.closeCodePanel(contentPanel));

		MouseAdapter clickAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					tabbedPane.closeCodePanel(contentPanel);
				} else if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = createTabPopupMenu(contentPanel);
					menu.show(e.getComponent(), e.getX(), e.getY());
				} else if (SwingUtilities.isLeftMouseButton(e)) {
					if (tabbedPane.getSelectedComponent() != contentPanel) {
						tabbedPane.selectTab(contentPanel);
					}
				}
			}
		};
		addMouseListener(clickAdapter);
		addListenerForDnd();

		add(label);
		add(closeBtn);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	private void addListenerForDnd() {
		if (tabbedPane.getDnd() == null) {
			return;
		}
		TabComponent comp = this;
		DragGestureListener dgl = new TabDndGestureListener(tabbedPane.getDnd()) {
			@Override
			protected Point getDragOrigin(DragGestureEvent e) {
				return SwingUtilities.convertPoint(comp, e.getDragOrigin(), tabbedPane);
			}
		};
		DragSource.getDefaultDragSource()
				.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
	}

	private String buildTabTitle(JNode node) {
		String tabTitle;
		if (node.getRootClass() != null) {
			tabTitle = node.getRootClass().getName();
		} else {
			tabTitle = node.makeLongStringHtml();
		}
		if (node instanceof JEditableNode) {
			if (((JEditableNode) node).isChanged()) {
				return "*" + tabTitle;
			}
		}
		return tabTitle;
	}

	private JPopupMenu createTabPopupMenu(final ContentPanel contentPanel) {
		JPopupMenu menu = new JPopupMenu();

		String nodeFullName = getNodeFullName(contentPanel);
		if (nodeFullName != null) {
			JMenuItem copyRootClassName = new JMenuItem(NLS.str("tabs.copy_class_name"));
			copyRootClassName.addActionListener(actionEvent -> UiUtils.setClipboardString(nodeFullName));
			menu.add(copyRootClassName);
			menu.addSeparator();
		}

		JMenuItem closeTab = new JMenuItem(NLS.str("tabs.close"));
		closeTab.addActionListener(e -> tabbedPane.closeCodePanel(contentPanel));
		menu.add(closeTab);

		List<ContentPanel> tabs = tabbedPane.getTabs();
		if (tabs.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(e -> {
				for (ContentPanel panel : tabs) {
					if (panel != contentPanel) {
						tabbedPane.closeCodePanel(panel);
					}
				}
			});
			menu.add(closeOther);

			JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
			closeAll.addActionListener(e -> tabbedPane.closeAllTabs());
			menu.add(closeAll);

			if (contentPanel != ListUtils.last(tabs)) {
				JMenuItem closeAllRight = new JMenuItem(NLS.str("tabs.closeAllRight"));
				closeAllRight.addActionListener(e -> {
					boolean pastCurrentPanel = false;
					for (ContentPanel panel : tabs) {
						if (!pastCurrentPanel) {
							if (panel == contentPanel) {
								pastCurrentPanel = true;
							}
						} else {
							tabbedPane.closeCodePanel(panel);
						}
					}
				});
				menu.add(closeAllRight);
			}
			menu.addSeparator();

			ContentPanel selectedContentPanel = tabbedPane.getSelectedContentPanel();
			for (ContentPanel tab : tabs) {
				if (tab == selectedContentPanel) {
					continue;
				}
				JNode node = tab.getNode();
				final String clsName = node.makeLongString();
				JMenuItem item = new JMenuItem(clsName);
				item.addActionListener(e -> tabbedPane.setSelectedComponent(tab));
				item.setIcon(node.getIcon());
				menu.add(item);
			}
		}
		return menu;
	}

	private String getNodeFullName(ContentPanel contentPanel) {
		JNode node = contentPanel.getNode();
		JClass jClass = node.getRootClass();
		if (jClass != null) {
			return jClass.getFullName();
		}
		return node.getName();
	}
}
