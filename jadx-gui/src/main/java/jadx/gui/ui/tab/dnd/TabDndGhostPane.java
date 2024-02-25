/*
 * The MIT License (MIT)
 * Copyright (c) 2015 TERAI Atsuhiro
 * Copyright (c) 2024 Skylot
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jadx.gui.ui.tab.dnd;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class TabDndGhostPane extends JComponent {

	private final TabDndController dnd;
	private final Rectangle lineRect = new Rectangle();
	private final Point location = new Point();
	private transient BufferedImage ghostImage;
	private TabDndGhostType tabDndGhostType = TabDndGhostType.COLORFUL_RECT;
	private Dimension ghostSize;
	private Color ghostColor;
	private Insets insets;

	protected TabDndGhostPane(TabDndController dnd) {
		super();
		this.dnd = dnd;
		loadSettings();
	}

	public void loadSettings() {
		Color systemColor = UIManager.getColor("Component.focusColor");
		Color fallbackColor = new Color(0, 100, 255);
		ghostColor = systemColor != null ? systemColor : fallbackColor;

		Insets ins = UIManager.getInsets("TabbedPane.tabInsets");
		insets = ins != null ? ins : new Insets(0, 0, 0, 0);
	}

	public void setTargetRect(int x, int y, int width, int height) {
		lineRect.setBounds(x, y, width, height);
	}

	public void setGhostImage(BufferedImage ghostImage) {
		this.ghostImage = ghostImage;
	}

	public void setGhostSize(Dimension ghostSize) {
		ghostSize.setSize(ghostSize.width + insets.left + insets.right, ghostSize.height + insets.top + insets.bottom);
		this.ghostSize = ghostSize;
	}

	public void setGhostType(TabDndGhostType tabDndGhostType) {
		this.tabDndGhostType = tabDndGhostType;
	}

	public TabDndGhostType getGhostType() {
		return this.tabDndGhostType;
	}

	public void setColor(Color color) {
		this.ghostColor = color;
	}

	public Color getColor() {
		return this.ghostColor;
	}

	public void setPoint(Point pt) {
		this.location.setLocation(pt);
	}

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if (!v) {
			setTargetRect(0, 0, 0, 0);
			setGhostImage(null);
			setGhostSize(new Dimension());
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		dnd.onPaintGlassPane(g2);
		renderMark(g2);
		renderGhost(g2);
		g2.dispose();
	}

	private void renderGhost(Graphics2D g) {
		switch (tabDndGhostType) {
			case IMAGE: {
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
				if (ghostImage == null) {
					return;
				}
				double x = location.getX() - ghostImage.getWidth(this) / 2d;
				double y = location.getY() - ghostImage.getHeight(this) / 2d;
				g.drawImage(ghostImage, (int) x, (int) y, this);
				break;
			}
			case COLORFUL_RECT: {
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f));
				if (ghostSize == null) {
					return;
				}
				double x = location.getX() - ghostSize.getWidth() / 2d;
				double y = location.getY() - ghostSize.getHeight() / 2d;
				g.setPaint(ghostColor);
				g.fillRect((int) x, (int) y, ghostSize.width, ghostSize.height);
				break;
			}
			case NONE:
				break;
		}
	}

	private void renderMark(Graphics2D g) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .7f));
		g.setPaint(ghostColor);
		g.fill(lineRect);
	}
}
