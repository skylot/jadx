package jadx.gui.ui.codearea;

import org.fife.ui.rtextarea.LineNumberFormatter;

import jadx.api.ICodeInfo;

public class SourceLineFormatter implements LineNumberFormatter {
	private final ICodeInfo codeInfo;
	private final int maxLength;

	public SourceLineFormatter(ICodeInfo codeInfo) {
		this.codeInfo = codeInfo;
		this.maxLength = calcMaxLength(codeInfo);
	}

	@Override
	public String format(int lineNumber) {
		Integer sourceLine = codeInfo.getCodeMetadata().getLineMapping().get(lineNumber);
		if (sourceLine == null) {
			return "";
		}
		return String.valueOf(sourceLine);
	}

	@Override
	public int getMaxLength(int maxLineNumber) {
		return maxLength;
	}

	private static int calcMaxLength(ICodeInfo codeInfo) {
		int maxLine = codeInfo.getCodeMetadata().getLineMapping()
				.values().stream()
				.mapToInt(Integer::intValue)
				.max().orElse(1);
		// maxLine can be anything including zero and negative numbers,
		// so use safe 'stringify' method instead faster 'Math.log10'
		return Integer.toString(maxLine).length();
	}
}
