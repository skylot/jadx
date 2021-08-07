package jadx.gui.ui;

import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

class TabDropTargetAdapter extends DropTargetAdapter {
	private void clearDropLocationPaint(Component c) {
		if (c instanceof DnDTabbedPane) {
			DnDTabbedPane t = (DnDTabbedPane) c;
			t.updateTabDropLocation(null, null, false);
			t.setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		Component c = dtde.getDropTargetContext().getComponent();
		// System.out.println("DropTargetListener#drop: " + c.getName());
		clearDropLocationPaint(c);
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		Component c = dte.getDropTargetContext().getComponent();
		// System.out.println("DropTargetListener#dragExit: " + c.getName());
		clearDropLocationPaint(c);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		Component c = dtde.getDropTargetContext().getComponent();
		// System.out.println("DropTargetListener#dragEnter: " + c.getName());
	}

	// @Override public void dragOver(DropTargetDragEvent dtde) {
	// // //System.out.println("dragOver");
	// }

	// @Override public void dropActionChanged(DropTargetDragEvent dtde) {
	// //System.out.println("dropActionChanged");
	// }
}
