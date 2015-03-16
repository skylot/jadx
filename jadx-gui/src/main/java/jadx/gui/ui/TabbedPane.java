package jadx.gui.ui;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.Utils;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TabbedPane extends JTabbedPane {

	private static final long serialVersionUID = -8833600618794570904L;

	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_CLOSE_INACTIVE = Utils.openIcon("cross_grayed");

	private final MainWindow mainWindow;
	private final Map<JNode, ContentPanel> openTabs = new LinkedHashMap<JNode, ContentPanel>();
	private JumpManager jumps = new JumpManager();

	TabbedPane(MainWindow window) {
		mainWindow = window;

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				int direction = e.getWheelRotation();
				int index = getSelectedIndex();
				int maxIndex = getTabCount() - 1;
				if ((index == 0 && direction < 0)
						|| (index == maxIndex && direction > 0)) {
					index = maxIndex - index;
				} else {
					index += direction;
				}
				setSelectedIndex(index);
			}
		});
	}

	MainWindow getMainWindow() {
		return mainWindow;
	}

	void showCode(final Position pos) {
		final ContentPanel contentPanel = getCodePanel(pos.getNode());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setSelectedComponent(contentPanel);
				ContentArea contentArea = contentPanel.getContentArea();
				contentArea.scrollToLine(pos.getLine());
				contentArea.requestFocus();
			}
		});
	}

	public void navBack() {
		Position pos = jumps.getPrev();
		if (pos != null) {
			showCode(pos);
		}
	}

	public void navForward() {
		Position pos = jumps.getNext();
		if (pos != null) {
			showCode(pos);
		}
	}

	public JumpManager getJumpManager() {
		return jumps;
	}

	private void addCodePanel(ContentPanel contentPanel) {
		openTabs.put(contentPanel.getNode(), contentPanel);
		add(contentPanel);
	}

	private void closeCodePanel(ContentPanel contentPanel) {
		openTabs.remove(contentPanel.getNode());
		remove(contentPanel);
	}

	private ContentPanel getCodePanel(JNode cls) {
		ContentPanel panel = openTabs.get(cls);
		if (panel == null) {
			panel = new ContentPanel(this, cls);
			addCodePanel(panel);
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
		}
		return panel;
	}

	ContentPanel getSelectedCodePanel() {
		return (ContentPanel) getSelectedComponent();
	}

	private Component makeTabComponent(final ContentPanel contentPanel) {
		JNode node = contentPanel.getNode();
		String name = node.makeLongString();

		final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		panel.setOpaque(false);

		final JLabel label = new JLabel(name);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		label.setIcon(node.getIcon());

		final JButton button = new JButton();
		button.setIcon(ICON_CLOSE_INACTIVE);
		button.setRolloverIcon(ICON_CLOSE);
		button.setRolloverEnabled(true);
		button.setOpaque(false);
		button.setUI(new BasicButtonUI());
		button.setContentAreaFilled(false);
		button.setFocusable(false);
		button.setBorder(null);
		button.setBorderPainted(false);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeCodePanel(contentPanel);
			}
		});

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					closeCodePanel(contentPanel);
				} else if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = createTabPopupMenu(contentPanel);
					menu.show(panel, e.getX(), e.getY());
				} else {
					// TODO: make correct event delegation to tabbed pane
					setSelectedComponent(contentPanel);
				}
			}
		});

		panel.add(label);
		panel.add(button);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		return panel;
	}

	private JPopupMenu createTabPopupMenu(final ContentPanel contentPanel) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem closeTab = new JMenuItem(NLS.str("tabs.close"));
		closeTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeCodePanel(contentPanel);
			}
		});
		menu.add(closeTab);

		if (openTabs.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<ContentPanel> contentPanels = new ArrayList<ContentPanel>(openTabs.values());
					for (ContentPanel panel : contentPanels) {
						if (panel != contentPanel) {
							closeCodePanel(panel);
						}
					}
				}
			});
			menu.add(closeOther);

			JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
			closeAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					closeAllTabs();
				}
			});
			menu.add(closeAll);
			menu.addSeparator();

			ContentPanel selectedContentPanel = getSelectedCodePanel();
			for (final Map.Entry<JNode, ContentPanel> entry : openTabs.entrySet()) {
				final ContentPanel cp = entry.getValue();
				if (cp == selectedContentPanel) {
					continue;
				}
				JNode node = entry.getKey();
				final String clsName = node.makeLongString();
				JMenuItem item = new JMenuItem(clsName);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedComponent(cp);
					}
				});
				item.setIcon(node.getIcon());
				menu.add(item);
			}
		}
		return menu;
	}

	public void closeAllTabs() {
		List<ContentPanel> contentPanels = new ArrayList<ContentPanel>(openTabs.values());
		for (ContentPanel panel : contentPanels) {
			closeCodePanel(panel);
		}
	}

	public void loadSettings() {
		for (ContentPanel panel : openTabs.values()) {
			panel.getContentArea().loadSettings();
		}
	}
}
