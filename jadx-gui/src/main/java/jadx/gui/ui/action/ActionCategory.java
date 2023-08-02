package jadx.gui.ui.action;

public enum ActionCategory {
	MENU_TOOLBAR("action_category.menu_toolbar"),
	CODE_AREA("action_category.code_area"),
	PLUGIN_SCRIPT("action_category.plugin_script");

	public final String nameRes;

	ActionCategory(String nameRes) {
		this.nameRes = nameRes;
	}
}
