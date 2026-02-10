package jadx.gui.ui.codearea.sync.fallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import jadx.gui.ui.codearea.AbstractCodeArea;

public abstract class AbstractCodeAreaToken {
	protected final AbstractCodeArea area;
	private final int atPos;
	protected int startPos;
	protected int length;

	protected AbstractCodeAreaToken(AbstractCodeArea area, int at) throws BadLocationException, FallbackSyncException {
		this.area = area;
		this.atPos = at;
		this.extractTokenAt();
	}

	public int getAtPos() {
		return atPos;
	}

	public String getStr() throws BadLocationException {
		return area.getText(this.startPos, this.length);
	}

	public boolean isMethodConstructorDeclarationOrCall() throws BadLocationException {
		return area.getText(this.startPos + this.length, 1).equals("(");
	}

	// Class field reference within a method
	public abstract boolean isFieldReference() throws BadLocationException;

	// Class field token in class field declaration
	public abstract boolean isClassField() throws BadLocationException;

	public abstract AbstractCodeAreaLine getLine() throws BadLocationException;

	// Helper to extract token under caret (at pos)
	private void extractTokenAt() throws FallbackSyncException, BadLocationException {
		String text = area.getText();
		if (text == null || text.isEmpty()) {
			throw new FallbackSyncException("text area is null or empty");
		}
		// Find word boundaries around caretPos
		int start = atPos;
		int end = atPos;

		while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
			start--;
		}
		while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
			end++;
		}
		if (start == end) {
			// No identifier found, try string literal at caret line
			int line = area.getLineOfOffset(atPos);
			String lineText = area.getText(area.getLineStartOffset(line), area.getLineEndOffset(line) - area.getLineStartOffset(line));
			Pattern p = Pattern.compile("\"([^\"]*)\"");
			Matcher m = p.matcher(lineText);
			while (m.find()) {
				int litStart = area.getLineStartOffset(line) + m.start(1);
				int litEnd = area.getLineStartOffset(line) + m.end(1);
				if (atPos >= litStart && atPos <= litEnd) {
					this.startPos = m.start(1);
					this.length = m.end(1) - m.start(1);
					return;
				}
			}
			throw new FallbackSyncException("Unable to extract token at position " + atPos);
		}
		this.startPos = start;
		this.length = end - start;
	}
}
