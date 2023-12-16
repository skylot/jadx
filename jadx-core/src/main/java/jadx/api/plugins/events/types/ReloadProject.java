package jadx.api.plugins.events.types;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

public class ReloadProject implements IJadxEvent {

	public static final ReloadProject EVENT = new ReloadProject();

	private ReloadProject() {
		// singleton
	}

	@Override
	public JadxEventType<ReloadProject> getType() {
		return JadxEvents.RELOAD_PROJECT;
	}

	@Override
	public String toString() {
		return "RELOAD_PROJECT";
	}
}
