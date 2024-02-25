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
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;

public class TabDndGestureListener implements DragGestureListener {

	private final transient TabDndController dnd;

	public TabDndGestureListener(TabDndController dnd) {
		this.dnd = dnd;
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent e) {
		Point tabPt = getDragOrigin(e);
		if (!dnd.onStartDrag(tabPt)) {
			return;
		}
		try {
			e.startDrag(DragSource.DefaultMoveDrop, new TabDndTransferable(), new TabDndSourceListener(dnd));
		} catch (InvalidDnDOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected Point getDragOrigin(DragGestureEvent e) {
		return e.getDragOrigin();
	}
}
