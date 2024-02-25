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

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

class TabDndTargetListener implements DropTargetListener {
	private static final Point HIDDEN_POINT = new Point(0, -1000);

	private final transient TabDndController dnd;

	public TabDndTargetListener(TabDndController dnd) {
		this.dnd = dnd;
	}

	@Override
	public void dragEnter(DropTargetDragEvent e) {
		TabDndGhostPane pane = dnd.getDndGhostPane();
		if (pane == null || e.getDropTargetContext().getComponent() != pane) {
			return;
		}
		Transferable t = e.getTransferable();
		DataFlavor[] f = e.getCurrentDataFlavors();
		if (t.isDataFlavorSupported(f[0])) {
			e.acceptDrag(e.getDropAction());
		} else {
			e.rejectDrag();
		}
	}

	@Override
	public void dragExit(DropTargetEvent e) {
		TabDndGhostPane pane = dnd.getDndGhostPane();
		if (pane == null || e.getDropTargetContext().getComponent() != pane) {
			return;
		}
		pane.setPoint(HIDDEN_POINT);
		pane.setTargetRect(0, 0, 0, 0);
		pane.repaint();
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent e) {
	}

	@Override
	public void dragOver(DropTargetDragEvent e) {
		TabDndGhostPane pane = dnd.getDndGhostPane();
		if (pane == null || e.getDropTargetContext().getComponent() != pane) {
			return;
		}
		Point glassPt = e.getLocation();
		dnd.updateTargetMark(dnd.getTargetTabIndex(glassPt));
		dnd.scrollIfNeeded(glassPt); // backward and forward scrolling
		pane.setPoint(glassPt);
		pane.repaint();
	}

	@Override
	public void drop(DropTargetDropEvent e) {
		TabDndGhostPane pane = dnd.getDndGhostPane();
		if (pane == null || e.getDropTargetContext().getComponent() != pane) {
			return;
		}
		Transferable t = e.getTransferable();
		DataFlavor[] f = t.getTransferDataFlavors();
		int oldIdx = dnd.dragTabIndex;
		int newIdx = dnd.getTargetTabIndex(e.getLocation());
		if (t.isDataFlavorSupported(f[0]) && oldIdx != newIdx) {
			dnd.swapTabs(oldIdx, newIdx);
			e.dropComplete(true);
		} else {
			e.dropComplete(false);
		}
		pane.setVisible(false);
	}
}
