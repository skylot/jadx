package jadx.gui.treemodel;

import javax.swing.Icon;

import jadx.api.JavaNode;
import jadx.gui.utils.search.StringRef;

public class CodeNode extends JNode {

	private static final long serialVersionUID = 1658650786734966545L;

	private final transient JNode jNode;
	private final transient JClass jParent;
	private final transient StringRef line;
	private final transient int lineNum;
	private transient int pos;

	public CodeNode(JNode jNode, StringRef lineStr, int lineNum, int pos) {
		this.jNode = jNode;
		this.jParent = this.jNode.getJParent();
		this.line = lineStr;
		this.lineNum = lineNum;
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
		JClass parent = jParent;
		if (parent != null) {
			return parent.getRootClass();
		}
		if (jNode instanceof JClass) {
			return (JClass) jNode;
		}
		return null;
	}

	public StringRef getLineStr() {
		return line;
	}

	@Override
	public int getLine() {
		return lineNum;
	}

	@Override
	public String makeDescString() {
		return line.toString();
	}

	@Override
	public boolean hasDescString() {
		return true;
	}

	@Override
	public String makeString() {
		return jNode.makeLongString();
	}

	@Override
	public String makeLongString() {
		return makeString();
	}

	@Override
	public String getSyntaxName() {
		return jNode.getSyntaxName();
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
	public int getPos() {
		return pos;
	}
}
