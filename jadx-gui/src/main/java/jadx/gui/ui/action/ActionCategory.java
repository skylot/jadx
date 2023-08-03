package jadx.gui.ui.action;

import jadx.gui.utils.NLS;

public enum ActionCategory {
	MENU_TOOLBAR("action_category.menu_toolbar"),
	CODE_AREA("action_category.code_area"),
	PLUGIN_SCRIPT("action_category.plugin_script");

	private final String nameRes;

	ActionCategory(String nameRes) {
		this.nameRes = nameRes;
	}

	public String getName() {
		if (nameRes != null) {
			return NLS.str(nameRes);
		}
		return null;
	}
}
