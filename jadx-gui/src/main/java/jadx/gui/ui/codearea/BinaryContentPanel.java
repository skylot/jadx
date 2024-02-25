package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.tab.TabbedPane;

public class BinaryContentPanel extends AbstractCodeContentPanel {
	private final transient CodePanel textCodePanel;
	private final transient CodePanel hexCodePanel;
	private final transient HexConfigurationPanel hexConfigurationPanel;
	private final transient JTabbedPane areaTabbedPane;

	public BinaryContentPanel(TabbedPane panel, JNode jnode) {
		this(panel, jnode, true);
	}

	public BinaryContentPanel(TabbedPane panel, JNode jnode, boolean supportsText) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		if (supportsText) {
			textCodePanel = new CodePanel(new CodeArea(this, jnode));
		} else {
			textCodePanel = null;
		}
		HexArea hexArea = new HexArea(this, jnode);
		hexConfigurationPanel = new HexConfigurationPanel(hexArea.getConfiguration());
		hexArea.setConfigurationPanel(hexConfigurationPanel);
		hexCodePanel = new CodePanel(hexArea);
		areaTabbedPane = buildTabbedPane();
		add(areaTabbedPane);

		getSelectedPanel().load();
	}

	private JTabbedPane buildTabbedPane() {
		JSplitPane hexSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hexCodePanel, hexConfigurationPanel);
		hexSplitPanel.setResizeWeight(0.8);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		if (textCodePanel != null) {
			tabbedPane.add(textCodePanel, "Text");
		}
		tabbedPane.add(hexSplitPanel, "Hex");
		tabbedPane.addChangeListener(e -> {
			getSelectedPanel().load();
		});
		return tabbedPane;
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		if (textCodePanel != null) {
			return textCodePanel.getCodeArea();
		} else {
			return hexCodePanel.getCodeArea();
		}
	}

	@Override
	public void loadSettings() {
		if (textCodePanel != null) {
			textCodePanel.loadSettings();
		}
		hexCodePanel.loadSettings();
		updateUI();
	}

	@Override
	public JadxSettings getSettings() {
		JadxSettings settings = super.getSettings();
		settings.setLineNumbersMode(LineNumbersMode.NORMAL);
		return settings;
	}

	private CodePanel getSelectedPanel() {
		Component selectedComponent = areaTabbedPane.getSelectedComponent();
		CodePanel selectedPanel;
		if (selectedComponent instanceof CodePanel) {
			selectedPanel = (CodePanel) selectedComponent;
		} else if (selectedComponent instanceof JSplitPane) {
			selectedPanel = (CodePanel) ((JSplitPane) selectedComponent).getLeftComponent();
		} else {
			throw new RuntimeException("tabbedPane.getSelectedComponent returned a Component "
					+ "of unexpected type " + selectedComponent);
		}
		return selectedPanel;
	}
}
