package jadx.plugins.input.java.data.code.trycatch;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ITry;
import jadx.api.plugins.utils.Utils;

public class JavaTryData implements ITry {

	private final int startOffset;
	private final int endOffset;
	private ICatch catchHandler;

	public JavaTryData(int startOffset, int endOffset) {
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	@Override
	public ICatch getCatch() {
		return catchHandler;
	}

	public void setCatch(ICatch catchHandler) {
		this.catchHandler = catchHandler;
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
	public int hashCode() {
		return startOffset + 31 * endOffset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JavaTryData)) {
			return false;
		}
		JavaTryData that = (JavaTryData) o;
		return startOffset == that.startOffset && endOffset == that.endOffset;
	}

	@Override
	public String toString() {
		return "Try{" + Utils.formatOffset(startOffset) + " - " + Utils.formatOffset(endOffset) + ": " + catchHandler + '}';
	}
}
