package jadx.gui.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
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
			label.setToolTipText(toolTip);
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
			public void mouseClicked(MouseEvent e) {
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
		label.addMouseListener(clickAdapter);
		closeBtn.addMouseListener(clickAdapter);

		add(label);
		add(closeBtn);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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

		Map<JNode, ContentPanel> openTabs = tabbedPane.getOpenTabs();
		if (openTabs.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(e -> {
				List<ContentPanel> contentPanels = new ArrayList<>(openTabs.values());
				for (ContentPanel panel : contentPanels) {
					if (panel != contentPanel) {
						tabbedPane.closeCodePanel(panel);
					}
				}
			});
			menu.add(closeOther);

			JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
			closeAll.addActionListener(e -> tabbedPane.closeAllTabs());
			menu.add(closeAll);
			menu.addSeparator();

			ContentPanel selectedContentPanel = tabbedPane.getSelectedCodePanel();
			for (final Map.Entry<JNode, ContentPanel> entry : openTabs.entrySet()) {
				final ContentPanel cp = entry.getValue();
				if (cp == selectedContentPanel) {
					continue;
				}
				JNode node = entry.getKey();
				final String clsName = node.makeLongString();
				JMenuItem item = new JMenuItem(clsName);
				item.addActionListener(e -> tabbedPane.setSelectedComponent(cp));
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
