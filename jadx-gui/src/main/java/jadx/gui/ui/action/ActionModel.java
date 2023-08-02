package jadx.gui.ui.action;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;

import static jadx.gui.ui.action.ActionCategory.*;

public enum ActionModel {
	ABOUT(MENU_TOOLBAR, "menu.about", "menu.about", "ui/showInfos",
			Shortcut.keyboard(KeyEvent.VK_F1)),
	OPEN(MENU_TOOLBAR, "file.open_action", "file.open_action", "ui/openDisk",
			Shortcut.keyboard(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)),
	OPEN_PROJECT(MENU_TOOLBAR, "file.open_project", "file.open_project", "ui/projectDirectory",
			Shortcut.keyboard(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK | UiUtils.ctrlButton())),
	ADD_FILES(MENU_TOOLBAR, "file.add_files_action", "file.add_files_action", "ui/addFile",
			null),
	NEW_PROJECT(MENU_TOOLBAR, "file.new_project", "file.new_project", "ui/newFolder",
			null),
	SAVE_PROJECT(MENU_TOOLBAR, "file.save_project", "file.save_project", null,
			null),
	SAVE_PROJECT_AS(MENU_TOOLBAR, "file.save_project_as", "file.save_project_as", null,
			null),
	RELOAD(MENU_TOOLBAR, "file.reload", "file.reload", "ui/refresh",
			Shortcut.keyboard(KeyEvent.VK_F5)),
	LIVE_RELOAD(MENU_TOOLBAR, "file.live_reload", "file.live_reload_desc", null,
			Shortcut.keyboard(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK)),
	SAVE_ALL(MENU_TOOLBAR, "file.save_all", "file.save_all", "ui/menu-saveall",
			Shortcut.keyboard(KeyEvent.VK_E, UiUtils.ctrlButton())),
	EXPORT(MENU_TOOLBAR, "file.export_gradle", "file.export_gradle", "ui/export",
			Shortcut.keyboard(KeyEvent.VK_E, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	PREFS(MENU_TOOLBAR, "menu.preferences", "menu.preferences", "ui/settings",
			Shortcut.keyboard(KeyEvent.VK_P, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	EXIT(MENU_TOOLBAR, "file.exit", "file.exit", "ui/exit",
			null),
	SYNC(MENU_TOOLBAR, "menu.sync", "menu.sync", "ui/pagination",
			Shortcut.keyboard(KeyEvent.VK_T, UiUtils.ctrlButton())),
	TEXT_SEARCH(MENU_TOOLBAR, "menu.text_search", "menu.text_search", "ui/find",
			Shortcut.keyboard(KeyEvent.VK_F, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	CLASS_SEARCH(MENU_TOOLBAR, "menu.class_search", "menu.class_search", "ui/ejbFinderMethod",
			Shortcut.keyboard(KeyEvent.VK_N, UiUtils.ctrlButton())),
	COMMENT_SEARCH(MENU_TOOLBAR, "menu.comment_search", "menu.comment_search", "ui/usagesFinder",
			Shortcut.keyboard(KeyEvent.VK_SEMICOLON, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	GOTO_MAIN_ACTIVITY(MENU_TOOLBAR, "menu.goto_main_activity", "menu.goto_main_activity", "ui/home",
			Shortcut.keyboard(KeyEvent.VK_M, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	DECOMPILE_ALL(MENU_TOOLBAR, "menu.decompile_all", "menu.decompile_all", "ui/runAll",
			null),
	RESET_CACHE(MENU_TOOLBAR, "menu.reset_cache", "menu.reset_cache", "ui/reset",
			null),
	DEOBF(MENU_TOOLBAR, "menu.deobfuscation", "preferences.deobfuscation", "ui/helmChartLock",
			Shortcut.keyboard(KeyEvent.VK_D, UiUtils.ctrlButton() | KeyEvent.ALT_DOWN_MASK)),
	SHOW_LOG(MENU_TOOLBAR, "menu.log", "menu.log", "ui/logVerbose",
			Shortcut.keyboard(KeyEvent.VK_L, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK)),
	BACK(MENU_TOOLBAR, "nav.back", "nav.back", "ui/left",
			Shortcut.keyboard(KeyEvent.VK_ESCAPE)),
	FORWARD(MENU_TOOLBAR, "nav.forward", "nav.forward", "ui/right",
			Shortcut.keyboard(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK)),
	QUARK(MENU_TOOLBAR, "menu.quark", "menu.quark", "ui/quark",
			null),
	OPEN_DEVICE(MENU_TOOLBAR, "debugger.process_selector", "debugger.process_selector", "ui/startDebugger",
			null),

	FIND_USAGE(CODE_AREA, "popup.find_usage", "popup.find_usage", null,
			Shortcut.keyboard(KeyEvent.VK_X)),
	GOTO_DECLARATION(CODE_AREA, "popup.go_to_declaration", "popup.go_to_declaration", null,
			Shortcut.keyboard(KeyEvent.VK_D)),
	CODE_COMMENT(CODE_AREA, "popup.add_comment", "popup.add_comment", null,
			Shortcut.keyboard(KeyEvent.VK_SEMICOLON)),
	CODE_COMMENT_SEARCH(CODE_AREA, "popup.search_comment", "popup.search_comment", null,
			Shortcut.keyboard(KeyEvent.VK_SEMICOLON, UiUtils.ctrlButton())),
	CODE_RENAME(CODE_AREA, "popup.rename", "popup.rename", null,
			Shortcut.keyboard(KeyEvent.VK_N)),
	FRIDA_COPY(CODE_AREA, "popup.frida", "popup.frida", null,
			Shortcut.keyboard(KeyEvent.VK_F)),
	XPOSED_COPY(CODE_AREA, "popup.xposed", "popup.xposed", null,
			Shortcut.keyboard(KeyEvent.VK_Y)),
	JSON_PRETTIFY(CODE_AREA, "popup.json_prettify", "popup.json_prettify", null,
			null),

	SCRIPT_RUN(PLUGIN_SCRIPT, "script.run", "script.run", "ui/run",
			Shortcut.keyboard(KeyEvent.VK_F8)),
	SCRIPT_SAVE(PLUGIN_SCRIPT, "script.save", "script.save", "ui/menu-saveall",
			Shortcut.keyboard(KeyEvent.VK_S, UiUtils.ctrlButton())),
	SCRIPT_AUTO_COMPLETE(PLUGIN_SCRIPT, "script.auto_complete", "script.auto_complete", null,
			Shortcut.keyboard(KeyEvent.VK_SPACE, UiUtils.ctrlButton()));

	public final ActionCategory category;
	public final String nameRes;
	public final String descRes;
	public final String iconPath;
	public final Shortcut defaultShortcut;

	ActionModel(ActionCategory category, String nameRes, String descRes, String iconPath, Shortcut defaultShortcut) {
		this.category = category;
		this.nameRes = nameRes;
		this.descRes = descRes;
		this.iconPath = iconPath;
		this.defaultShortcut = defaultShortcut;
	}

	public static List<ActionModel> select(ActionCategory category) {
		return Arrays.stream(values())
				.filter(actionModel -> actionModel.category == category)
				.collect(Collectors.toUnmodifiableList());
	}
}
