package jadx.gui.utils;

import javax.swing.ImageIcon;

import static jadx.gui.utils.UiUtils.openSvgIcon;

public class Icons {

	public static final ImageIcon OPEN = openSvgIcon("ui/openDisk");
	public static final ImageIcon OPEN_PROJECT = openSvgIcon("ui/projectDirectory");
	public static final ImageIcon NEW_PROJECT = openSvgIcon("ui/newFolder");

	public static final ImageIcon CLOSE = openSvgIcon("ui/closeHovered");
	public static final ImageIcon CLOSE_INACTIVE = openSvgIcon("ui/close");

	public static final ImageIcon SAVE_ALL = UiUtils.openSvgIcon("ui/menu-saveall");

	public static final ImageIcon FLAT_PKG = UiUtils.openSvgIcon("ui/moduleGroup");
	public static final ImageIcon QUICK_TABS = UiUtils.openSvgIcon("ui/dataView");

	public static final ImageIcon PIN = UiUtils.openSvgIcon("nodes/pin");
	public static final ImageIcon PIN_DARK = UiUtils.openSvgIcon("nodes/pin_dark");
	public static final ImageIcon PIN_HOVERED = UiUtils.openSvgIcon("nodes/pinHovered");
	public static final ImageIcon PIN_HOVERED_DARK = UiUtils.openSvgIcon("nodes/pinHovered_dark");
	public static final ImageIcon BOOKMARK = UiUtils.openSvgIcon("nodes/bookmark");
	public static final ImageIcon BOOKMARK_OVERLAY = UiUtils.openSvgIcon("nodes/bookmark_overlay");
	public static final ImageIcon BOOKMARK_DARK = UiUtils.openSvgIcon("nodes/bookmark_dark");
	public static final ImageIcon BOOKMARK_OVERLAY_DARK = UiUtils.openSvgIcon("nodes/bookmark_overlay_dark");

	public static final ImageIcon STATIC = openSvgIcon("nodes/staticMark");
	public static final ImageIcon FINAL = openSvgIcon("nodes/finalMark");

	public static final ImageIcon START_PAGE = openSvgIcon("nodes/newWindow");

	public static final ImageIcon FOLDER = UiUtils.openSvgIcon("nodes/folder");
	public static final ImageIcon FILE = UiUtils.openSvgIcon("nodes/file_any_type");

	public static final ImageIcon PACKAGE = UiUtils.openSvgIcon("nodes/package");
	public static final ImageIcon CLASS = UiUtils.openSvgIcon("nodes/class");
	public static final ImageIcon METHOD = UiUtils.openSvgIcon("nodes/method");
	public static final ImageIcon FIELD = UiUtils.openSvgIcon("nodes/field");
	public static final ImageIcon PROPERTY = UiUtils.openSvgIcon("nodes/property");
	public static final ImageIcon PARAMETER = UiUtils.openSvgIcon("nodes/parameter");

	public static final ImageIcon RUN = UiUtils.openSvgIcon("ui/run");
	public static final ImageIcon CHECK = UiUtils.openSvgIcon("ui/checkConstraint");
	public static final ImageIcon FORMAT = UiUtils.openSvgIcon("ui/toolWindowMessages");
	public static final ImageIcon RESET = UiUtils.openSvgIcon("ui/reset");

	public static final ImageIcon FONT = UiUtils.openSvgIcon("nodes/fontFile");
	public static final ImageIcon ICON_MARK = UiUtils.openSvgIcon("search/mark");
	public static final ImageIcon ICON_MARK_SELECTED = UiUtils.openSvgIcon("search/previewSelected");
	public static final ImageIcon ICON_REGEX = UiUtils.openSvgIcon("search/regexHovered");
	public static final ImageIcon ICON_REGEX_SELECTED = UiUtils.openSvgIcon("search/regexSelected");
	public static final ImageIcon ICON_WORDS = UiUtils.openSvgIcon("search/wordsHovered");
	public static final ImageIcon ICON_WORDS_SELECTED = UiUtils.openSvgIcon("search/wordsSelected");
	public static final ImageIcon ICON_MATCH = UiUtils.openSvgIcon("search/matchCaseHovered");
	public static final ImageIcon ICON_MATCH_SELECTED = UiUtils.openSvgIcon("search/matchCaseSelected");
	public static final ImageIcon ICON_UP = UiUtils.openSvgIcon("ui/top");
	public static final ImageIcon ICON_DOWN = UiUtils.openSvgIcon("ui/bottom");
	public static final ImageIcon ICON_CLOSE = UiUtils.openSvgIcon("ui/close");
	public static final ImageIcon ICON_FIND_TYPE_TXT = UiUtils.openSvgIcon("search/text");
	public static final ImageIcon ICON_FIND_TYPE_HEX = UiUtils.openSvgIcon("search/hexSerial");
	public static final ImageIcon ICON_ACTIVE_TAB = UiUtils.openSvgIcon("search/activeTab");
	public static final ImageIcon ICON_ACTIVE_TAB_SELECTED = UiUtils.openSvgIcon("search/activeTabSelected");
}
