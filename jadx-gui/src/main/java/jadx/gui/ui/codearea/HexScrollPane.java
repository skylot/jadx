package jadx.gui.ui.codearea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Utilities;

import org.fife.ui.rtextarea.Gutter;

public class HexScrollPane extends JScrollPane {
	private final HexArea hexArea;

	public HexScrollPane(HexArea hexArea) {
		super(hexArea);
		this.hexArea = hexArea;
		setRowHeaderView(new Gutter(hexArea));
	}

	private class HexOffsetView extends JPanel {
		private final Border OUTER_BORDER = new MatteBorder(0, 0, 0, 2, Color.LIGHT_GRAY);

		HexOffsetView() {
			setFont(hexArea.getFont());
			setForeground(Color.LIGHT_GRAY);
			setBorderGap(5);
		}

		public void setBorderGap(int borderGap) {
			Border inner = new EmptyBorder(0, borderGap, 0, borderGap);
			setBorder(new CompoundBorder(OUTER_BORDER, inner));
			setPreferredWidth();
		}

		private void setPreferredWidth() {
			Element root = hexArea.getDocument().getDefaultRootElement();
			int lines = root.getElementCount();
			int digits = Math.max(String.valueOf(lines).length(), 4);

			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = fontMetrics.charWidth('0') * digits;
			Insets insets = getInsets();
			int preferredWidth = insets.left + insets.right + width;

			Dimension dim = getPreferredSize();
			dim.setSize(preferredWidth, HEIGHT);
			setPreferredSize(dim);
			setSize(dim);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			// Determine the width of the space available to draw the line number
			FontMetrics fontMetrics = hexArea.getFontMetrics(hexArea.getFont());
			Insets insets = getInsets();
			int availableWidth = getSize().width - insets.left - insets.right;

			// Determine the rows to draw within the clipped bounds.
			Rectangle clip = g.getClipBounds();
			int rowStartOffset = hexArea.viewToModel2D(new Point(0, clip.y));
			int endOffset = hexArea.viewToModel2D(new Point(0, clip.y + clip.height));

			// Rectangle visibleRect = g.getClipBounds();
			// Insets hexAreaInsets = hexArea.getInsets();
			// int cellHeight = hexArea.getLineHeight();
			// int ascent = hexArea.getMaxAscent();
			// int topLine = (visibleRect.y-hexAreaInsets.top)/cellHeight;
			// int actualTopY = topLine*cellHeight + hexAreaInsets.top;
			// int y = actualTopY + ascent;
			//
			// int line = topLine + 1;
			// while (y<visibleRect.y+visibleRect.height && line<hexArea.getLineCount()) {
			// try {
			// String lineNumber = String.valueOf(line);
			// int stringWidth = fontMetrics.stringWidth(lineNumber);
			// int offsetX = getOffsetX(availableWidth, stringWidth) + insets.left;
			//// int offsetY = getOffsetY(rowStartOffset, fontMetrics);
			// g.drawString(lineNumber, offsetX, y);
			//
			// y += cellHeight;
			// line++;
			// } catch (Exception e) {
			// break;
			// }
			// }

			while (rowStartOffset <= endOffset) {
				try {
					// Get the line number as a string and then determine the
					// "X" and "Y" offsets for drawing the string.
					String lineNumber = getTextLineNumber(rowStartOffset);
					int stringWidth = fontMetrics.stringWidth(lineNumber);
					int x = getOffsetX(availableWidth, stringWidth) + insets.left;
					int y = getOffsetY(rowStartOffset, fontMetrics);
					g.drawString(lineNumber, x, y);

					// Move to the next row
					rowStartOffset = Utilities.getRowEnd(hexArea, rowStartOffset) + 1;
				} catch (Exception e) {
					break;
				}
			}
		}

		protected String getTextLineNumber(int rowStartOffset) {
			Element root = hexArea.getDocument().getDefaultRootElement();
			int index = root.getElementIndex(rowStartOffset);
			Element line = root.getElement(index);

			if (line.getStartOffset() == rowStartOffset)
				return String.valueOf(index + 1);
			else
				return "";
		}

		private int getOffsetX(int availableWidth, int stringWidth) {
			return (int) ((availableWidth - stringWidth) * 1.0f);
		}

		private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics)
				throws BadLocationException {
			Rectangle r = hexArea.modelToView(rowStartOffset);
			int y = r.y + r.height;
			int descent = fontMetrics.getDescent();

			return y - descent;
		}
	}
}
