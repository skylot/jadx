package jadx.api.plugins.events.types;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

public class ReloadSettingsWindow implements IJadxEvent {

	public static final ReloadSettingsWindow INSTANCE = new ReloadSettingsWindow();

	private ReloadSettingsWindow() {
		// singleton
	}

	@Override
	public JadxEventType<ReloadSettingsWindow> getType() {
		return JadxEvents.RELOAD_SETTINGS_WINDOW;
	}

	@Override
	public String toString() {
		return "RELOAD_SETTINGS_WINDOW";
	}
}
