package jadx.api.plugins.events.types;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

/**
 * Code tab opened.
 */
public class EditorOpened implements IJadxEvent {
	private final ICodeNodeRef node;

	public EditorOpened(ICodeNodeRef node) {
		this.node = node;
	}

	public ICodeNodeRef getNode() {
		return node;
	}

	@Override
	public JadxEventType<EditorOpened> getType() {
		return JadxEvents.EDITOR_OPENED;
	}

	@Override
	public String toString() {
		return "EditorOpened{" + "node=" + node + '}';
	}
}
