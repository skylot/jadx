package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;

public class BinaryContentPanel extends AbstractCodeContentPanel {
	private final transient CodePanel textCodePanel;
	private final transient CodePanel hexCodePanel;
	private final transient HexConfigurationPanel hexConfigurationPanel;

	public BinaryContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		textCodePanel = new CodePanel(new CodeArea(this, jnode));
		HexArea hexArea = new HexArea(this, jnode);
		hexConfigurationPanel = new HexConfigurationPanel(hexArea.getConfiguration());
		hexArea.setConfigurationPanel(hexConfigurationPanel);
		hexCodePanel = new CodePanel(hexArea);
		JTabbedPane areaTabbedPane = buildTabbedPane();
		add(areaTabbedPane);

		textCodePanel.load();
	}

	private JTabbedPane buildTabbedPane() {
		JSplitPane hexSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hexCodePanel, hexConfigurationPanel);
		hexSplitPanel.setResizeWeight(0.8);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.add(textCodePanel, "Text");
		tabbedPane.add(hexSplitPanel, "Hex");
		tabbedPane.addChangeListener(e -> {
			Component selectedComponent = tabbedPane.getSelectedComponent();
			CodePanel selectedPanel;
			if (selectedComponent instanceof CodePanel) {
				selectedPanel = (CodePanel) selectedComponent;
			} else if (selectedComponent instanceof JSplitPane) {
				selectedPanel = (CodePanel) ((JSplitPane) selectedComponent).getLeftComponent();
			} else {
				throw new RuntimeException("tabbedPane.getSelectedComponent returned a Component " +
						"of unexpected type " + selectedComponent.getClass().getName());
			}
			selectedPanel.load();
		});
		return tabbedPane;
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		return textCodePanel.getCodeArea();
	}

	@Override
	public void loadSettings() {
		textCodePanel.loadSettings();
		hexCodePanel.loadSettings();
		updateUI();
	}

	@Override
	public JadxSettings getSettings() {
		JadxSettings settings = super.getSettings();
		settings.setLineNumbersMode(LineNumbersMode.NORMAL);
		return settings;
	}
}
