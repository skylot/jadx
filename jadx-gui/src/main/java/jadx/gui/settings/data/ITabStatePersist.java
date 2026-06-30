package jadx.gui.settings.data;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;

/**
 * Adapter interface to allow save/load state of opened tabs
 */
public interface ITabStatePersist {

	Class<? extends JNode> getNodeClass();

	String save(JNode node);

	@Nullable
	JNode load(String stateStr);
}
