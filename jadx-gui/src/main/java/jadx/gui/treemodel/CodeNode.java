package jadx.gui.treemodel;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

import jadx.api.JavaNode;

public class CodeNode extends JNode {
	private static final long serialVersionUID = 1658650786734966545L;

	private final transient JClass rootCls;
	private final transient JNode jNode;
	private final transient String line;
	private final transient int pos;

	public CodeNode(JClass rootCls, JNode jNode, String lineStr, int pos) {
		this.rootCls = rootCls;
		this.jNode = jNode;
		this.line = lineStr;
		this.pos = pos;
	}

	@Override
	public Icon getIcon() {
		return jNode.getIcon();
	}

	@Override
	public JavaNode getJavaNode() {
		return jNode.getJavaNode();
	}

	@Override
	public JClass getJParent() {
		return getRootClass();
	}

	@Override
	public JClass getRootClass() {
		return rootCls;
	}

	@Override
	public String makeDescString() {
		return line;
	}

	@Override
	public boolean hasDescString() {
		return true;
	}

	@Override
	public String makeString() {
		return jNode.makeString();
	}

	@Override
	public String makeStringHtml() {
		return jNode.makeStringHtml();
	}

	@Override
	public String makeLongString() {
		return jNode.makeLongString();
	}

	@Override
	public String makeLongStringHtml() {
		return jNode.makeLongStringHtml();
	}

	@Override
	public boolean disableHtml() {
		return jNode.disableHtml();
	}

	@Override
	public String getSyntaxName() {
		return jNode.getSyntaxName();
	}

	@Override
	public int getPos() {
		return pos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CodeNode)) {
			return false;
		}
		CodeNode codeNode = (CodeNode) o;
		return jNode.equals(codeNode.jNode);
	}

	@Override
	public int hashCode() {
		return jNode.hashCode();
	}

	@Override
	public int compareTo(@NotNull JNode other) {
		if (other instanceof CodeNode) {
			return jNode.compareTo(((CodeNode) other).jNode);
		}
		return super.compareTo(other);
	}
}
