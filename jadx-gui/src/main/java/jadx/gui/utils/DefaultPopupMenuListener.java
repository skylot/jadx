package jadx.gui.utils;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public interface DefaultPopupMenuListener extends PopupMenuListener {
	@Override
	default void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	}

	@Override
	default void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	}

	@Override
	default void popupMenuCanceled(PopupMenuEvent e) {
	}
}
