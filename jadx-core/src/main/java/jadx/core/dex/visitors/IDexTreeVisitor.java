package jadx.core.dex.visitors;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

/**
 * Visitor interface for traverse dex tree
 */
public interface IDexTreeVisitor {

	/**
	 * Visit class
	 *
	 * @return false for disable child methods and inner classes traversal
	 * @throws JadxException
	 */
	boolean visit(ClassNode cls) throws JadxException;

	/**
	 * Visit method
	 *
	 * @throws JadxException
	 */
	void visit(MethodNode mth) throws JadxException;
}
