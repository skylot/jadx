package jadx.gui.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class TabComponent extends JPanel {
	private static final long serialVersionUID = -8147035487543610321L;

	private static final ImageIcon ICON_CLOSE = UiUtils.openIcon("cross");
	private static final ImageIcon ICON_CLOSE_INACTIVE = UiUtils.openIcon("cross_grayed");

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
		JPanel panel = this;
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 0));
		panel.setOpaque(false);

		JNode node = contentPanel.getNode();
		String tabTitle;
		if (node.getRootClass() != null) {
			tabTitle = node.getRootClass().getName();
		} else {
			tabTitle = node.makeLongStringHtml();
		}
		label = new JLabel(tabTitle);
		label.setFont(getLabelFont());
		String toolTip = contentPanel.getTabTooltip();
		if (toolTip != null) {
			label.setToolTipText(toolTip);
		}
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		label.setIcon(node.getIcon());

		final JButton closeBtn = new JButton();
		closeBtn.setIcon(ICON_CLOSE_INACTIVE);
		closeBtn.setRolloverIcon(ICON_CLOSE);
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
						tabbedPane.setSelectedComponent(contentPanel);
					}
				}
			}
		};
		panel.addMouseListener(clickAdapter);
		label.addMouseListener(clickAdapter);
		closeBtn.addMouseListener(clickAdapter);

		panel.add(label);
		panel.add(closeBtn);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
