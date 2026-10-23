package jadx.api.gui.editor.events;

import jadx.api.gui.JadxGuiEvents;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;

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
		return JadxGuiEvents.EDITOR_OPENED;
	}

	@Override
	public String toString() {
		return "EditorOpened{" + "node=" + node + '}';
	}
}
