package jadx.gui.ui.codearea.sync;

import java.awt.Color;

import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import jadx.gui.ui.codearea.AbstractCodeArea;

/**
 * Highlighting and scrolling utility into a CodeArea for a given color
 */
public class CodeSyncHighlighter {
	private final Color color;

	public CodeSyncHighlighter(Color color) {
		this.color = color;
	}

	public void highlightAndScrollToLine(AbstractCodeArea area, int lineIndex) throws BadLocationException {
		highlightLine(area, lineIndex);
		area.scrollToPos(area.getLineStartOffset(lineIndex));
	}

	public void highlightLine(AbstractCodeArea area, int lineIndex) throws BadLocationException {
		int startOffset = area.getLineStartOffset(lineIndex);
		int endOffset = area.getLineEndOffset(lineIndex);
		highlightRange(area, startOffset, endOffset);
	}

	// Highlight range in code area with a temporary yellow highlight
	public void highlightRange(AbstractCodeArea area, int startOffset, int endOffset) throws BadLocationException {
		Highlighter hl = area.getHighlighter();
		HighlightPainter painter =
				new DefaultHighlighter.DefaultHighlightPainter(this.color);
		Object tag = hl.addHighlight(startOffset, endOffset, painter);
		new Timer(1000, e -> hl.removeHighlight(tag)).start();
	}

	public static CodeSyncHighlighter defaultHighlighter() {
		return new CodeSyncHighlighter(UIManager.getColor("TabbedPane.hoverColor"));
	}
}
