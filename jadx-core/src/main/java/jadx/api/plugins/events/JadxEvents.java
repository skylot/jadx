package jadx.api.plugins.events;

import jadx.api.plugins.events.types.NodeRenamedByUser;

import static jadx.api.plugins.events.JadxEventType.create;

/**
 * Typed and extendable enumeration of event types
 */
public class JadxEvents {

	/**
	 * Notify about renames done by user (GUI only).
	 */
	public static final JadxEventType<NodeRenamedByUser> NODE_RENAMED_BY_USER = create();
}
