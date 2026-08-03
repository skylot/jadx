package jadx.gui.ui.action;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;

import org.jetbrains.annotations.Nullable;

import jadx.core.utils.Utils;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;

import static jadx.gui.ui.action.ActionCategory.CODE_AREA;
import static jadx.gui.ui.action.ActionCategory.HEX_VIEWER_MENU;
import static jadx.gui.ui.action.ActionCategory.MENU_TOOLBAR;
import static jadx.gui.ui.action.ActionCategory.PLUGIN_SCRIPT;
import static jadx.gui.utils.UiUtils.ctrlButton;
import static jadx.gui.utils.shortcut.Shortcut.keyboard;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

public enum ActionModel {
	ABOUT(MENU_TOOLBAR, NLS.str("menu.about"), null, "ui/showInfos", keyboard(KeyEvent.VK_F1)),
	OPEN(MENU_TOOLBAR, NLS.str("file.open_action"), null, "ui/openDisk", keyboard(KeyEvent.VK_O, ctrlButton())),
	OPEN_PROJECT(MENU_TOOLBAR, NLS.str("file.open_project"), null, "ui/projectDirectory",
			keyboard(KeyEvent.VK_O, SHIFT_DOWN_MASK | ctrlButton())),
	ADD_FILES(MENU_TOOLBAR, NLS.str("file.add_files_action"), null, "ui/addFile", null),
	NEW_PROJECT(MENU_TOOLBAR, NLS.str("file.new_project"), null, "ui/newFolder", null),
	SAVE_PROJECT(MENU_TOOLBAR, NLS.str("file.save_project"), null, null, null),
	SAVE_PROJECT_AS(MENU_TOOLBAR, NLS.str("file.save_project_as"), null, null, null),
	RELOAD(MENU_TOOLBAR, NLS.str("file.reload"), null, "ui/refresh", keyboard(KeyEvent.VK_F5)),
	LIVE_RELOAD(MENU_TOOLBAR, NLS.str("file.live_reload"), NLS.str("file.live_reload_desc"), null,
			keyboard(KeyEvent.VK_F5, SHIFT_DOWN_MASK)),
	SAVE_ALL(MENU_TOOLBAR, NLS.str("file.save_all"), null, "ui/menu-saveall", keyboard(KeyEvent.VK_E, ctrlButton())),
	EXPORT(MENU_TOOLBAR, NLS.str("file.export"), null, "ui/export", keyboard(KeyEvent.VK_E, ctrlButton() | SHIFT_DOWN_MASK)),
	PREFS(MENU_TOOLBAR, NLS.str("menu.preferences"), null, "ui/settings", keyboard(KeyEvent.VK_P, ctrlButton() | SHIFT_DOWN_MASK)),
	EXIT(MENU_TOOLBAR, NLS.str("file.exit"), null, "ui/exit", null),
	SYNC(MENU_TOOLBAR, NLS.str("menu.sync"), null, "ui/locate", keyboard(KeyEvent.VK_T, ctrlButton())),
	TEXT_SEARCH(MENU_TOOLBAR, NLS.str("menu.text_search"), null, "ui/find", keyboard(KeyEvent.VK_F, ctrlButton() | SHIFT_DOWN_MASK)),
	STRINGS(MENU_TOOLBAR, NLS.str("menu.strings"), null, "ui/strings", Shortcut.none()),
	CLASS_SEARCH(MENU_TOOLBAR, NLS.str("menu.class_search"), null, "ui/ejbFinderMethod", keyboard(KeyEvent.VK_N, ctrlButton())),
	COMMENT_SEARCH(MENU_TOOLBAR, NLS.str("menu.comment_search"), null, "ui/usagesFinder",
			keyboard(KeyEvent.VK_SEMICOLON, ctrlButton() | SHIFT_DOWN_MASK)),
	GO_TO_MAIN_ACTIVITY(MENU_TOOLBAR, NLS.str("menu.go_to_main_activity"), null, "ui/home",
			keyboard(KeyEvent.VK_M, ctrlButton() | SHIFT_DOWN_MASK)),
	GO_TO_APPLICATION(MENU_TOOLBAR, NLS.str("menu.go_to_application"), null, "ui/application",
			keyboard(KeyEvent.VK_A, ctrlButton() | SHIFT_DOWN_MASK)),
	GO_TO_ANDROID_MANIFEST(MENU_TOOLBAR, NLS.str("menu.go_to_android_manifest"), null, "ui/androidManifest", null),
	PREVIEW_TAB(MENU_TOOLBAR, NLS.str("menu.enable_preview_tab"), null, "ui/editorPreview", null),
	DECOMPILE_ALL(MENU_TOOLBAR, NLS.str("menu.decompile_all"), null, "ui/runAll", null),
	RESET_CACHE(MENU_TOOLBAR, NLS.str("menu.reset_cache"), null, "ui/reset", null),
	DEOBF(MENU_TOOLBAR, NLS.str("menu.deobfuscation"), null, "ui/helmChartLock", keyboard(KeyEvent.VK_D, ctrlButton() | ALT_DOWN_MASK)),
	SHOW_LOG(MENU_TOOLBAR, NLS.str("menu.log"), null, "ui/logVerbose", keyboard(KeyEvent.VK_L, ctrlButton() | SHIFT_DOWN_MASK)),
	CREATE_DESKTOP_ENTRY(MENU_TOOLBAR, NLS.str("menu.create_desktop_entry"), null, null, null),
	BACK(MENU_TOOLBAR, NLS.str("nav.back"), null, "ui/left", keyboard(KeyEvent.VK_ESCAPE)),
	BACK_V(MENU_TOOLBAR, NLS.str("action.variant", NLS.str("nav.back")), null, "ui/left", null),
	FORWARD(MENU_TOOLBAR, NLS.str("nav.forward"), null, "ui/right", keyboard(KeyEvent.VK_RIGHT, ALT_DOWN_MASK)),
	FORWARD_V(MENU_TOOLBAR, NLS.str("action.variant", NLS.str("nav.forward")), null, "ui/right", null),
	QUARK(MENU_TOOLBAR, NLS.str("menu.quark"), null, "ui/quark", null),
	OPEN_DEVICE(MENU_TOOLBAR, NLS.str("debugger.process_selector"), null, "ui/startDebugger", null),

