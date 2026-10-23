package jadx.api.gui;

import jadx.api.gui.dock.events.DockableHidden;
import jadx.api.gui.dock.events.DockableShown;
import jadx.api.gui.editor.events.EditorClosed;
import jadx.api.gui.editor.events.EditorOpened;
import jadx.api.gui.editor.events.EditorSelected;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.api.plugins.events.types.ReloadProject;
import jadx.api.plugins.events.types.ReloadSettingsWindow;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiSettings;

import static jadx.api.plugins.events.JadxEventType.create;

public class JadxGuiEvents {

	/**
	 * Notify about renaming done by user (GUI only).
	 * NOTE: instance from JadxEvents used for compatibility reasons
	 */
	public static final JadxEventType<NodeRenamedByUser> NODE_RENAMED_BY_USER = JadxEvents.NODE_RENAMED_BY_USER;

	/**
	 * Request reload of a current project (GUI only).
	 * NOTE: instance from JadxEvents used for compatibility reasons
	 */
	public static final JadxEventType<ReloadProject> RELOAD_PROJECT = JadxEvents.RELOAD_PROJECT;

	/**
	 * Request reload of a settings window (GUI only).
	 * Useful for a reload custom settings group which was set with
	 * {@link JadxGuiSettings#setCustomSettingsGroup(ISettingsGroup)}.
	 * NOTE: instance from JadxEvents used for compatibility reasons
	 */
	public static final JadxEventType<ReloadSettingsWindow> RELOAD_SETTINGS_WINDOW = JadxEvents.RELOAD_SETTINGS_WINDOW;

	/**
	 * Notify about plugin dockable panel shown.
	 */
	public static final JadxEventType<DockableShown> DOCKABLE_SHOWN = create("DOCKABLE_SHOWN");

	/**
	 * Notify about plugin dockable panel hidden.
	 */
	public static final JadxEventType<DockableHidden> DOCKABLE_HIDDEN = create("DOCKABLE_HIDDEN");

	/**
	 * Notify about code tab opened.
	 */
	public static final JadxEventType<EditorOpened> EDITOR_OPENED = create("EDITOR_OPENED");

	/**
	 * Notify about active code tab changed.
	 */
	public static final JadxEventType<EditorSelected> EDITOR_SELECTED = create("EDITOR_SELECTED");

	/**
	 * Notify about code tab closed.
	 */
	public static final JadxEventType<EditorClosed> EDITOR_CLOSED = create("EDITOR_CLOSED");
}
