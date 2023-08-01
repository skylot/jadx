package jadx.gui.ui.menu;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;

public enum ActionModel {
	ABOUT("menu.about", "menu.about", "ui/showInfos",
			Shortcut.keyboard(KeyEvent.VK_F1)),
	OPEN("file.open_action", "file.open_action", "ui/openDisk",
			Shortcut.keyboard(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)),
	OPEN_PROJECT("file.open_project", "file.open_project", "ui/projectDirectory",
			Shortcut.keyboard(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK | UiUtils.ctrlButton())),
	ADD_FILES("file.add_files_action", "file.add_files_action", "ui/addFile",
			null),
	NEW_PROJECT("file.new_project", "file.new_project", "ui/newFolder",
			null),
	SAVE_PROJECT("file.save_project", "file.save_project", null,
			null),
	SAVE_PROJECT_AS("file.save_project_as", "file.save_project_as", null,
			null),
	RELOAD("file.reload", "file.reload", "ui/refresh",
			Shortcut.keyboard(KeyEvent.VK_F5)),
	LIVE_RELOAD("file.live_reload", "file.live_reload_desc", null,
			Shortcut.keyboard(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK)),
	SAVE_ALL("file.save_all", "file.save_all", "ui/menu-saveall",
			Shortcut.keyboard(KeyEvent.VK_E, UiUtils.ctrlButton())),
	EXPORT("file.export_gradle", "file.export_gradle", "ui/export",
			Shortcut.keyboard(KeyEvent.VK_E, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	PREFS("menu.preferences", "menu.preferences", "ui/settings",
			Shortcut.keyboard(KeyEvent.VK_P, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	EXIT("file.exit", "file.exit", "ui/exit",
			null),
	SYNC("menu.sync", "menu.sync", "ui/pagination",
			Shortcut.keyboard(KeyEvent.VK_T, UiUtils.ctrlButton())),
	TEXT_SEARCH("menu.text_search", "menu.text_search", "ui/find",
			Shortcut.keyboard(KeyEvent.VK_F, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	CLASS_SEARCH("menu.class_search", "menu.class_search", "ui/ejbFinderMethod",
			Shortcut.keyboard(KeyEvent.VK_N, UiUtils.ctrlButton())),
	COMMENT_SEARCH("menu.comment_search", "menu.comment_search", "ui/usagesFinder",
			Shortcut.keyboard(KeyEvent.VK_SEMICOLON, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	GOTO_MAIN_ACTIVITY("menu.goto_main_activity", "menu.goto_main_activity", "ui/home",
			Shortcut.keyboard(KeyEvent.VK_M, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	DECOMPILE_ALL("menu.decompile_all", "menu.decompile_all", "ui/runAll",
			null),
	RESET_CACHE("menu.reset_cache", "menu.reset_cache", "ui/reset",
			null),
	DEOBF("menu.deobfuscation", "preferences.deobfuscation", "ui/helmChartLock",
			Shortcut.keyboard(KeyEvent.VK_D, UiUtils.ctrlButton() | KeyEvent.ALT_DOWN_MASK)),
	SHOW_LOG("menu.log", "menu.log", "ui/logVerbose",
			Shortcut.keyboard(KeyEvent.VK_L, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	BACK("nav.back", "nav.back", "ui/left",
			Shortcut.keyboard(KeyEvent.VK_ESCAPE)),
	FORWARD("nav.forward", "nav.forward", "ui/right",
			Shortcut.keyboard(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK)),
	QUARK("menu.quark", "menu.quark", "ui/quark",
			null),
	OPEN_DEVICE("debugger.process_selector", "debugger.process_selector", "ui/startDebugger",
			null),
			;

	public final String nameRes;
	public final String descRes;
	public final String iconPath;
	public final Shortcut defaultShortcut;

	ActionModel(String nameRes, String descRes, String iconPath, Shortcut defaultShortcut) {
		this.nameRes = nameRes;
		this.descRes = descRes;
		this.iconPath = iconPath;
		this.defaultShortcut = defaultShortcut;
	}
}
