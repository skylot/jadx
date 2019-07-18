package jadx.gui.ui.codearea;

import java.awt.*;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;

public final class CodeContentPanel extends AbstractCodeContentPanel {
	private static final long serialVersionUID = 5310536092010045565L;

	private final CodePanel codePanel;

	public CodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		codePanel = new CodePanel(new CodeArea(this));
		add(codePanel, BorderLayout.CENTER);
		codePanel.load();
	}

	@Override
	public void loadSettings() {
		codePanel.loadSettings();
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

	SearchBar getSearchBar() {
		return codePanel.getSearchBar();
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		return codePanel.getCodeArea();
	}

	@Override
	public String getTabTooltip() {
		String s = node.getName();
		JNode n = (JNode) node.getParent();
		while (n != null) {
			String name = n.getName();
			if (name == null) {
				break;
			}
			s = name + '/' + s;
			n = (JNode) n.getParent();
		}
		return '/' + s;
	}
}
