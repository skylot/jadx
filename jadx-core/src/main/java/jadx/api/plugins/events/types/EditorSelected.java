package jadx.api.plugins.events.types;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

/**
 * Active code tab changed, node is null if no code tab active.
 */
public class EditorSelected implements IJadxEvent {
	private final @Nullable ICodeNodeRef node;

	public EditorSelected(@Nullable ICodeNodeRef node) {
		this.node = node;
	}

	public @Nullable ICodeNodeRef getNode() {
		return node;
	}

	@Override
	public JadxEventType<EditorSelected> getType() {
		return JadxEvents.EDITOR_SELECTED;
	}

	@Override
	public String toString() {
		return "EditorSelected{" + "node=" + node + '}';
	}
}
