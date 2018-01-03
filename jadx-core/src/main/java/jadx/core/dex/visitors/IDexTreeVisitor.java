package jadx.core.dex.visitors;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

/**
 * Visitor interface for traverse dex tree
 */
public interface IDexTreeVisitor {

	/**
	 * Called after loading dex tree, but before visitor traversal.
	 */
	void init(RootNode root) throws JadxException;

	/**
	 * Visit class
	 *
	 * @return false for disable child methods and inner classes traversal
	 */
	boolean visit(ClassNode cls) throws JadxException;

	/**
	 * Visit method
	 */
	void visit(MethodNode mth) throws JadxException;
}
