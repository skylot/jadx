package jadx.gui.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.files.FileUtils;

/**
 * Enables drop support from external applications for the {@link MainWindow} (load dropped APK
 * file)
 */
public class MainDropTarget implements DropTargetListener {

	private static final Logger LOG = LoggerFactory.getLogger(MainDropTarget.class);

	private final MainWindow mainWindow;

	public MainDropTarget(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	protected void processDrag(DropTargetDragEvent dtde) {
		if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
		} else {
			dtde.rejectDrag();
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		processDrag(dtde);
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		processDrag(dtde);
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent dtde) {
		if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.rejectDrop();
			return;
		}
		dtde.acceptDrop(dtde.getDropAction());
		try {
			Transferable transferable = dtde.getTransferable();
			List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
			if (!transferData.isEmpty()) {
				dtde.dropComplete(true);
				mainWindow.open(FileUtils.toPaths(transferData));
			}
		} catch (Exception e) {
			LOG.error("File drop operation failed", e);
		}
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}
}
