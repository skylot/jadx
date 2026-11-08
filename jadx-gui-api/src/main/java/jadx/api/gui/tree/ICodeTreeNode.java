package jadx.api.gui.tree;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeNodeRef;

public interface ICodeTreeNode extends ITreeNode {

	/**
	 * Related code node reference.
	 */
	ICodeNodeRef getCodeNodeRef();

	/**
	 * Get node code
	 */
	ICodeInfo getCodeInfo();
}
