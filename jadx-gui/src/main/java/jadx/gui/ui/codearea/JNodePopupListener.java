package jadx.gui.ui.codearea;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jadx.gui.treemodel.JNode;

public final class JNodePopupListener implements PopupMenuListener {
	private final CodeArea codeArea;
	private final List<JNodeAction> actions = new ArrayList<>();

	public JNodePopupListener(CodeArea codeArea) {
		this.codeArea = codeArea;
	}

	public void addActions(JNodeAction action) {
		actions.add(action);
	}

	private void updateNode(JNode node) {
		for (JNodeAction action : actions) {
			action.changeNode(node);
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		updateNode(codeArea.getNodeUnderMouse());
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// this event can be called just before running action, so can't reset node here
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		updateNode(null);
	}
}
