package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AttrNode;

public abstract class LineAttrNode extends AttrNode {

	private int sourceLine;

	private int decompiledLine;

	// the position exactly where a node declared at in decompiled java code.
	private int defPosition;

	public int getDefPosition() {
		return this.defPosition;
	}

	public void setDefPosition(int defPosition) {
		this.defPosition = defPosition;
	}

	public int getSourceLine() {
		return sourceLine;
	}

	public void setSourceLine(int sourceLine) {
		this.sourceLine = sourceLine;
	}

	public int getDecompiledLine() {
		return decompiledLine;
	}

	public void setDecompiledLine(int decompiledLine) {
		this.decompiledLine = decompiledLine;
	}

	public void copyLines(LineAttrNode lineAttrNode) {
		setSourceLine(lineAttrNode.getSourceLine());
		setDecompiledLine(lineAttrNode.getDecompiledLine());
	}
}
