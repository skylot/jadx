package jadx.gui.ui.codearea;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JTextArea;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexPreviewPanel extends JTextArea {
	private static final Logger LOG = LoggerFactory.getLogger(HexPreviewPanel.class);

	private final HexAreaConfiguration config;

	private byte[] bytes = new byte[0];

	private Color highlightColor = Color.YELLOW;
	private boolean hasHighlight = false;

	public HexPreviewPanel(HexAreaConfiguration configuration) {
		super(0, configuration.bytesPerLine);

		this.config = configuration;
		initView();
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			char c = (char) bytes[i];
			if (c <= 0x1f || (c & (1 << 7)) != 0) {
				sb.append('.');
			} else {
				sb.append(c);
			}
			if (i != bytes.length - 1 && i % config.bytesPerLine == config.bytesPerLine - 1) {
				sb.append('\n');
			}
		}
		setText(sb.toString());
	}

	public void clearHighlights() {
		hasHighlight = false;
		getHighlighter().removeAllHighlights();
	}

	public void highlightBytes(int startOffset, int endOffset) {
		if (hasHighlight) {
			getHighlighter().removeAllHighlights();
		}

		// Include line breaks in the index
		startOffset += startOffset / config.bytesPerLine;
		endOffset += endOffset / config.bytesPerLine;

		Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
		try {
			getHighlighter().addHighlight(startOffset, endOffset + 1, painter);
		} catch (BadLocationException e) {
			LOG.error("Unable to highlight bytes " + startOffset + ":" + endOffset, e);
		}
		hasHighlight = true;
	}

	public void setHighlightColor(Color highlightColor) {
		this.highlightColor = highlightColor;
	}

	public void setBorderColor(Color borderColor) {
		setBorder(new MatteBorder(0, 2, 0, 0, borderColor));
	}

	public void applyTheme(Theme theme, Font font) {
		setBackground(theme.bgColor);
		setHighlightColor(theme.selectionBG);
		setBorderColor(theme.gutterBorderColor);
		setDisabledTextColor(theme.scheme.getStyle(SyntaxScheme.IDENTIFIER).foreground);
		setFont(font);
	}

	private void initView() {
		setEnabled(false);
		setEditable(false);
	}
}
