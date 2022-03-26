package jadx.gui.ui.codearea;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.PopupMenuEvent;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.DefaultPopupMenuListener;

public final class JNodePopupListener implements DefaultPopupMenuListener {
	private final CodeArea codeArea;
	private final List<JNodeAction> actions = new ArrayList<>();

	public JNodePopupListener(CodeArea codeArea) {
		this.codeArea = codeArea;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		JNode node = codeArea.getNodeUnderMouse();
		actions.forEach(action -> action.changeNode(node));
	}

	public void addActions(JNodeAction action) {
		actions.add(action);
	}
}
