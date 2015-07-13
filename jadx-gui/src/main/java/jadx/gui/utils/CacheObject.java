package jadx.gui.utils;

import org.jetbrains.annotations.Nullable;

public class CacheObject {
	@Nullable
	private TextSearchIndex textIndex;

	public void reset() {
		textIndex = null;
	}

	@Nullable
	public TextSearchIndex getTextIndex() {
		return textIndex;
	}

	public void setTextIndex(@Nullable TextSearchIndex textIndex) {
		this.textIndex = textIndex;
	}
}
