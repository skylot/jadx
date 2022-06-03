package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Point;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.IViewStateSupport;

public final class CodeContentPanel extends AbstractCodeContentPanel implements IViewStateSupport {
	private static final long serialVersionUID = 5310536092010045565L;

	private final CodePanel codePanel;

	public CodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		codePanel = new CodePanel(new CodeArea(this, jnode));
		add(codePanel, BorderLayout.CENTER);
		codePanel.load();
	}

	@Override
	public void loadSettings() {
		codePanel.loadSettings();
		updateUI();
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

	@Override
	public EditorViewState getEditorViewState() {
		int caretPos = codePanel.getCodeArea().getCaretPosition();
		Point viewPoint = codePanel.getCodeScrollPane().getViewport().getViewPosition();
		return new EditorViewState(getNode(), "", caretPos, viewPoint);
	}

	@Override
	public void restoreEditorViewState(EditorViewState viewState) {
		codePanel.getCodeScrollPane().getViewport().setViewPosition(viewState.getViewPoint());
		codePanel.getCodeArea().setCaretPosition(viewState.getCaretPos());
	}

	@Override
	public void dispose() {
		codePanel.dispose();
	}
}
