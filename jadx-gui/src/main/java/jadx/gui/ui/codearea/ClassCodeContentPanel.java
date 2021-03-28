package jadx.gui.ui.codearea;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

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

	private final transient CodePanel javaCodePanel;
	private final transient CodePanel smaliCodePanel;
	private final transient JTabbedPane areaTabbedPane;

	public ClassCodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);

		javaCodePanel = new CodePanel(new CodeArea(this));
		smaliCodePanel = new CodePanel(new SmaliArea(this));

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));

		areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		areaTabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		areaTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
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

	public CodePanel getJavaCodePanel() {
		return javaCodePanel;
	}

	public void switchPanel() {
		boolean toSmali = areaTabbedPane.getSelectedComponent() == javaCodePanel;
		areaTabbedPane.setSelectedComponent(toSmali ? smaliCodePanel : javaCodePanel);
	}

	public AbstractCodeArea getCurrentCodeArea() {
		return ((CodePanel) areaTabbedPane.getSelectedComponent()).getCodeArea();
	}

	public AbstractCodeArea getSmaliCodeArea() {
		return smaliCodePanel.getCodeArea();
	}

	public void showSmaliPane() {
		areaTabbedPane.setSelectedComponent(smaliCodePanel);
	}
}
