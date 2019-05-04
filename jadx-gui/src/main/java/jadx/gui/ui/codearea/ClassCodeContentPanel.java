package jadx.gui.ui.codearea;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.utils.NLS;

/**
 * Displays one class with two different view:
 *
 * <ul>
 * <li>Java source code of the selected class (default)</li>
 * <li>Smali source code of the selected class</li>
 * </ul>
 */
public final class ClassCodeContentPanel extends AbstractCodeContentPanel {
	private static final long serialVersionUID = -7229931102504634591L;

	private final CodePanel javaCodePanel;
	private final CodePanel smaliCodePanel;
	private JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);

	public ClassCodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);

		javaCodePanel = new CodePanel(this, new CodeArea(this));
		smaliCodePanel = new CodePanel(this, new SmaliArea(this));

		setLayout(new BorderLayout());

		areaTabbedPane.add(javaCodePanel, NLS.str("tabs.code"));
		areaTabbedPane.add(smaliCodePanel, NLS.str("tabs.smali"));
		add(areaTabbedPane);

		javaCodePanel.load();

		areaTabbedPane.addChangeListener(e -> {
			CodePanel selectedPanel = (CodePanel) areaTabbedPane.getSelectedComponent();
			selectedPanel.load();
		});
	}

	@Override
	public void loadSettings() {
		javaCodePanel.loadSettings();
		smaliCodePanel.loadSettings();
		updateUI();
	}

	@Override
	public TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	@Override
	public JNode getNode() {
		return node;
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		return javaCodePanel.getCodeArea();
	}

}
