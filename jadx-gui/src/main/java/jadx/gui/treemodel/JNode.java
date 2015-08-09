package jadx.gui.treemodel;

import jadx.api.JavaNode;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public abstract class JNode extends DefaultMutableTreeNode {

	public abstract JClass getJParent();

	/**
	 * Return top level JClass or self if already at top.
	 */
	public JClass getRootClass() {
		return null;
	}

	public JavaNode getJavaNode() {
		return null;
	}

	public String getContent() {
		return null;
	}

	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE;
	}

	public int getLine() {
		return 0;
	}

	public Integer getSourceLine(int line) {
		return null;
	}

	public abstract Icon getIcon();

	public String getName() {
		JavaNode javaNode = getJavaNode();
		if (javaNode == null) {
			return null;
		}
		return javaNode.getName();
	}

	public abstract String makeString();

	public String makeDescString() {
		return null;
	}

	public boolean hasDescString() {
		return false;
	}

	public String makeLongString() {
		return makeString();
	}

	@Override
	public String toString() {
		return makeString();
	}
}
