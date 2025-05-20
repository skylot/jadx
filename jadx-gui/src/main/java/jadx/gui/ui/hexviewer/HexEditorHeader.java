package jadx.gui.ui.hexviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.exbin.bined.CodeAreaCaretListener;
import org.exbin.bined.CodeAreaSection;
import org.exbin.bined.DataChangedListener;
import org.exbin.bined.SelectionChangedListener;
import org.exbin.bined.SelectionRange;
import org.exbin.bined.basic.BasicCodeAreaSection;
import org.exbin.bined.swing.section.SectCodeArea;

public class HexEditorHeader extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final String HEX_ALPHABET = "0123456789ABCDEF";
	private final SectCodeArea parent;
	private final DataChangedListener dataChangedListener = this::repaint;
	private final CodeAreaCaretListener caretMovedListener = caretPosition -> repaint();
	private final SelectionChangedListener selectionChangedListener = this::repaint;

	private Dimension minimumSize = null;
	private Dimension preferredSize = null;

	public HexEditorHeader(SectCodeArea parent) {
		this.parent = parent;
		if (this.parent != null) {
			this.parent.addCaretMovedListener(caretMovedListener);
			this.parent.addSelectionChangedListener(selectionChangedListener);
			this.parent.addDataChangedListener(dataChangedListener);
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (this.parent != null) {
			this.parent.addCaretMovedListener(caretMovedListener);
			this.parent.addSelectionChangedListener(selectionChangedListener);
			this.parent.addDataChangedListener(dataChangedListener);
		}
	}

	@Override
	public void removeNotify() {
		if (this.parent != null) {
			this.parent.removeCaretMovedListener(caretMovedListener);
			this.parent.removeSelectionChangedListener(selectionChangedListener);
			this.parent.removeDataChangedListener(dataChangedListener);
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
		if (fm == null) {
			return new Dimension(100, 20); // Fallback
		}
		int ch = fm.getHeight() + 2; // Row height
		int cw = fm.stringWidth(HEX_ALPHABET) / 16; // Char width estimate

		String sampleText = "Sel: 00000000:00000000 Len: 00000000/00000000 TXT UTF-8";
		int minTextWidth = fm.stringWidth(sampleText);
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
		if (fm == null) {
			return getMinimumSize(); // Fallback
		}
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;

		String sampleText = "Sel: 00000000:00000000 Len: 00000000/00000000 TXT UTF-8";
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
		int fw = getWidth();
		int fh = getHeight();
		int w = fw - i.left - i.right;
		int h = fh - i.top - i.bottom;

		// Get font metrics
		g.setFont(parent.getFont());
		FontMetrics fm = g.getFontMetrics();
		if (fm == null) {
			return; // Cannot paint without font metrics
		}
		int ca = fm.getAscent() + 1; // Character ascent for baseline
		int ch = fm.getHeight() + 2; // Row height including padding
		int cw = fm.stringWidth(HEX_ALPHABET) / 16; // Character width estimate
		if (cw <= 0) {
			cw = 1; // Avoid division by zero or incorrect calculations
		}

		// Get colors and status from parent editor
		Color separatorForeground = UIManager.getColor("Separator.foreground");
		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Panel.foreground");

		SelectionRange selectionRange = parent.getSelection();
		long ss = selectionRange.getStart();
		long se = selectionRange.getEnd();
		long sl = selectionRange.getLength();
		long length = parent.getDataSize();

		// Vertical position for the text baseline (centered vertically)
		int ty = i.top + ((h - ch) / 2) + ca; // Calculate middle Y for the single line of text

		// Draw Background
		g.setColor(themeBackground);
		g.fillRect(i.left, i.top, w, h);

		// Draw Text Elements (Sel, Len, Status)
		g.setColor(themeForeground);

		// Start drawing position (adjust for left inset)
		int currentX = i.left + cw / 2; // Start with a small left padding

		// Selection Range (Sel: start:end
		String selLabel = "Sel:";
		g.drawString(selLabel, currentX, ty);
		currentX += fm.stringWidth(selLabel) + cw; // "Sel:" + space

		String sss = addressString(ss);
		g.drawString(sss, currentX, ty);
		currentX += fm.stringWidth(sss); // start

		String separator1 = ":";
		g.drawString(separator1, currentX, ty);
		currentX += fm.stringWidth(separator1); // :

		String ses = addressString(se);
		g.drawString(ses, currentX, ty);
		currentX += fm.stringWidth(ses) + cw; // end + larger space

		// Draw Divider After Sel Range
		int dividerTopY = i.top;
		int dividerHeight = h;
		g.setColor(separatorForeground);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += cw; // Add larger space after the divider

		// Length Information (Len: selected/total-
		g.setColor(themeForeground);
		String lenLabel = "Len:";
		g.drawString(lenLabel, currentX, ty);
		currentX += fm.stringWidth(lenLabel) + cw; // "Len:" + space

		String sls = addressString(sl);
		g.drawString(sls, currentX, ty);
		currentX += fm.stringWidth(sls); // selected length

		String separator2 = "/";
		g.drawString(separator2, currentX, ty);
		currentX += fm.stringWidth(separator2);

		String ls = addressString(length);
		g.drawString(ls, currentX, ty);
		currentX += fm.stringWidth(ls) + cw; // total length + larger space

		// Draw Divider After Len Info
		g.setColor(separatorForeground);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += cw; // Add larger space after the divider

		// Status Indicators (TXT/HEX, RO/RW, OVR/INS, LE/BE, Charset)
		g.setColor(themeForeground); // Ensure color is set for text

		// Draw TXT/HEX status
		String statusTxtHex = "HEX";
		CodeAreaSection section = parent.getActiveSection();
		if (section == BasicCodeAreaSection.TEXT_PREVIEW) {
			statusTxtHex = "TXT";
		}

		g.drawString(statusTxtHex, currentX, ty);
		currentX += fm.stringWidth(statusTxtHex) + cw; // TXT/HEX

		// Draw Divider After TXT/HEX
		g.setColor(separatorForeground);
		g.fillRect(currentX, dividerTopY, 1, dividerHeight);
		currentX += cw; // Add larger space after the divider

		g.setColor(themeForeground); // Restore text color

		// Draw Charset
		String statusCharset = parent.getCharset().name();
		g.drawString(statusCharset, currentX, ty);
		// No divider or space needed after the last element

		// Draw Bottom Border
		g.setColor(separatorForeground); // Use headerDivider color for the border
		g.fillRect(i.left, i.top + h - 1, w, 1);

	}

	public String addressString(long address) {
		return String.format("%08X", address);
	}
}
