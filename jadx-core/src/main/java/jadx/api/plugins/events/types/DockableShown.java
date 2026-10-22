package jadx.api.plugins.events.types;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

/**
 * Plugin dockable panel shown.
 */
public class DockableShown implements IJadxEvent {
	private final String dockableId;

	public DockableShown(String dockableId) {
		this.dockableId = dockableId;
	}

	public String getDockableId() {
		return dockableId;
	}

	@Override
	public JadxEventType<DockableShown> getType() {
		return JadxEvents.DOCKABLE_SHOWN;
	}

	@Override
	public String toString() {
		return "DockableShown{" + "dockableId=" + dockableId + '}';
	}
}
