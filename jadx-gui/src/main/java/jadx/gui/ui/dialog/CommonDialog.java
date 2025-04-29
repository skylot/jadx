package jadx.gui.ui.dialog;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public abstract class CommonDialog extends JDialog {
	private static final Logger LOG = LoggerFactory.getLogger(CommonDialog.class);

	protected final MainWindow mainWindow;

	public CommonDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
	}

	protected void commonWindowInit() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		UiUtils.addEscapeShortCutToDispose(this);
		setLocationRelativeTo(null);

		UiUtils.uiRunAndWait(this::pack);
		Dimension minSize = getSize();
		setMinimumSize(minSize);
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(incByPercent(minSize.getWidth(), 30), incByPercent(minSize.getHeight(), 30));
		}
	}

	@Override
	public void dispose() {
		try {
			mainWindow.getSettings().saveWindowPos(this);
		} catch (Exception e) {
			LOG.warn("Failed to save window size and position", e);
		}
		super.dispose();
	}

	private static int incByPercent(double value, int percent) {
		return (int) (value * (1 + percent * 0.01));
	}
}
