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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.tab.TabbedPane;

public class TabDndController {

	private final transient JTabbedPane pane;

	private static final int DROP_TARGET_MARK_SIZE = 4;
	private static final int SCROLL_AREA_SIZE = 30;
	private static final int SCROLL_AREA_EXTRA = 30; // Making area with scroll buttons a bit bigger.
	private static final String ACTION_SCROLL_FORWARD = "scrollTabsForwardAction";
	private static final String ACTION_SCROLL_BACKWARD = "scrollTabsBackwardAction";

	private final transient TabDndGhostPane tabDndGhostPane;
	protected int dragTabIndex = -1;

	protected boolean drawGhost = true; // Semi-transparent tab copy moving along with cursor.
	protected boolean paintScrollTriggerAreas = false; // For debug purposes.

	protected Rectangle rectBackward = new Rectangle();
	protected Rectangle rectForward = new Rectangle();
	private boolean isDragging = false;

	public TabDndController(TabbedPane pane, JadxSettings settings) {
		pane.setDnd(this);
		this.pane = pane;

		tabDndGhostPane = new TabDndGhostPane(this, settings);

		new DropTarget(tabDndGhostPane, DnDConstants.ACTION_COPY_OR_MOVE, new TabDndTargetListener(this), true);
		DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(pane,
				DnDConstants.ACTION_COPY_OR_MOVE,
				new TabDndGestureListener(this));
	}

	public static boolean isHorizontalTabPlacement(int tabPlacement) {
		return tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM;
	}

	/**
	 * Check if dragging near edges and scroll to according
	 * direction through programmatically clicking system's scroll buttons.
	 *
	 * @param glassPt Cursor position in TabbedPane coordinates.
	 */
	public void scrollIfNeeded(Point glassPt) {
		Rectangle r = getTabAreaBounds();
		boolean isHorizontal = isHorizontalTabPlacement(pane.getTabPlacement());

		// Trying to avoid calculating two directions simultaneously. Forward first.
		if (isHorizontal) {
			rectForward.setBounds(r.x + r.width - SCROLL_AREA_SIZE - SCROLL_AREA_EXTRA,
					r.y,
					SCROLL_AREA_SIZE + SCROLL_AREA_EXTRA,
					r.height);
		} else {
			rectForward.setBounds(r.x,
					r.y + r.height - SCROLL_AREA_SIZE - SCROLL_AREA_EXTRA,
					r.width,
					SCROLL_AREA_SIZE + SCROLL_AREA_EXTRA);
		}
		rectForward = SwingUtilities.convertRectangle(pane.getParent(), rectForward, tabDndGhostPane);
		if (rectForward.contains(glassPt)) {
			clickScrollButton(ACTION_SCROLL_FORWARD);
		}

		// Backward.
		if (isHorizontal) {
			rectBackward.setBounds(r.x, r.y, SCROLL_AREA_SIZE, r.height);
		} else {
			rectBackward.setBounds(r.x, r.y, r.width, SCROLL_AREA_SIZE);
		}
		rectBackward = SwingUtilities.convertRectangle(pane.getParent(), rectBackward, tabDndGhostPane);
		if (rectBackward.contains(glassPt)) {
			clickScrollButton(ACTION_SCROLL_BACKWARD);
		}
	}

	private void clickScrollButton(String actionKey) {
		JButton forwardButton = null;
		JButton backwardButton = null;
		for (Component c : pane.getComponents()) {
			if (c instanceof JButton) {
				if (Objects.isNull(forwardButton)) {
					forwardButton = (JButton) c;
				} else {
					backwardButton = (JButton) c;
					break;
				}
			}
		}
		JButton scrollButton = ACTION_SCROLL_FORWARD.equals(actionKey) ? forwardButton : backwardButton;
		if (scrollButton != null && scrollButton.isEnabled()) {
			scrollButton.doClick();
		}
	}

	/**
	 * Finds the tab index by cursor position. If tabs first half contains the point,
	 * then its index is returned. Second half means inserting at next index.
	 *
	 * @param glassPt Cursor position in TabbedPane coordinates.
	 * @return Tab index.
	 */
	protected int getTargetTabIndex(Point glassPt) {
		Point tabPt = SwingUtilities.convertPoint(tabDndGhostPane, glassPt, pane);
		boolean isHorizontal = isHorizontalTabPlacement(pane.getTabPlacement());
		for (int i = 0; i < pane.getTabCount(); ++i) {
			Rectangle r = pane.getBoundsAt(i);

			// First half.
			if (isHorizontal) {
				r.width = r.width / 2 + 1;
			} else {
				r.height = r.height / 2 + 1;
			}
			if (r.contains(tabPt)) {
				return i;
			}

			// Second half.
			if (isHorizontal) {
				r.x = r.x + r.width;
			} else {
				r.y = r.y + r.height;
			}
			if (r.contains(tabPt)) {
				return i + 1;
			}
		}

		int count = pane.getTabCount();
		if (count == 0) {
			return -1;
		}
		Rectangle lastRect = pane.getBoundsAt(count - 1);
		Point d = isHorizontal ? new Point(1, 0) : new Point(0, 1);
		lastRect.translate(lastRect.width * d.x, lastRect.height * d.y);
		return lastRect.contains(tabPt) ? count : -1;
	}

