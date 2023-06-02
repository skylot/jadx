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

import jadx.api.plugins.gui.ISettingsGroup;

public class SettingsGroup implements ISettingsGroup {
	private static final long serialVersionUID = -6487309975896192544L;

	private final String title;
	private final JPanel panel;
	private final GridBagConstraints c;
	private int row;

	public SettingsGroup(String title) {
		this.title = title;
		panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(title));
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
		panel.add(jLabel, c);
		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.2;
		c.fill = GridBagConstraints.HORIZONTAL;

		if (tooltip != null) {
			jLabel.setToolTipText(tooltip);
			comp.setToolTipText(tooltip);
		}

		panel.add(comp, c);

		comp.addPropertyChangeListener("enabled", evt -> jLabel.setEnabled((boolean) evt.getNewValue()));
		return jLabel;
	}

	public void end() {
		panel.add(Box.createVerticalGlue());
	}

	@Override
	public JComponent buildComponent() {
		return panel;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public JPanel getPanel() {
		return panel;
	}

	@Override
	public String toString() {
		return title;
	}
}
