package jadx.gui.treemodel;

import jadx.api.JavaNode;
import jadx.gui.utils.search.StringRef;

import javax.swing.Icon;

public class CodeNode extends JNode {

	private static final long serialVersionUID = 1658650786734966545L;

	private final JNode jNode;
	private final JClass jParent;
	private final StringRef line;
	private final int lineNum;

	public CodeNode(JNode jNode, int lineNum, StringRef line) {
		this.jNode = jNode;
		this.jParent = this.jNode.getJParent();
		this.line = line;
		this.lineNum = lineNum;
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
}
