package jadx.gui.settings.data;

import java.util.HashMap;

import jadx.gui.ui.action.ActionModel;
import jadx.gui.utils.shortcut.Shortcut;

public class ShortcutsMap extends HashMap<ActionModel, Shortcut> {
	@Override
	public Shortcut get(Object key) {
		if (key instanceof ActionModel) {
			return getOrDefault(key, ((ActionModel) key).getDefaultShortcut());
		}
		return super.get(key);
	}
}
