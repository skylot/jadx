package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.ILineAttributeNode;

public abstract class LineAttrNode extends AttrNode implements ILineAttributeNode {

	private int sourceLine;

	/**
	 * Position where a node declared at in decompiled code
	 */
	private int defPosition;

	@Override
	public int getSourceLine() {
		return sourceLine;
	}

	@Override
	public void setSourceLine(int sourceLine) {
		this.sourceLine = sourceLine;
	}

	@Override
	public int getDefPosition() {
		return this.defPosition;
	}

	@Override
	public void setDefPosition(int defPosition) {
		this.defPosition = defPosition;
	}

	public void addSourceLineFrom(LineAttrNode lineAttrNode) {
		if (this.getSourceLine() == 0) {
			this.setSourceLine(lineAttrNode.getSourceLine());
		}
	}

	public void copyLines(LineAttrNode lineAttrNode) {
		setSourceLine(lineAttrNode.getSourceLine());
		setDefPosition(lineAttrNode.getDefPosition());
	}
}
