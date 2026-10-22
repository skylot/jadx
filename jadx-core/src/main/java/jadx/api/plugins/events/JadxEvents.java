package jadx.api.plugins.events;

import jadx.api.plugins.events.types.DockableHidden;
import jadx.api.plugins.events.types.DockableShown;
import jadx.api.plugins.events.types.EditorClosed;
import jadx.api.plugins.events.types.EditorOpened;
import jadx.api.plugins.events.types.EditorSelected;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.api.plugins.events.types.ReloadProject;
import jadx.api.plugins.events.types.ReloadSettingsWindow;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiSettings;

import static jadx.api.plugins.events.JadxEventType.create;

/**
 * Typed and extendable enumeration of event types
 */
public class JadxEvents {

	/**
	 * Notify about renaming done by user (GUI only).
	 */
	public static final JadxEventType<NodeRenamedByUser> NODE_RENAMED_BY_USER = create("NODE_RENAMED_BY_USER");

	/**
	 * Request reload of a current project (GUI only).
	 */
	public static final JadxEventType<ReloadProject> RELOAD_PROJECT = create("RELOAD_PROJECT");

	/**
	 * Request reload of a settings window (GUI only).
	 * Useful for a reload custom settings group which was set with
	 * {@link JadxGuiSettings#setCustomSettingsGroup(ISettingsGroup)}.
	 */
	public static final JadxEventType<ReloadSettingsWindow> RELOAD_SETTINGS_WINDOW = create("RELOAD_SETTINGS_WINDOW");

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
