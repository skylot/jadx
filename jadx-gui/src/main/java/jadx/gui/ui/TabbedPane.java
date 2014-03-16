package jadx.gui.ui;

import jadx.gui.treemodel.JClass;
import jadx.gui.utils.NLS;
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
	private final Map<JClass, CodePanel> openTabs = new LinkedHashMap<JClass, CodePanel>();

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

	void showCode(final JClass cls, final int line) {
		final CodePanel codePanel = getCodePanel(cls);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setSelectedComponent(codePanel);
				CodeArea codeArea = codePanel.getCodeArea();
				codeArea.scrollToLine(line);
				codeArea.requestFocus();
			}
		});
	}

	private void addCodePanel(CodePanel codePanel) {
		openTabs.put(codePanel.getCls(), codePanel);
		add(codePanel);
	}

	private void closeCodePanel(CodePanel codePanel) {
		openTabs.remove(codePanel.getCls());
		remove(codePanel);
	}

	private CodePanel getCodePanel(JClass cls) {
		CodePanel panel = openTabs.get(cls);
		if (panel == null) {
			panel = new CodePanel(this, cls);
			addCodePanel(panel);
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
		}
		return panel;
	}

	private CodePanel getCodePanel(int index) {
		Component component = getComponent(index);
		if (component instanceof CodePanel) {
			return (CodePanel) component;
		}
		return null;
	}

	CodePanel getSelectedCodePanel() {
		return (CodePanel) getSelectedComponent();
	}

	private Component makeTabComponent(final CodePanel codePanel) {
		JClass cls = codePanel.getCls();
		String name = cls.getCls().getFullName();

		final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		panel.setOpaque(false);

		final JLabel label = new JLabel(name);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		label.setIcon(cls.getIcon());

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
				closeCodePanel(codePanel);
			}
		});

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					closeCodePanel(codePanel);
				} else if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = createTabPopupMenu(codePanel);
					menu.show(panel, e.getX(), e.getY());
				} else {
					// TODO: make correct event delegation to tabbed pane
					setSelectedComponent(codePanel);
				}
			}
		});

		panel.add(label);
		panel.add(button);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		return panel;
	}

	private JPopupMenu createTabPopupMenu(final CodePanel codePanel) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem closeTab = new JMenuItem(NLS.str("tabs.close"));
		closeTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeCodePanel(codePanel);
			}
		});
		menu.add(closeTab);

		if (openTabs.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<CodePanel> codePanels = new ArrayList<CodePanel>(openTabs.values());
					for (CodePanel panel : codePanels) {
						if (panel != codePanel) {
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
					List<CodePanel> codePanels = new ArrayList<CodePanel>(openTabs.values());
					for (CodePanel panel : codePanels) {
						closeCodePanel(panel);
					}
				}
			});
			menu.add(closeAll);
			menu.addSeparator();

			CodePanel selectedCodePanel = getSelectedCodePanel();
			for (final Map.Entry<JClass, CodePanel> entry : openTabs.entrySet()) {
				final CodePanel cp = entry.getValue();
				if (cp == selectedCodePanel) {
					continue;
				}
				JClass jClass = entry.getKey();
				final String clsName = jClass.getCls().getFullName();
				JMenuItem item = new JMenuItem(clsName);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedComponent(cp);
					}
				});
				item.setIcon(jClass.getIcon());
				menu.add(item);
			}
		}
		return menu;
	}
}
