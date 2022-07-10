package jadx.gui.ui.codearea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Element;
import javax.swing.text.View;

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

	private final transient AbstractCodeArea codeArea;
	private final transient ICodeInfo codeInfo;
	private boolean useSourceLines = true;

	private transient int lastDigits;
	private transient int lastLine;

	private final transient Color numberColor;
	private final transient Color normalNumColor;
	private final transient Color currentColor;
	private final transient Border border;

	private transient Insets textAreaInsets;
	private transient Rectangle visibleRect = new Rectangle();

	public LineNumbers(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		this.codeInfo = codeArea.getCodeInfo();

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
		int digits = Math.max(numberLength(lines), numberLength(getMaxDebugLine()));
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

	private int numberLength(int value) {
		return String.valueOf(value).length();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void paintComponent(Graphics g) {
		visibleRect = g.getClipBounds(visibleRect);
		if (visibleRect == null) {
			visibleRect = getVisibleRect();
		}
		if (visibleRect == null) {
			return;
		}
		applyRenderHints(g);

		Font baseFont = codeArea.getFont();
		Font font = baseFont.deriveFont(baseFont.getSize2D() - 1.0f);
		g.setFont(font);

		Dimension size = getSize();
		g.setColor(codeArea.getBackground());
		g.fillRect(0, visibleRect.y, size.width, visibleRect.height);

		FontMetrics fontMetrics = codeArea.getFontMetrics(font);
		Insets insets = getInsets();
		int availableWidth = size.width - insets.right;

		textAreaInsets = codeArea.getInsets(textAreaInsets);
		if (visibleRect.y < textAreaInsets.top) {
			visibleRect.height -= (textAreaInsets.top - visibleRect.y);
			visibleRect.y = textAreaInsets.top;
		}
		boolean lineWrap = codeArea.getLineWrap();
		int cellHeight = codeArea.getLineHeight();
		int ascent = codeArea.getMaxAscent();
		int currentLine = codeArea.getCaretLineNumber();

		int y;
		int topLine;
		int linesCount;
		View parentView = null;
		Rectangle editorRect = null;
		if (lineWrap) {
			Element root = codeArea.getDocument().getDefaultRootElement();
			parentView = codeArea.getUI().getRootView(codeArea).getView(0);
			int topPosition = codeArea.viewToModel(new Point(visibleRect.x, visibleRect.y));
			topLine = root.getElementIndex(topPosition);
			linesCount = root.getElementCount();
			editorRect = getEditorBoundingRect();
			Rectangle topLineBounds = getLineBounds(parentView, topLine, editorRect);
			if (topLineBounds == null) {
				return;
			}
			y = ascent + topLineBounds.y;
		} else {
			linesCount = codeArea.getLineCount();
			topLine = (visibleRect.y - textAreaInsets.top) / cellHeight;
			y = ascent + topLine * cellHeight + textAreaInsets.top;
		}
		int endY = visibleRect.y + visibleRect.height + ascent;
		int lineNum = topLine;
		boolean isCurLine = updateColor(g, false, true);
		while (y < endY && lineNum < linesCount) {
			try {
				String lineStr = getTextLineNumber(lineNum + 1);
				if (lineStr != null) {
					isCurLine = updateColor(g, lineNum == currentLine, isCurLine);
					int x = availableWidth - fontMetrics.stringWidth(lineStr);
					g.drawString(lineStr, x, y);
				}
				if (lineWrap) {
					Rectangle lineBounds = getLineBounds(parentView, lineNum, editorRect);
					if (lineBounds == null) {
						return;
					}
					y += lineBounds.height;
				} else {
					y += cellHeight;
				}
				lineNum++;
			} catch (Exception e) {
				LOG.debug("Line numbers draw error", e);
				break;
			}
		}
	}

	private Rectangle getLineBounds(View parent, int line, Rectangle editorRect) {
		Shape alloc = parent.getChildAllocation(line, editorRect);
		if (alloc == null) {
			return null;
		}
		if (alloc instanceof Rectangle) {
			return (Rectangle) alloc;
		}
		return alloc.getBounds();
	}

	protected Rectangle getEditorBoundingRect() {
		Rectangle bounds = codeArea.getBounds();
		if (bounds.width <= 0 || bounds.height <= 0) {
			return null;
		}
		bounds.x = 0;
		bounds.y = 0;
		Insets insets = codeArea.getInsets();
		bounds.x += insets.left;
		bounds.y += insets.top;
		bounds.width -= insets.left + insets.right;
		bounds.height -= insets.top + insets.bottom;
		return bounds;
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
		Integer sourceLine = codeInfo.getCodeMetadata().getLineMapping().get(lineNumber);
		if (sourceLine == null) {
			return null;
		}
		return String.valueOf(sourceLine);
	}

	private int getMaxDebugLine() {
		return codeInfo.getCodeMetadata().getLineMapping()
				.keySet().stream()
				.mapToInt(Integer::intValue)
				.max().orElse(0);
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

	public boolean isUseSourceLines() {
		return useSourceLines;
	}

	public void setUseSourceLines(boolean useSourceLines) {
		this.useSourceLines = useSourceLines;
	}
}
