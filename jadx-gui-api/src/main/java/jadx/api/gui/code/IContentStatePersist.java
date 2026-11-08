package jadx.api.gui.code;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.tree.ITreeNode;

/**
 * Adapter interface to allow save/load state of opened content panels
 */
public interface IContentStatePersist {

	Class<? extends ITreeNode> getNodeClass();

	String save(ITreeNode node);

	@Nullable
	ITreeNode load(String stateStr);
}
