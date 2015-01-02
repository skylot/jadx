package jadx.gui.ui;

import javax.swing.JPanel;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class LineNumbers extends JPanel implements CaretListener {
	private static final long serialVersionUID = -4978268673635308190L;

	private static final Border OUTER = new MatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY);

	private static final int HEIGHT = Integer.MAX_VALUE - 1000000;
	private static final Color FOREGROUND = Color.GRAY;
	private static final Color BACKGROUND = ContentArea.BACKGROUND;
	private static final Color CURRENT_LINE_FOREGROUND = new Color(227, 0, 0);

	private ContentArea contentArea;
	private boolean useSourceLines = true;

	private int lastDigits;
	private int lastLine;
	private Map<String, FontMetrics> fonts;

	public LineNumbers(ContentArea component) {
		this.contentArea = component;
		setFont(component.getFont());
		setBackground(BACKGROUND);
		setForeground(FOREGROUND);

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
		setBorder(new CompoundBorder(OUTER, inner));
		lastDigits = 0;
	}

	private void setPreferredWidth() {
		Element root = contentArea.getDocument().getDefaultRootElement();
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
				d.setSize(preferredWidth, HEIGHT);
				setPreferredSize(d);
				setSize(d);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		FontMetrics fontMetrics = contentArea.getFontMetrics(contentArea.getFont());
		Insets insets = getInsets();
		int availableWidth = getSize().width - insets.left - insets.right;
		Rectangle clip = g.getClipBounds();
		int rowStartOffset = contentArea.viewToModel(new Point(0, clip.y));
		int endOffset = contentArea.viewToModel(new Point(0, clip.y + clip.height));

		while (rowStartOffset <= endOffset) {
			try {
				if (isCurrentLine(rowStartOffset)) {
					g.setColor(CURRENT_LINE_FOREGROUND);
				} else {
					g.setColor(FOREGROUND);
				}
				String lineNumber = getTextLineNumber(rowStartOffset);
				int stringWidth = fontMetrics.stringWidth(lineNumber);
				int x = availableWidth - stringWidth + insets.left;
				int y = getOffsetY(rowStartOffset, fontMetrics);
				g.drawString(lineNumber, x, y);
				rowStartOffset = Utilities.getRowEnd(contentArea, rowStartOffset) + 1;
			} catch (Exception e) {
				break;
			}
		}
	}

	private boolean isCurrentLine(int rowStartOffset) {
		int caretPosition = contentArea.getCaretPosition();
		Element root = contentArea.getDocument().getDefaultRootElement();
		return root.getElementIndex(rowStartOffset) == root.getElementIndex(caretPosition);
	}

	protected String getTextLineNumber(int rowStartOffset) {
		Element root = contentArea.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(rowStartOffset);
		Element line = root.getElement(index);
		if (line.getStartOffset() == rowStartOffset) {
			int lineNumber = index + 1;
			if (useSourceLines) {
				Integer sourceLine = contentArea.getSourceLine(lineNumber);
				if (sourceLine != null) {
					return String.valueOf(sourceLine);
				}
			} else {
				return String.valueOf(lineNumber);
			}
		}
		return "";
	}

	private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics) throws BadLocationException {
		Rectangle r = contentArea.modelToView(rowStartOffset);
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
				fonts = new HashMap<String, FontMetrics>();
			}
			Element root = contentArea.getDocument().getDefaultRootElement();
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
					fm = contentArea.getFontMetrics(font);
					fonts.put(key, fm);
				}
				descent = Math.max(descent, fm.getDescent());
			}
		}
		return y - descent;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		int caretPosition = contentArea.getCaretPosition();
		Element root = contentArea.getDocument().getDefaultRootElement();
		int currentLine = root.getElementIndex(caretPosition);
		if (lastLine != currentLine) {
			repaint();
			lastLine = currentLine;
		}
	}
}
