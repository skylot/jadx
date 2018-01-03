package jadx.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import jadx.gui.utils.Utils;

public class ProgressPanel extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = -3238438119672015733L;

	private static final Icon ICON_CANCEL = Utils.openIcon("cross");

	private final JProgressBar progressBar;
	private final JLabel progressLabel;

	public ProgressPanel(final MainWindow mainWindow, boolean showCancelButton) {
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

		if (showCancelButton) {
			JButton cancelButton = new JButton(ICON_CANCEL);
			cancelButton.setPreferredSize(new Dimension(ICON_CANCEL.getIconWidth(), ICON_CANCEL.getIconHeight()));
			cancelButton.setToolTipText("Cancel background jobs");
			cancelButton.setBorderPainted(false);
			cancelButton.setFocusPainted(false);
			cancelButton.setContentAreaFilled(false);
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mainWindow.cancelBackgroundJobs();
				}
			});
			add(cancelButton);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
			progressBar.setString(progress + "%");
			progressBar.setStringPainted(true);
		} else if ("label".equals(evt.getPropertyName())) {
			setLabel((String) evt.getNewValue());
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
}
