package jadx.gui.settings.ui.cache;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class CachesTableRenderer implements TableCellRenderer {

	private final JLabel label;

	public CachesTableRenderer() {
		label = new JLabel();
		label.setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		TableRow obj = (TableRow) value;
		switch (column) {
			case 0:
				label.setText(obj.getProject());
				break;
			case 1:
				label.setText(obj.getUsage());
				break;
		}
		label.setToolTipText(obj.getCacheEntry().getCache());

		if (obj.isSelected()) {
			label.setBackground(table.getSelectionBackground());
			label.setForeground(table.getSelectionForeground());
		} else {
			label.setBackground(table.getBackground());
			label.setForeground(table.getForeground());
		}
		return label;
	}
}
