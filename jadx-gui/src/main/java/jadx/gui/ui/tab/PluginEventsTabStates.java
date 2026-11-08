package jadx.gui.ui.tab;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.editor.events.EditorClosed;
import jadx.api.gui.editor.events.EditorOpened;
import jadx.api.gui.editor.events.EditorSelected;
import jadx.api.gui.tree.ICodeTreeNode;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvents;

/**
 * Forward code tab changes to plugins
 */
public class PluginEventsTabStates implements ITabStatesListener {
	private final IJadxEvents events;
	private @Nullable ICodeNodeRef selected;

	public PluginEventsTabStates(IJadxEvents events) {
		this.events = events;
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {
		ICodeNodeRef ref = codeRef(blueprint.getNode());
		if (ref != null) {
			events.send(new EditorOpened(ref));
		}
	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {
		select(codeRef(blueprint.getNode()));
	}

	@Override
	public void onTabDeselect() {
		select(null);
	}

	@Override
	public void onTabClose(TabBlueprint blueprint) {
		ICodeNodeRef ref = codeRef(blueprint.getNode());
		if (ref != null) {
			if (ref == selected) {
				select(null);
			}
			events.send(new EditorClosed(ref));
		}
	}

	private void select(@Nullable ICodeNodeRef ref) {
		if (ref != selected) {
			selected = ref;
			events.send(new EditorSelected(ref));
		}
	}

	private static @Nullable ICodeNodeRef codeRef(ITreeNode node) {
		if (node instanceof ICodeNodeRef) {
			return (ICodeNodeRef) node;
		}
		if (node instanceof ICodeTreeNode) {
			return ((ICodeTreeNode) node).getCodeNodeRef();
		}
		return null;
	}
}
