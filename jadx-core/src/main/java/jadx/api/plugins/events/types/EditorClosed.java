package jadx.api.plugins.events.types;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

/**
 * Code tab closed.
 */
public class EditorClosed implements IJadxEvent {
	private final ICodeNodeRef node;

	public EditorClosed(ICodeNodeRef node) {
		this.node = node;
	}

	public ICodeNodeRef getNode() {
		return node;
	}

	@Override
	public JadxEventType<EditorClosed> getType() {
		return JadxEvents.EDITOR_CLOSED;
	}

	@Override
	public String toString() {
		return "EditorClosed{" + "node=" + node + '}';
	}
}
