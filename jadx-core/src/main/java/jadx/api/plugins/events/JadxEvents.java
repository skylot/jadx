package jadx.api.plugins.events;

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
}
