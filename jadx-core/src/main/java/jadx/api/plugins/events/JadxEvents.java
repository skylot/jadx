package jadx.api.plugins.events;

import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.api.plugins.events.types.ReloadSettingsWindow;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiSettings;

import static jadx.api.plugins.events.JadxEventType.create;

/**
 * Typed and extendable enumeration of event types
 */
public class JadxEvents {

	/**
	 * Notify about renames done by user (GUI only).
	 */
	public static final JadxEventType<NodeRenamedByUser> NODE_RENAMED_BY_USER = create();

	/**
	 * Request reload of settings window (GUI only).
	 * Useful for reload custom settings group set with
	 * {@link JadxGuiSettings#setCustomSettingsGroup(ISettingsGroup)}.
	 */
	public static final JadxEventType<ReloadSettingsWindow> RELOAD_SETTINGS_WINDOW = create();
}
