package jadx.gui.ui.menu;

public enum ActionCategory {
	MENU_TOOLBAR("action_category.menu_toolbar"),
	CODE_AREA("action_category.code_area"),
	;

	public final String nameRes;

	ActionCategory(String nameRes) {
		this.nameRes = nameRes;
	}
}
