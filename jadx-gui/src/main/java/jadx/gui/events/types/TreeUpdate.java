package jadx.gui.events.types;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.gui.events.JadxGuiEvents;
import jadx.gui.treemodel.JRoot;

public class TreeUpdate implements IJadxEvent {

	private final JRoot jRoot;

	public TreeUpdate(JRoot jRoot) {
		this.jRoot = jRoot;
	}

	public JRoot getJRoot() {
		return jRoot;
	}

	@Override
	public JadxEventType<TreeUpdate> getType() {
		return JadxGuiEvents.TREE_UPDATE;
	}
}
