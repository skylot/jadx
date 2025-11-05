package jadx.gui.ui.panel;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import jadx.gui.jobs.ITaskProgress;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.UiUtils;

public class ProgressPanel extends JPanel {
	private static final long serialVersionUID = -3238438119672015733L;

	private final JProgressBar progressBar;
	private final JLabel progressLabel;
	private final JButton cancelButton;
	private final boolean showCancelButton;

	public ProgressPanel(final MainWindow mainWindow, boolean showCancelButton) {
		this.showCancelButton = showCancelButton;

		progressLabel = new JLabel();
		progressBar = new JProgressBar(0, 100);
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		progressLabel.setLabelFor(progressBar);

		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setVisible(false);
		add(progressLabel);
		add(progressBar);

		Icon cancelIcon = Icons.ICON_CLOSE;
		cancelButton = new JButton(cancelIcon);
		cancelButton.setPreferredSize(new Dimension(cancelIcon.getIconWidth(), cancelIcon.getIconHeight()));
		cancelButton.setToolTipText("Cancel background jobs");
		cancelButton.setBorderPainted(false);
		cancelButton.setFocusPainted(false);
		cancelButton.setContentAreaFilled(false);
		cancelButton.addActionListener(e -> mainWindow.cancelBackgroundJobs());
		cancelButton.setVisible(showCancelButton);
		add(cancelButton);
	}

	public void reset() {
		cancelButton.setVisible(showCancelButton);
		progressBar.setIndeterminate(true);
		progressBar.setValue(0);
		progressBar.setString("");
		progressBar.setStringPainted(true);
	}

	public void setProgress(ITaskProgress taskProgress) {
		int progress = taskProgress.progress();
		int total = taskProgress.total();
		if (progress == 0 || total == 0) {
			progressBar.setIndeterminate(true);
		} else {
			if (progressBar.isIndeterminate()) {
				progressBar.setIndeterminate(false);
			}
			setProgress(UiUtils.calcProgress(progress, total));
		}
	}

	private void setProgress(int progress) {
		progressBar.setIndeterminate(false);
		progressBar.setValue(progress);
		progressBar.setString(progress + "%");
		progressBar.setStringPainted(true);
	}

	public void setLabel(String label) {
		progressLabel.setText(label);
	}

	public void setIndeterminate(boolean newValue) {
		progressBar.setIndeterminate(newValue);
	}

	public void setCancelButtonVisible(boolean visible) {
		cancelButton.setVisible(visible);
	}
}