	protected void swapTabs(int oldIdx, int newIdx) {
		if (newIdx < 0 || oldIdx == newIdx) {
			return;
		}
		final Component cmp = pane.getComponentAt(oldIdx);
		final Component tab = pane.getTabComponentAt(oldIdx);
		final String title = pane.getTitleAt(oldIdx);
		final Icon icon = pane.getIconAt(oldIdx);
		final String tip = pane.getToolTipTextAt(oldIdx);
		final boolean isEnabled = pane.isEnabledAt(oldIdx);
		newIdx = oldIdx > newIdx ? newIdx : (newIdx - 1);
		pane.remove(oldIdx);
		pane.insertTab(title, icon, cmp, tip, newIdx);
		pane.setEnabledAt(newIdx, isEnabled);
		if (isEnabled) {
			pane.setSelectedIndex(newIdx);
		}
		pane.setTabComponentAt(newIdx, tab);
	}

	protected void updateTargetMark(int tabIdx) {
		boolean isSideNeighbor = tabIdx < 0 || dragTabIndex == tabIdx || tabIdx == dragTabIndex + 1;
		if (isSideNeighbor) {
			tabDndGhostPane.setTargetRect(0, 0, 0, 0);
			return;
		}
		Rectangle boundsRect = pane.getBoundsAt(Math.max(0, tabIdx - 1));
		final Rectangle r = SwingUtilities.convertRectangle(pane, boundsRect, tabDndGhostPane);
		int a = Math.min(tabIdx, 1);
		if (isHorizontalTabPlacement(pane.getTabPlacement())) {
			tabDndGhostPane.setTargetRect(r.x + r.width * a - DROP_TARGET_MARK_SIZE / 2,
					r.y,
					DROP_TARGET_MARK_SIZE,
					r.height);
		} else {
			tabDndGhostPane.setTargetRect(r.x,
					r.y + r.height * a - DROP_TARGET_MARK_SIZE / 2,
					r.width,
					DROP_TARGET_MARK_SIZE);
		}
	}

	protected void initGlassPane(Point tabPt) {
		pane.getRootPane().setGlassPane(tabDndGhostPane);
		if (drawGhost) {
			Component c = pane.getTabComponentAt(dragTabIndex);
			if (c == null) {
				return;
			}
			Dimension d = c.getPreferredSize();
			switch (tabDndGhostPane.getGhostType()) {
				case IMAGE: {
					GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
					GraphicsDevice device = env.getDefaultScreenDevice();
					GraphicsConfiguration config = device.getDefaultConfiguration();
					BufferedImage image = config.createCompatibleImage(d.width, d.height, BufferedImage.TRANSLUCENT);
					Graphics2D g2 = image.createGraphics();
					SwingUtilities.paintComponent(g2, c, tabDndGhostPane, 0, 0, d.width, d.height);
					g2.dispose();
					tabDndGhostPane.setGhostImage(image);
					pane.setTabComponentAt(dragTabIndex, c);
					break;
				}
				case OUTLINE: {
					tabDndGhostPane.setGhostSize(d);
					break;
				}
				case TARGET_MARK:
					break;
			}
		}
		Point glassPt = SwingUtilities.convertPoint(pane, tabPt, tabDndGhostPane);
		tabDndGhostPane.setPoint(glassPt);
		tabDndGhostPane.setVisible(true);
	}

	protected Rectangle getTabAreaBounds() {
		Rectangle tabbedRect = pane.getBounds();
		Rectangle compRect;
		if (pane.getSelectedComponent() != null) {
			compRect = pane.getSelectedComponent().getBounds();
		} else {
			compRect = new Rectangle();
		}
		int tabPlacement = pane.getTabPlacement();
		if (isHorizontalTabPlacement(tabPlacement)) {
			tabbedRect.height = tabbedRect.height - compRect.height;
			if (tabPlacement == JTabbedPane.BOTTOM) {
				tabbedRect.y += compRect.y + compRect.height;
			}
		} else {
			tabbedRect.width = tabbedRect.width - compRect.width;
			if (tabPlacement == JTabbedPane.RIGHT) {
				tabbedRect.x += compRect.x + compRect.width;
			}
		}
		tabbedRect.grow(2, 2);
		return tabbedRect;
	}

	public void onPaintGlassPane(Graphics2D g) {
		boolean isScrollLayout = pane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT;
		if (isScrollLayout && paintScrollTriggerAreas) {
			g.setPaint(tabDndGhostPane.getColor());
			g.fill(rectBackward);
			g.fill(rectForward);
		}
	}

	public boolean onStartDrag(Point pt) {
		setDragging(true);
		int idx = pane.indexAtLocation(pt.x, pt.y);
		int selIdx = pane.getSelectedIndex();
		boolean isTabRunsRotated =
				!(pane.getUI() instanceof MetalTabbedPaneUI) && pane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT && idx != selIdx;
		dragTabIndex = isTabRunsRotated ? selIdx : idx;
		if (dragTabIndex >= 0 && pane.isEnabledAt(dragTabIndex)) {
			initGlassPane(pt);
			return true;
		}

		return false;
	}

	public void loadSettings() {
		tabDndGhostPane.loadSettings();
	}

	public boolean isDragging() {
		return isDragging;
	}

	public void setDragging(boolean dragging) {
		isDragging = dragging;
	}

	public TabDndGhostPane getDndGhostPane() {
		return tabDndGhostPane;
	}
}
