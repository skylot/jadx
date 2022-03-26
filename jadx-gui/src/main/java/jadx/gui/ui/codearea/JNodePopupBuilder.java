package jadx.gui.ui.codearea;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuListener;

public class JNodePopupBuilder {
	private final JPopupMenu menu;
	private final JNodePopupListener popupListener;

	public JNodePopupBuilder(CodeArea codeArea, JPopupMenu popupMenu) {
		menu = popupMenu;
		popupListener = new JNodePopupListener(codeArea);
		popupMenu.addPopupMenuListener(popupListener);
	}

	public void addSeparator() {
		menu.addSeparator();
	}

	public void add(JNodeAction nodeAction) {
		menu.add(nodeAction);
		popupListener.addActions(nodeAction);
	}

	public void add(Action action) {
		menu.add(action);
		if (action instanceof PopupMenuListener) {
			menu.addPopupMenuListener((PopupMenuListener) action);
		}
	}

	public JPopupMenu getMenu() {
		return menu;
	}
}
