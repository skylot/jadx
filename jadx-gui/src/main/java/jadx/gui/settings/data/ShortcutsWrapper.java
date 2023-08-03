package jadx.gui.settings.data;

import java.util.Map;

import jadx.gui.ui.action.ActionModel;
import jadx.gui.utils.shortcut.Shortcut;

public class ShortcutsWrapper {
	private Map<ActionModel, Shortcut> shortcuts;

	public void updateShortcuts(Map<ActionModel, Shortcut> shortcuts) {
		this.shortcuts = shortcuts;
	}

	public Shortcut get(ActionModel actionModel) {
		return shortcuts.getOrDefault(actionModel, actionModel.getDefaultShortcut());
	}

	public void put(ActionModel actionModel, Shortcut shortcut) {
		shortcuts.put(actionModel, shortcut);
	}
}
