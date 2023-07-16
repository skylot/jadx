package jadx.gui.settings.ui.cache;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import jadx.gui.utils.NLS;

public class CachesTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -7725573085995496397L;

	private static final String[] COLUMN_NAMES = {
			NLS.str("preferences.cache.table.project"),
			NLS.str("preferences.cache.table.size")
	};

	private transient List<TableRow> rows = Collections.emptyList();

	public void setRows(List<TableRow> list) {
		this.rows = list;
	}

	public List<TableRow> getRows() {
		return rows;
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int index) {
		return COLUMN_NAMES[index];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return TableRow.class;
	}

	@Override
	public TableRow getValueAt(int rowIndex, int columnIndex) {
		return rows.get(rowIndex);
	}

	public void changeSelection(int idx) {
		TableRow row = rows.get(idx);
		row.setSelected(!row.isSelected());
	}
}