	FIND_USAGE(CODE_AREA, NLS.str("popup.find_usage"), null, null, keyboard(KeyEvent.VK_X)),
	FIND_USAGE_PLUS(CODE_AREA, NLS.str("popup.usage_dialog_plus"), null, null, keyboard(KeyEvent.VK_C)),
	GOTO_DECLARATION(CODE_AREA, NLS.str("popup.go_to_declaration"), null, null, keyboard(KeyEvent.VK_D)),
	CONVERT_NUMBER(CODE_AREA, NLS.str("popup.convert_number"), null, null, null),
	VIEW_CLASS_INHERITANCE_GRAPH(CODE_AREA, NLS.str("popup.view_class_graph"), NLS.str("popup.view_class_graph_description"), null, null),
	VIEW_CLASS_METHOD_GRAPH(CODE_AREA, NLS.str("popup.view_class_method_graph"), NLS.str("popup.view_class_method_graph_description"), null,
			null),
	VIEW_CALL_GRAPH(CODE_AREA, NLS.str("popup.view_call_graph"), NLS.str("popup.view_call_graph_description"), null, null),
	VIEW_CONTROL_FLOW_GRAPH(CODE_AREA, NLS.str("popup.view_cfg"), null, null, null),

	CODE_COMMENT(CODE_AREA, NLS.str("popup.add_comment"), null, null, keyboard(KeyEvent.VK_SEMICOLON)),
	CODE_COMMENT_SEARCH(CODE_AREA, NLS.str("popup.search_comment"), null, null, keyboard(KeyEvent.VK_SEMICOLON, ctrlButton())),
	CODE_RENAME(CODE_AREA, NLS.str("popup.rename"), null, null, keyboard(KeyEvent.VK_N)),
	FRIDA_COPY(CODE_AREA, NLS.str("popup.frida"), null, null, keyboard(KeyEvent.VK_F)),
	XPOSED_COPY(CODE_AREA, NLS.str("popup.xposed"), null, null, keyboard(KeyEvent.VK_Y)),
	COPY_REFERENCE(CODE_AREA, NLS.str("popup.copy_reference"), null, null, keyboard(KeyEvent.VK_R)),
	JSON_PRETTIFY(CODE_AREA, NLS.str("popup.json_prettify"), null, null, null),

	SCRIPT_RUN(PLUGIN_SCRIPT, NLS.str("script.run"), null, "ui/run", keyboard(KeyEvent.VK_F8)),
	SCRIPT_SAVE(PLUGIN_SCRIPT, NLS.str("script.save"), null, "ui/menu-saveall", keyboard(KeyEvent.VK_S, ctrlButton())),
	SCRIPT_AUTO_COMPLETE(PLUGIN_SCRIPT, NLS.str("script.auto_complete"), null, null, keyboard(KeyEvent.VK_SPACE, ctrlButton())),

	HEX_VIEWER_SHOW_INSPECTOR(HEX_VIEWER_MENU, NLS.str("hex_viewer.show_inspector"), null, null, null),
	HEX_VIEWER_CHANGE_ENCODING(HEX_VIEWER_MENU, NLS.str("hex_viewer.change_encoding"), null, null, null),
	HEX_VIEWER_GO_TO_ADDRESS(HEX_VIEWER_MENU, NLS.str("hex_viewer.goto_address"), null, null, keyboard(KeyEvent.VK_J, ctrlButton())),
	HEX_VIEWER_FIND(HEX_VIEWER_MENU, NLS.str("hex_viewer.find"), null, null, keyboard(KeyEvent.VK_F, ctrlButton()));

	private final ActionCategory category;
	private final String name;
	private final String desc;
	private final @Nullable ImageIcon icon;
	private final Shortcut defaultShortcut;

	ActionModel(ActionCategory category, String name, @Nullable String desc, @Nullable String iconPath,
			@Nullable Shortcut defaultShortcut) {
		this.category = Objects.requireNonNull(category);
		this.name = Objects.requireNonNull(name);
		this.desc = Utils.getOrElse(desc, name);
		this.icon = iconPath != null ? UiUtils.openSvgIcon(iconPath) : null;
		this.defaultShortcut = defaultShortcut != null ? defaultShortcut : Shortcut.none();
	}

	public static List<ActionModel> select(ActionCategory category) {
		return Arrays.stream(values())
				.filter(actionModel -> actionModel.category == category)
				.collect(Collectors.toUnmodifiableList());
	}

	public ActionCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return desc;
	}

	public @Nullable ImageIcon getIcon() {
		return icon;
	}

	public Shortcut getDefaultShortcut() {
		return defaultShortcut;
	}
}
