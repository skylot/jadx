package jadx.gui.ui.codearea;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.SmartHighlightPainter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.utils.JumpPosition;

class MouseHoverHighlighter extends MouseMotionAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(MouseHoverHighlighter.class);

	private final CodeArea codeArea;
	private final CodeLinkGenerator codeLinkGenerator;
	private final Highlighter.HighlightPainter highlighter;

	private boolean added;

	public MouseHoverHighlighter(CodeArea codeArea, CodeLinkGenerator codeLinkGenerator) {
		this.codeArea = codeArea;
		this.codeLinkGenerator = codeLinkGenerator;
		this.highlighter = new SmartHighlightPainter(codeArea.getMarkOccurrencesColor());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Highlighter highlighter = codeArea.getHighlighter();
		if (added) {
			highlighter.removeAllHighlights();
			added = false;
		}
		if (e.getModifiersEx() != 0) {
			return;
		}
		try {
			Token token = codeArea.viewToToken(e.getPoint());
			if (token == null || token.getType() != TokenTypes.IDENTIFIER) {
				return;
			}
			JumpPosition jump = codeLinkGenerator.getJumpLinkAtOffset(codeArea, token.getOffset());
			if (jump == null) {
				return;
			}
			highlighter.removeAllHighlights();
			highlighter.addHighlight(token.getOffset(), token.getEndOffset(), this.highlighter);
			added = true;
		} catch (Exception exc) {
			LOG.error("Mouse hover highlight error", exc);
		}
	}
}
