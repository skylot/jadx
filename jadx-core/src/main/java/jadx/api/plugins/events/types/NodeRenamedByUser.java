package jadx.api.plugins.events.types;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;
import jadx.api.plugins.events.JadxEvents;

public class NodeRenamedByUser implements IJadxEvent {

	private final ICodeNodeRef node;
	private final String oldName;
	private final String newName;

	public NodeRenamedByUser(ICodeNodeRef node, String oldName, String newName) {
		this.node = node;
		this.oldName = oldName;
		this.newName = newName;
	}

	public ICodeNodeRef getNode() {
		return node;
	}

	public String getOldName() {
		return oldName;
	}

	public String getNewName() {
		return newName;
	}

	@Override
	public JadxEventType<NodeRenamedByUser> getType() {
		return JadxEvents.NODE_RENAMED_BY_USER;
	}

	@Override
	public String toString() {
		return "NodeRenamedByUser{" + node + ", '" + oldName + "' -> '" + newName + "'}";
	}
}
