package jadx.gui.ui.hexeditor.editor;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import jadx.gui.ui.hexeditor.buffer.ByteBuffer;
import jadx.gui.ui.hexeditor.buffer.ByteBufferDocument;
import jadx.gui.ui.hexeditor.buffer.ByteBufferSelectionModel;

public class JHexEditorHeader extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final String HEX_ALPHABET = "0123456789ABCDEF";
	private final JHexEditor parent;
	private final JHexEditorListener listener = new JHexEditorListener() {
		@Override
		public void dataInserted(ByteBuffer buffer, long offset, int length) {

		}

		@Override
		public void dataOverwritten(ByteBuffer buffer, long offset, int length) {
			repaint();
		}

		@Override
		public void dataRemoved(ByteBuffer buffer, long offset, long length) {

		}

		@Override
		public void selectionChanged(ByteBufferSelectionModel sm, long start, long end) {
			repaint();
		}

		@Override
		public void documentChanged(JHexEditor editor, ByteBufferDocument document) {
			repaint();
		}

		@Override
		public void colorsChanged(JHexEditor editor, JHexEditorColors colors) {
			repaint();
		}

		@Override
		public void editorStatusChanged(JHexEditor editor) {
			repaint();
		}
	};
	private Dimension minimumSize = null;
	private Dimension preferredSize = null;

	public JHexEditorHeader(JHexEditor parent) {
		this.parent = parent;
		if (this.parent != null) {
			this.parent.addHexEditorListener(listener);
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (this.parent != null) {
			this.parent.addHexEditorListener(listener);
		}
	}

	@Override
	public void removeNotify() {
		if (this.parent != null) {
			this.parent.removeHexEditorListener(listener);
		}
		super.removeNotify();
	}

	@Override
	public Dimension getMinimumSize() {
		if (minimumSize != null) {
			return minimumSize;
		}
		Insets i = getInsets();
		FontMetrics fm = getFontMetrics(parent.getFont());
		if (fm == null)
			return new Dimension(100, 20); // Fallback
		int ch = fm.getHeight() + 2; // Row height
		int cw = fm.stringWidth(HEX_ALPHABET) / 16; // Char width estimate

		int minTextWidth = fm.stringWidth("Sel: 00000000:00000000 Len: 0/0 TXT RO OVR BE ISO-8859-1");
		int minimumWidth = minTextWidth + cw * 5 + i.left + i.right;

		int minimumHeight = ch + 5 + i.top + i.bottom;

		return new Dimension(minimumWidth, minimumHeight);
	}

	@Override
	public void setMinimumSize(Dimension minimumSize) {
		this.minimumSize = minimumSize;
		revalidate();
	}

	@Override
	public Dimension getPreferredSize() {
		if (preferredSize != null) {
			return preferredSize;
		}
		Insets i = getInsets();
		FontMetrics fm = getFontMetrics(parent.getFont());
		if (fm == null)
			return getMinimumSize(); // Fallback
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;

		String maxAddr = addressString(-1, false);
		String maxLen = addressString(Long.MAX_VALUE, false);

		String sampleText = "Sel: " +
				maxAddr + ":" + maxAddr + // Sel: start:end
				"  Len: " +
				maxLen + "/" + maxLen + // Len: selected/total
				"  TXT RO OVR BE ISO-8859-1";
		int preferredTextWidth = fm.stringWidth(sampleText);

		int preferredWidth = preferredTextWidth + cw * 10 + i.left + i.right;

		int preferredHeight = ch + 5 + i.top + i.bottom;

		return new Dimension(preferredWidth, preferredHeight);
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		this.preferredSize = preferredSize;
		revalidate();
	}

	@Override
	protected void paintComponent(Graphics g) {
		// Standard Graphics2D setup
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		// Get insets, width, height
		Insets i = getInsets();
		int fw = getWidth(), w = fw - i.left - i.right;
		int fh = getHeight(), h = fh - i.top - i.bottom;

		// Get font metrics
		g.setFont(parent.getFont());
		FontMetrics fm = g.getFontMetrics();
		if (fm == null)
			return; // Cannot paint without font metrics
		int ca = fm.getAscent() + 1; // Character ascent for baseline
		int ch = fm.getHeight() + 2; // Row height including padding
		int cw = fm.stringWidth(HEX_ALPHABET) / 16; // Character width estimate
		if (cw <= 0)
			cw = 1; // Avoid division by zero or incorrect calculations

		// Get colors and status from parent editor
		JHexEditorColors colors = parent.getColors();
		boolean extendBorders = parent.getExtendBorders();
		boolean decimalAddresses = parent.getDecimalAddresses();
		long ss = parent.getSelectionMin();
		long se = parent.getSelectionMax();
		long sl = parent.getSelectionLength();
		long length = parent.length();

		// Vertical position for the text baseline (centered vertically)
		int ty = i.top + ((h - ch) / 2) + ca; // Calculate middle Y for the single line of text

		// Draw Background
		g.setColor(colors.headerArea);
		if (extendBorders)
			g.fillRect(0, 0, fw, fh);
		else
			g.fillRect(i.left, i.top, w, h);

		// Draw Text Elements (Sel, Len, Status)
		g.setColor(colors.headerText);

		// Start drawing position (adjust for left inset)
		int currentX = i.left + cw / 2; // Start with a small left padding

		// Selection Range (Sel: start:end
		String selLabel = "Sel:";
		g.drawString(selLabel, currentX, ty);
		currentX += fm.stringWidth(selLabel) + cw; // "Sel:" + space

		String sss = addressString(ss, decimalAddresses);
		g.drawString(sss, currentX, ty);
		currentX += fm.stringWidth(sss); // start

		String separator1 = ":";
		g.drawString(separator1, currentX, ty);
		currentX += fm.stringWidth(separator1); // :

		String ses = addressString(se, decimalAddresses);
		g.drawString(ses, currentX, ty);
		currentX += fm.stringWidth(ses) + cw; // end + larger space

		// Draw Divider After Sel Range
		int dividerTopY = extendBorders ? 0 : i.top;
		int dividerHeight = extendBorders ? fh : h;
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		// Length Information (Len: selected/total-
		g.setColor(colors.headerText);
		String lenLabel = "Len:";
		g.drawString(lenLabel, currentX, ty);
		currentX += fm.stringWidth(lenLabel) + cw; // "Len:" + space

		String sls = addressString(sl, decimalAddresses);
		g.drawString(sls, currentX, ty);
		currentX += fm.stringWidth(sls); // selected length

		String separator2 = "/";
		g.drawString(separator2, currentX, ty);
		currentX += fm.stringWidth(separator2);

		String ls = addressString(length, decimalAddresses);
		g.drawString(ls, currentX, ty);
		currentX += fm.stringWidth(ls) + cw; // total length + larger space

		// Draw Divider After Len Info
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		// Status Indicators (TXT/HEX, RO/RW, OVR/INS, LE/BE, Charset)
		g.setColor(colors.headerText); // Ensure color is set for text

		// Draw TXT/HEX status
		String statusTxtHex = parent.isTextActive() ? "TXT" : "HEX";
		g.drawString(statusTxtHex, currentX, ty);
		currentX += fm.stringWidth(statusTxtHex) + cw; // TXT/HEX

		// Draw Divider After TXT/HEX
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		g.setColor(colors.headerText); // Restore text color

		// Draw RO/RW status
		String statusRoRw = parent.isReadOnly() ? "RO" : "RW";
		g.drawString(statusRoRw, currentX, ty);
		currentX += fm.stringWidth(statusRoRw) + cw; // RO/RW

		// Draw Divider After RO/RW
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		g.setColor(colors.headerText);

		// Draw OVR/INS status
		String statusOvrIns = parent.getOvertype() ? "OVR" : "INS";
		g.drawString(statusOvrIns, currentX, ty);
		currentX += fm.stringWidth(statusOvrIns) + cw; // OVR/INS

		// Draw Divider After OVR/INS
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		g.setColor(colors.headerText);

		// Draw LE/BE status
		String statusEndian = parent.isLittleEndian() ? "LE" : "BE";
		g.drawString(statusEndian, currentX, ty);
		currentX += fm.stringWidth(statusEndian) + cw; // LE/BE

		// Draw Divider After LE/BE
		g.setColor(colors.headerDivider);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += 1; // Move past the divider
		currentX += cw; // Add larger space after the divider

		g.setColor(colors.headerText);

		// Draw Charset
		String statusCharset = parent.getCharset();
		g.drawString(statusCharset, currentX, ty);
		// No divider or space needed after the last element

		// Draw Bottom Border
		g.setColor(colors.headerDivider); // Use headerDivider color for the border
		if (extendBorders)
			g.fillRect(0, fh - 1, fw, 1);
		else
			g.fillRect(i.left, i.top + h - 1, w, 1);
	}

	private String addressString(long address, boolean decimalAddresses) {
		return parent.getDocument().addressString(address, decimalAddresses);
	}
}
