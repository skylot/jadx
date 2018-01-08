package jadx.gui.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.BadLocationException;
import java.awt.*;
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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.Utils;

class TabbedPane extends JTabbedPane {

	private static final Logger LOG = LoggerFactory.getLogger(TabbedPane.class);
	private static final long serialVersionUID = -8833600618794570904L;

	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_CLOSE_INACTIVE = Utils.openIcon("cross_grayed");

	private final transient MainWindow mainWindow;
	private final transient Map<JNode, ContentPanel> openTabs = new LinkedHashMap<>();
	private transient JumpManager jumps = new JumpManager();

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

	private void showCode(final Position pos) {
		final CodePanel contentPanel = (CodePanel) getContentPanel(pos.getNode());
		if (contentPanel == null) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setSelectedComponent(contentPanel);
				CodeArea codeArea = contentPanel.getCodeArea();
				int line = pos.getLine();
				if (line < 0) {
					try {
						line = 1 + codeArea.getLineOfOffset(-line);
					} catch (BadLocationException e) {
						LOG.error("Can't get line for: {}", pos, e);
						line = pos.getNode().getLine();
					}
				}
				codeArea.scrollToLine(line);
				codeArea.requestFocus();
			}
		});
	}

	public void showResource(JResource res) {
		final ContentPanel contentPanel = getContentPanel(res);
		if (contentPanel == null) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setSelectedComponent(contentPanel);
			}
		});
	}

	public void codeJump(Position pos) {
		Position curPos = getCurrentPosition();
		if (curPos != null) {
			jumps.addPosition(curPos);
			jumps.addPosition(pos);
		}
		showCode(pos);
	}

	@Nullable
	private Position getCurrentPosition() {
		ContentPanel selectedCodePanel = getSelectedCodePanel();
		if (selectedCodePanel instanceof CodePanel) {
			return ((CodePanel) selectedCodePanel).getCodeArea().getCurrentPosition();
		}
		return null;
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

	private void addContentPanel(ContentPanel contentPanel) {
		openTabs.put(contentPanel.getNode(), contentPanel);
		add(contentPanel);
	}

	private void closeCodePanel(ContentPanel contentPanel) {
		openTabs.remove(contentPanel.getNode());
		remove(contentPanel);
	}

	@Nullable
	private ContentPanel getContentPanel(JNode node) {
		ContentPanel panel = openTabs.get(node);
		if (panel == null) {
			panel = makeContentPanel(node);
			if (panel == null) {
				return null;
			}
			addContentPanel(panel);
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
		}
		return panel;
	}

	@Nullable
	private ContentPanel makeContentPanel(JNode node) {
		if (node instanceof JResource) {
			JResource res = (JResource) node;
			ResourceFile resFile = res.getResFile();
			if (resFile != null) {
				if (resFile.getType() == ResourceType.IMG) {
					return new ImagePanel(this, res);
				}
			} else {
				return null;
			}
		}
		return new CodePanel(this, node);
	}

	@Nullable
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
					List<ContentPanel> contentPanels = new ArrayList<>(openTabs.values());
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
		List<ContentPanel> contentPanels = new ArrayList<>(openTabs.values());
		for (ContentPanel panel : contentPanels) {
			closeCodePanel(panel);
		}
	}

	public void loadSettings() {
		for (ContentPanel panel : openTabs.values()) {
			panel.loadSettings();
		}
	}
}
