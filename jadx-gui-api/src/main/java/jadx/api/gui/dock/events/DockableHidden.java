package jadx.api.gui.dock.events;

import jadx.api.gui.JadxGuiEvents;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;

/**
 * Plugin dockable panel hidden.
 */
public class DockableHidden implements IJadxEvent {
	private final String dockableId;

	public DockableHidden(String dockableId) {
		this.dockableId = dockableId;
	}

	public String getDockableId() {
		return dockableId;
	}

	@Override
	public JadxEventType<DockableHidden> getType() {
		return JadxGuiEvents.DOCKABLE_HIDDEN;
	}

	@Override
	public String toString() {
		return "DockableHidden{" + "dockableId=" + dockableId + '}';
	}
}
