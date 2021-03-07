package jadx.gui.utils;

import java.util.Map;

import org.fife.ui.rsyntaxtextarea.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.data.annotations.ICodeRawOffset;
import jadx.gui.treemodel.JClass;
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
	private TokenInfo tokenInfo;

	private int javaNodeLine = -1;
	private int codeRawOffset = -1;

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

			tokenInfo = getTokenInfoByOffset(codeArea.getTokenListForLine(line), pos);

			JClass cls = codeArea.getJClass();
			if (cls != null) {
				JavaClass topParentClass = cls.getJavaNode().getTopParentClass();
				Object ann = topParentClass.getAnnotationAt(new CodePosition(line));
				if (ann instanceof ICodeRawOffset) {
					codeRawOffset = ((ICodeRawOffset) ann).getOffset();
					CodeLinesInfo codeLinesInfo = new CodeLinesInfo(topParentClass);
					JavaNode javaNodeAtLine = codeLinesInfo.getJavaNodeByLine(line);
					if (javaNodeAtLine != null) {
						javaNodeLine = javaNodeAtLine.getDecompiledLine();
					}
				}
			}
			LOG.debug("Saved position data: line={}, lineOffset={}, token={}, codeRawOffset={}, javaNodeLine={}",
					line, lineOffset, tokenInfo, codeRawOffset, javaNodeLine);
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
			int newLine = getNewLine();
			int lineStartOffset = codeArea.getLineStartOffset(newLine);
			int lineEndOffset = codeArea.getLineEndOffset(newLine) - 1;
			int lineLength = lineEndOffset - lineStartOffset;
			Token token = codeArea.getTokenListForLine(newLine);
			int newPos = getOffsetFromTokenInfo(tokenInfo, token);
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
			LOG.debug("Restored caret position: {}, line: {}", newPos, newLine);
		} catch (Exception e) {
			LOG.warn("Failed to restore caret position", e);
		}
	}

	private int getNewLine() {
		int newLinesCount = codeArea.getLineCount();
		if (linesCount == newLinesCount) {
			return line;
		}
		// lines count changes, try find line by raw offset
		if (javaNodeLine != -1) {
			JClass cls = codeArea.getJClass();
			if (cls != null) {
				JavaClass topParentClass = cls.getJavaNode().getTopParentClass();
				for (Map.Entry<CodePosition, Object> entry : topParentClass.getCodeAnnotations().entrySet()) {
					CodePosition pos = entry.getKey();
					if (pos.getOffset() == 0 && pos.getLine() >= javaNodeLine) {
						Object ann = entry.getValue();
						if (ann instanceof ICodeRawOffset && ((ICodeRawOffset) ann).getOffset() == codeRawOffset) {
							return pos.getLine() - 1;
						}
					}
				}
			}
		}
		// fallback: assume lines added/removed before caret
		return line - (linesCount - newLinesCount);
	}

	private TokenInfo getTokenInfoByOffset(Token token, int offset) {
		if (token == null) {
			return null;
		}
		int index = 1;
		while (token.getEndOffset() < offset) {
			token = token.getNextToken();
			if (token == null) {
				return null;
			}
			index++;
		}
		return new TokenInfo(index, token.getType());
	}

	private int getOffsetFromTokenInfo(TokenInfo tokenInfo, Token token) {
		if (tokenInfo == null || token == null) {
			return -1;
		}
		int index = tokenInfo.getIndex();
		if (index == -1) {
			return -1;
		}
		for (int i = 0; i < index; i++) {
			token = token.getNextToken();
			if (token == null) {
				return -1;
			}
		}
		if (token.getType() != tokenInfo.getType()) {
			return -1;
		}
		return token.getOffset();
	}

	private static final class TokenInfo {
		private final int index;
		private final int type;

		public TokenInfo(int index, int type) {
			this.index = index;
			this.type = type;
		}

		public int getIndex() {
			return index;
		}

		public int getType() {
			return type;
		}

		@Override
		public String toString() {
			return "Token{index=" + index + ", type=" + type + '}';
		}
	}
}
