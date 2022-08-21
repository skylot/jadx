package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ITry;
import jadx.api.plugins.utils.Utils;

public class TryData implements ITry {

	private final int startOffset;
	private final int endOffset;
	private final ICatch catchHandler;

	public TryData(int startOffset, int endOffset, ICatch catchHandler) {
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.catchHandler = catchHandler;
	}

	@Override
	public ICatch getCatch() {
		return catchHandler;
	}

	@Override
	public int getStartOffset() {
		return startOffset;
	}

	@Override
	public int getEndOffset() {
		return endOffset;
	}

	@Override
	public String toString() {
		return "Try{" + Utils.formatOffset(startOffset) + " - " + Utils.formatOffset(endOffset) + ": " + catchHandler + '}';
	}
}
