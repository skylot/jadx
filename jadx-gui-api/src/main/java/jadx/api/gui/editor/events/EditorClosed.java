package jadx.api.gui.editor.events;

import jadx.api.gui.JadxGuiEvents;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;

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
		return JadxGuiEvents.EDITOR_CLOSED;
	}

	@Override
	public String toString() {
		return "EditorClosed{" + "node=" + node + '}';
	}
}
