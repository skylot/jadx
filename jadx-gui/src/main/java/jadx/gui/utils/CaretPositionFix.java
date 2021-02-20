package jadx.gui.utils;

import org.fife.ui.rsyntaxtextarea.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.AbstractCodeArea;

/**
 * After class refresh (rename, comment, etc) the change of document is undetectable.
 * So use Token index or offset in line to calculate the new caret position.
 */
public class CaretPositionFix {
	private static final Logger LOG = LoggerFactory.getLogger(CaretPositionFix.class);

	private final AbstractCodeArea codeArea;

	private int linesCount;
	private int line;
	private int lineOffset;
	private int tokenIndex;
	private int tokenText;

	public CaretPositionFix(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
	}

	/**
	 * Save caret position by anchor to token under caret
	 */
	public void save() {
		try {
			linesCount = codeArea.getLineCount();
			int pos = codeArea.getCaretPosition();
			line = codeArea.getLineOfOffset(pos);
			lineOffset = pos - codeArea.getLineStartOffset(line);
			tokenIndex = getTokenIndexByOffset(codeArea.getTokenListForLine(line), pos);
		} catch (Exception e) {
			LOG.error("Failed to save caret position before refresh", e);
			line = -1;
		}
	}

	/**
	 * Restore caret position in refreshed code.
	 * Expected to be called in UI thread.
	 */
	public void restore() {
		if (line == -1) {
			return;
		}
		try {
			int newLinesCount = codeArea.getLineCount();
			if (linesCount != newLinesCount) {
				// assume lines added/removed before caret, so adjust line number
				line -= linesCount - newLinesCount;
			}
			int lineStartOffset = codeArea.getLineStartOffset(line);
			int lineEndOffset = codeArea.getLineEndOffset(line) - 1;
			int lineLength = lineEndOffset - lineStartOffset;
			Token token = codeArea.getTokenListForLine(line);
			int newPos = getOffsetOfTokenByIndex(tokenIndex, token);
			if (newPos == -1) {
				// can't restore using token -> just restore by line offset
				if (lineOffset < lineLength) {
					newPos = lineStartOffset + lineOffset;
				} else {
					// line truncated -> set caret at line end
					newPos = lineEndOffset;
				}
			}
			codeArea.setCaretPosition(newPos);
		} catch (Exception e) {
			LOG.warn("Failed to restore caret position", e);
		}
	}

	private int getTokenIndexByOffset(Token token, int offset) {
		if (token == null) {
			return -1;
		}
		int index = 1;
		while (token.getEndOffset() < offset) {
			token = token.getNextToken();
			if (token == null) {
				return -1;
			}
			index++;
		}
		return index;
	}

	private int getOffsetOfTokenByIndex(int index, Token token) {
		if (token != null && index != -1) {
			for (int i = 0; i < index; i++) {
				token = token.getNextToken();
				if (token == null) {
					return -1;
				}
			}
			return token.getOffset();
		}
		return -1;
	}
}
