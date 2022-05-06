package jadx.gui.utils;

import java.util.Map;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
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
	private int pos;
	private int lineOffset;
	private TokenInfo tokenInfo;

	private int javaNodePos = -1;
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
			pos = codeArea.getCaretPosition();
			line = codeArea.getLineOfOffset(pos);
			lineOffset = pos - codeArea.getLineStartOffset(line);

			tokenInfo = getTokenInfoByOffset(codeArea.getTokenListForLine(line), pos);

			ICodeInfo codeInfo = codeArea.getCodeInfo();
			if (codeInfo.hasMetadata()) {
				ICodeMetadata metadata = codeInfo.getCodeMetadata();
				ICodeAnnotation ann = metadata.getAt(pos);
				if (ann instanceof InsnCodeOffset) {
					codeRawOffset = ((InsnCodeOffset) ann).getOffset();
					ICodeNodeRef javaNode = metadata.getNodeAt(pos);
					if (javaNode != null) {
						javaNodePos = javaNode.getDefPosition();
					}
				}
			}
			LOG.debug("Saved position data: line={}, lineOffset={}, token={}, codeRawOffset={}, javaNodeLine={}",
					line, lineOffset, tokenInfo, codeRawOffset, javaNodePos);
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
			int newPos = getNewPos();
			int newLine = codeArea.getLineOfOffset(newPos);
			Token token = codeArea.getTokenListForLine(newLine);
			int tokenPos = getOffsetFromTokenInfo(tokenInfo, token);
			if (tokenPos == -1) {
				int lineStartOffset = codeArea.getLineStartOffset(newLine);
				int lineEndOffset = codeArea.getLineEndOffset(newLine) - 1;
				int lineLength = lineEndOffset - lineStartOffset;
				// can't restore using token -> just restore by line offset
				if (lineOffset < lineLength) {
					tokenPos = lineStartOffset + lineOffset;
				} else {
					// line truncated -> set caret at line end
					tokenPos = lineEndOffset;
				}
			}
			codeArea.setCaretPosition(tokenPos);
			LOG.debug("Restored caret position: {}", tokenPos);
		} catch (Exception e) {
			LOG.warn("Failed to restore caret position", e);
		}
	}

	private int getNewPos() throws BadLocationException {
		int newLinesCount = codeArea.getLineCount();
		if (linesCount == newLinesCount) {
			return pos;
		}
		// lines count changes, try find line by raw offset
		ICodeInfo codeInfo = codeArea.getCodeInfo();
		if (javaNodePos != -1 && codeInfo.hasMetadata()) {
			JClass cls = codeArea.getJClass();
			if (cls != null) {
				ICodeMetadata codeMetadata = codeInfo.getCodeMetadata();
				for (Map.Entry<Integer, ICodeAnnotation> entry : codeMetadata.getAsMap().entrySet()) {
					int annPos = entry.getKey();
					if (annPos >= javaNodePos) {
						ICodeAnnotation ann = entry.getValue();
						if (ann instanceof InsnCodeOffset
								&& ((InsnCodeOffset) ann).getOffset() == codeRawOffset) {
							return annPos;
						}
					}
				}
			}
		}
		// fallback: assume lines added/removed before caret
		int newLine = line - (linesCount - newLinesCount);
		return codeArea.getLineStartOffset(newLine);
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
