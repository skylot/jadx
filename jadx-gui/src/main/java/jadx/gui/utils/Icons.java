package jadx.gui.utils;

import javax.swing.ImageIcon;

import static jadx.gui.utils.UiUtils.openSvgIcon;

public class Icons {

	public static final ImageIcon OPEN = openSvgIcon("ui/openDisk");
	public static final ImageIcon OPEN_PROJECT = openSvgIcon("ui/projectDirectory");
	public static final ImageIcon NEW_PROJECT = openSvgIcon("ui/newFolder");

	public static final ImageIcon CLOSE = openSvgIcon("ui/closeHovered");
	public static final ImageIcon CLOSE_INACTIVE = openSvgIcon("ui/close");

	public static final ImageIcon STATIC = openSvgIcon("nodes/staticMark");
	public static final ImageIcon FINAL = openSvgIcon("nodes/finalMark");

	public static final ImageIcon START_PAGE = openSvgIcon("nodes/newWindow");

	public static final ImageIcon FOLDER = UiUtils.openSvgIcon("nodes/folder");
	public static final ImageIcon FILE = UiUtils.openSvgIcon("nodes/file_any_type");
}
