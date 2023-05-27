package jadx.gui.settings.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SettingsGroupPanel extends JPanel {
	private static final long serialVersionUID = -6487309975896192544L;

	private final String title;
	private final GridBagConstraints c;
	private int row;

	public SettingsGroupPanel(String title) {
		this.title = title;
		setBorder(BorderFactory.createTitledBorder(title));
		setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.weighty = 1.0;
	}

	public JLabel addRow(String label, JComponent comp) {
		return addRow(label, null, comp);
	}

	public JLabel addRow(String label, String tooltip, JComponent comp) {
		c.gridy = row++;
		JLabel jLabel = new JLabel(label);
		jLabel.setLabelFor(comp);
		jLabel.setHorizontalAlignment(SwingConstants.LEFT);
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.NONE;
		add(jLabel, c);
		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.2;
		c.fill = GridBagConstraints.HORIZONTAL;

		if (tooltip != null) {
			jLabel.setToolTipText(tooltip);
			comp.setToolTipText(tooltip);
		}

		add(comp, c);

		comp.addPropertyChangeListener("enabled", evt -> jLabel.setEnabled((boolean) evt.getNewValue()));
		return jLabel;
	}

	public void end() {
		add(Box.createVerticalGlue());
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return title;
	}
}
