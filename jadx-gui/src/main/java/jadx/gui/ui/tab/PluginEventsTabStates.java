package jadx.gui.ui.tab;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.types.EditorClosed;
import jadx.api.plugins.events.types.EditorOpened;
import jadx.api.plugins.events.types.EditorSelected;
import jadx.gui.treemodel.JNode;

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

	private static @Nullable ICodeNodeRef codeRef(JNode node) {
		return node.getCodeNodeRef();
	}
}
