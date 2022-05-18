package jadx.gui.ui.panel;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import jadx.gui.jobs.ITaskProgress;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public class ProgressPanel extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = -3238438119672015733L;

	private static final Icon ICON_CANCEL = UiUtils.openSvgIcon("ui/close");

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

		cancelButton = new JButton(ICON_CANCEL);
		cancelButton.setPreferredSize(new Dimension(ICON_CANCEL.getIconWidth(), ICON_CANCEL.getIconHeight()));
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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
			case "progress":
				setProgress((Integer) evt.getNewValue());
				break;

			case "label":
				setLabel((String) evt.getNewValue());
				break;

			case "visible":
				setVisible(((Boolean) evt.getNewValue()));
				break;

			case "indeterminate":
				setIndeterminate(((Boolean) evt.getNewValue()));
				break;

			case "cancel-visible":
				cancelButton.setVisible(((Boolean) evt.getNewValue()));
				break;
		}
	}

	public void setLabel(String label) {
		progressLabel.setText(label);
	}

	public void setIndeterminate(boolean newValue) {
		progressBar.setIndeterminate(newValue);
	}

	public void changeLabel(SwingWorker<?, ?> task, String label) {
		task.firePropertyChange("label", null, label);
	}

	public void changeIndeterminate(SwingWorker<?, ?> task, boolean indeterminate) {
		task.firePropertyChange("indeterminate", null, indeterminate);
	}

	public void changeVisibility(SwingWorker<?, ?> task, boolean visible) {
		task.firePropertyChange("visible", null, visible);
	}

	public void changeCancelBtnVisible(SwingWorker<?, ?> task, boolean visible) {
		task.firePropertyChange("cancel-visible", null, visible);
	}
}
