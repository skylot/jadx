package jadx.gui.settings.ui;

import java.awt.BorderLayout;
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
	private final String title;
	private final JPanel panel;
	private final JPanel gridPanel;
	private final GridBagConstraints c;
	private int row;

	public SettingsGroup(String title) {
		this.title = title;
		gridPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.weighty = 1.0;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(title));
		panel.add(gridPanel, BorderLayout.PAGE_START);
	}

	public JLabel addRow(String label, JComponent comp) {
		return addRow(label, null, comp);
	}

	public JLabel addRow(String label, String tooltip, JComponent comp) {
		JLabel rowLbl = new JLabel(label);
		rowLbl.setLabelFor(comp);
		rowLbl.setHorizontalAlignment(SwingConstants.LEFT);
		if (tooltip != null) {
			rowLbl.setToolTipText(tooltip);
			comp.setToolTipText(tooltip);
		} else {
			comp.setToolTipText(label);
		}
		comp.getAccessibleContext().setAccessibleName(label);

		c.gridy = row++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0.1;
		c.fill = GridBagConstraints.LINE_START;
		gridPanel.add(rowLbl, c);
		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0.7;
		c.fill = GridBagConstraints.LINE_START;

		gridPanel.add(comp, c);
		comp.addPropertyChangeListener("enabled", evt -> rowLbl.setEnabled((boolean) evt.getNewValue()));
		return rowLbl;
	}

	public void end() {
		gridPanel.add(Box.createVerticalGlue());
	}

	@Override
	public JComponent buildComponent() {
		return panel;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public JPanel getGridPanel() {
		return gridPanel;
	}

	@Override
	public String toString() {
		return title;
	}
}
