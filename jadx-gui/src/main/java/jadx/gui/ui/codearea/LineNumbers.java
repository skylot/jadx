package jadx.gui.ui.codearea;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineNumbers extends JPanel implements CaretListener {
	private static final Logger LOG = LoggerFactory.getLogger(LineNumbers.class);

	private static final long serialVersionUID = -4978268673635308190L;

	private static final int NUM_HEIGHT = Integer.MAX_VALUE - 1000000;
	private static final Map<?, ?> DESKTOP_HINTS = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

	private CodeArea codeArea;
	private boolean useSourceLines = true;

	private int lastDigits;
	private int lastLine;
	private Map<String, FontMetrics> fonts;

	private transient final Color numberColor;
	private transient final Color currentColor;
	private transient final Border border;

	public LineNumbers(CodeArea component) {
		this.codeArea = component;
		setFont(component.getFont());
		SyntaxScheme syntaxScheme = codeArea.getSyntaxScheme();
		numberColor = syntaxScheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground;
		currentColor = syntaxScheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground;
		border = new MatteBorder(0, 0, 0, 1, syntaxScheme.getStyle(Token.COMMENT_MULTILINE).foreground);
		setBackground(codeArea.getBackground());
		setForeground(numberColor);

		setBorderGap(5);
		setPreferredWidth();

		component.addCaretListener(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					useSourceLines = !useSourceLines;
					repaint();
				}
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
		int digits = Math.max(String.valueOf(lines).length(), 3);
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
		super.paintComponent(g);
		applyRenderHints(g);

		Font font = codeArea.getFont();
		font = font.deriveFont(font.getSize2D() - 1.0f);
		g.setFont(font);

		Dimension size = getSize();
		g.setColor(codeArea.getBackground());
		g.fillRect(0, 0, size.width, size.height);

		FontMetrics fontMetrics = codeArea.getFontMetrics(font);
		Insets insets = getInsets();
		int availableWidth = size.width - insets.left - insets.right;
		Rectangle clip = g.getClipBounds();
		int rowStartOffset = codeArea.viewToModel(new Point(0, clip.y));
		int endOffset = codeArea.viewToModel(new Point(0, clip.y + clip.height));

		while (rowStartOffset <= endOffset) {
			try {
				String lineNumber = getTextLineNumber(rowStartOffset);
				if (lineNumber != null) {
					if (isCurrentLine(rowStartOffset)) {
						g.setColor(currentColor);
					} else {
						g.setColor(numberColor);
					}
					int stringWidth = fontMetrics.stringWidth(lineNumber);
					int x = availableWidth - stringWidth + insets.left;
					int y = getOffsetY(rowStartOffset, fontMetrics);
					g.drawString(lineNumber, x, y);
				}
				rowStartOffset = Utilities.getRowEnd(codeArea, rowStartOffset) + 1;
			} catch (Exception e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Line numbers draw error", e);
				}
				break;
			}
		}
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

	private boolean isCurrentLine(int rowStartOffset) {
		int caretPosition = codeArea.getCaretPosition();
		Element root = codeArea.getDocument().getDefaultRootElement();
		return root.getElementIndex(rowStartOffset) == root.getElementIndex(caretPosition);
	}

	@Nullable
	protected String getTextLineNumber(int rowStartOffset) {
		Element root = codeArea.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(rowStartOffset);
		Element line = root.getElement(index);
		if (line.getStartOffset() != rowStartOffset) {
			return null;
		}
		int lineNumber = index + 1;
		if (useSourceLines) {
			Integer sourceLine = codeArea.getSourceLine(lineNumber);
			if (sourceLine == null) {
				return null;
			}
			return String.valueOf(sourceLine);
		}
		return String.valueOf(lineNumber);
	}

	private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics) throws BadLocationException {
		Rectangle r = codeArea.modelToView(rowStartOffset);
		if (r == null) {
			throw new BadLocationException("Can't get Y offset", rowStartOffset);
		}
		int lineHeight = fontMetrics.getHeight();
		int y = r.y + r.height;
		int descent = 0;
		if (r.height == lineHeight) {
			descent = fontMetrics.getDescent();
		} else {
			if (fonts == null) {
				fonts = new HashMap<>();
			}
			Element root = codeArea.getDocument().getDefaultRootElement();
			int index = root.getElementIndex(rowStartOffset);
			Element line = root.getElement(index);
			for (int i = 0; i < line.getElementCount(); i++) {
				Element child = line.getElement(i);
				AttributeSet as = child.getAttributes();
				String fontFamily = (String) as.getAttribute(StyleConstants.FontFamily);
				Integer fontSize = (Integer) as.getAttribute(StyleConstants.FontSize);
				String key = fontFamily + fontSize;
				FontMetrics fm = fonts.get(key);
				if (fm == null) {
					Font font = new Font(fontFamily, Font.PLAIN, fontSize);
					fm = codeArea.getFontMetrics(font);
					fonts.put(key, fm);
				}
				descent = Math.max(descent, fm.getDescent());
			}
		}
		return y - descent;
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
}
