package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;

public class LineNumbers extends JPanel implements CaretListener {
	private static final Logger LOG = LoggerFactory.getLogger(LineNumbers.class);

	private static final long serialVersionUID = -4978268673635308190L;

	private static final int NUM_HEIGHT = Integer.MAX_VALUE - 1000000;
	private static final Map<?, ?> DESKTOP_HINTS = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

	private final AbstractCodeArea codeArea;
	private boolean useSourceLines = true;

	private transient int lastDigits;
	private transient int lastLine;

	private final transient Color numberColor;
	private final transient Color normalNumColor;
	private final transient Color currentColor;
	private final transient Border border;

	private transient Insets textAreaInsets;
	private transient Rectangle visibleRect = new Rectangle();
	private transient ICodeInfo codeInfo;

	public LineNumbers(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		setFont(codeArea.getFont());
		SyntaxScheme syntaxScheme = codeArea.getSyntaxScheme();
		numberColor = syntaxScheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground;
		normalNumColor = syntaxScheme.getStyle(Token.ANNOTATION).foreground;
		currentColor = syntaxScheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground;
		border = new MatteBorder(0, 0, 0, 1, syntaxScheme.getStyle(Token.COMMENT_MULTILINE).foreground);
		setBackground(codeArea.getBackground());
		setForeground(numberColor);

		setBorderGap(5);
		setPreferredWidth();

		codeArea.addCaretListener(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				useSourceLines = !useSourceLines;
				repaint();
			}
		});
	}

	public void setBorderGap(int borderGap) {
		Border inner = new EmptyBorder(0, borderGap, 0, borderGap);
		setBorder(new CompoundBorder(border, inner));
		lastDigits = 0;
	}

	private void setPreferredWidth() {
		Element root = codeArea.getDocument().getDefaultRootElement();
		int lines = root.getElementCount();
		int digits = Math.max(String.valueOf(lines).length(), 4);
		if (lastDigits != digits) {
			lastDigits = digits;
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = fontMetrics.charWidth('0') * digits;
			Insets insets = getInsets();
			int preferredWidth = insets.left + insets.right + width;

			Dimension d = getPreferredSize();
			if (d != null) {
				d.setSize(preferredWidth, NUM_HEIGHT);
				setPreferredSize(d);
				setSize(d);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		codeInfo = codeArea.getNode().getCodeInfo();

		visibleRect = g.getClipBounds(visibleRect);
		if (visibleRect == null) {
			visibleRect = getVisibleRect();
		}
		if (visibleRect == null) {
			return;
		}
		applyRenderHints(g);

		Font font = codeArea.getFont();
		font = font.deriveFont(font.getSize2D() - 1.0f);
		g.setFont(font);

		Dimension size = getSize();
		g.setColor(codeArea.getBackground());
		g.fillRect(0, 0, size.width, size.height);

		FontMetrics fontMetrics = codeArea.getFontMetrics(font);
		Insets insets = getInsets();
		int availableWidth = size.width - insets.right;

		int cellHeight = codeArea.getLineHeight();
		int ascent = codeArea.getMaxAscent();

		textAreaInsets = codeArea.getInsets(textAreaInsets);
		if (visibleRect.y < textAreaInsets.top) {
			visibleRect.height -= (textAreaInsets.top - visibleRect.y);
			visibleRect.y = textAreaInsets.top;
		}

		int topLine = (visibleRect.y - textAreaInsets.top) / cellHeight;
		int actualTopY = topLine * cellHeight + textAreaInsets.top;
		int y = actualTopY + ascent;
		int endY = visibleRect.y + visibleRect.height + ascent;

		int currentLine = 1 + codeArea.getCaretLineNumber();
		int lineNum = topLine + 1;
		int linesCount = codeArea.getLineCount();
		boolean isCurLine = updateColor(g, false, true);
		while (y < endY && lineNum <= linesCount) {
			try {
				String lineStr = getTextLineNumber(lineNum);
				if (lineStr != null) {
					isCurLine = updateColor(g, lineNum == currentLine, isCurLine);
					int x = availableWidth - fontMetrics.stringWidth(lineStr);
					g.drawString(lineStr, x, y);
				} else if (!useSourceLines) {
					break;
				}
				lineNum++;
				y += cellHeight;
			} catch (Exception e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Line numbers draw error", e);
				}
				break;
			}
		}
	}

	private boolean updateColor(Graphics g, boolean newCurLine, boolean oldCurLine) {
		if (oldCurLine != newCurLine) {
			if (newCurLine) {
				g.setColor(currentColor);
			} else {
				g.setColor(useSourceLines ? numberColor : normalNumColor);
			}
		}
		return newCurLine;
	}

	private void applyRenderHints(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D) g;
			if (DESKTOP_HINTS != null) {
				g2d.setRenderingHints(DESKTOP_HINTS);
			} else {
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			}
		}
	}

	@Nullable
	protected String getTextLineNumber(int lineNumber) {
		if (!useSourceLines) {
			return String.valueOf(lineNumber);
		}
		Integer sourceLine = codeInfo.getLineMapping().get(lineNumber);
		if (sourceLine == null) {
			return null;
		}
		return String.valueOf(sourceLine);
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		int caretPosition = codeArea.getCaretPosition();
		Element root = codeArea.getDocument().getDefaultRootElement();
		int currentLine = root.getElementIndex(caretPosition);
		if (lastLine != currentLine) {
			repaint();
			lastLine = currentLine;
		}
	}

	public void setUseSourceLines(boolean useSourceLines) {
		this.useSourceLines = useSourceLines;
	}
}
