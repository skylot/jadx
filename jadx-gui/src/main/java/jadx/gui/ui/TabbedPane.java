package jadx.gui.ui;

import jadx.gui.treemodel.JClass;
import jadx.gui.utils.Utils;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

class TabbedPane extends JTabbedPane {

	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_CLOSE_INACTIVE = Utils.openIcon("cross_grayed");

	private final MainWindow mainWindow;
	private final Map<JClass, CodePanel> openTabs = new HashMap<JClass, CodePanel>();

	TabbedPane(MainWindow window) {
		mainWindow = window;

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	private void addCodePanel(CodePanel codePanel) {
		add(codePanel);
		openTabs.put(codePanel.getCls(), codePanel);
	}

	private void closeCodePanel(CodePanel codePanel) {
		remove(codePanel);
		openTabs.remove(codePanel.getCls());
	}

	void showCode(JClass cls, int line) {
		CodePanel panel = openTabs.get(cls);
		if (panel == null) {
			panel = new CodePanel(this, cls);
			addCodePanel(panel);
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
		}

		setSelectedComponent(panel);
		CodeArea codeArea = panel.getCodeArea();
		codeArea.scrollToLine(line);
		codeArea.requestFocus();
	}

	private Component makeTabComponent(final CodePanel codePanel) {
		JClass cls = codePanel.getCls();
		String name = cls.getCls().getFullName();
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
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
				if (e.getButton() == MouseEvent.BUTTON2) {
					closeCodePanel(codePanel);
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

	CodePanel getSelectedCodePanel() {
		return (CodePanel) getSelectedComponent();
	}

	MainWindow getMainWindow() {
		return mainWindow;
	}
}
