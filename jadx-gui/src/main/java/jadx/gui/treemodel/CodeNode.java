package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.gui.utils.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class CodeNode extends JClass {

	private static final ImageIcon ICON = Utils.openIcon("file_obj");

	private final String line;
	private final int lineNum;

	public CodeNode(JavaClass javaClass, int lineNum, String line) {
		super(javaClass, (JClass) makeFrom(javaClass.getDeclaringClass()));
		this.line = line;
		this.lineNum = lineNum;
	}

	@Override
	public Icon getIcon() {
		return ICON;
	}

	@Override
	public int getLine() {
		return lineNum;
	}

	@Override
	public String makeString() {
		return getCls().getFullName() + ":" + lineNum + "   " + line;
	}

	@Override
	public String makeLongString() {
		return makeString();
	}

	@Override
	public String toString() {
		return makeString();
	}
}
