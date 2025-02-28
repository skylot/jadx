package jadx.api.gui.tree;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeNodeRef;

public interface ITreeNode extends TreeNode {

	/**
	 * Locale independent node identifier
	 */
	String getID();

	/**
	 * Node title
	 */
	String getName();

	/**
	 * Node icon
	 */
	Icon getIcon();

	/**
	 * Related code node reference.
	 */
	@Nullable
	ICodeNodeRef getCodeNodeRef();
}
