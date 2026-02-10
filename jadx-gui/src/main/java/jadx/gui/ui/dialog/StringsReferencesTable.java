package jadx.gui.ui.dialog;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.jetbrains.annotations.Nullable;

import jadx.gui.strings.SingleStringResult;
import jadx.gui.strings.StringResult;

public class StringsReferencesTable extends JTable {

	private static final int CODE_NODE_COLUMN_INDEX = 0;

	private final JNodeNavigationAction jnodeNavigationAction;
	private final TableCellRenderer renderer;

	public StringsReferencesTable(final JFrame owner, final TableCellRenderer renderer, final List<StringResult> results,
			final int rowHeight, final JNodeNavigationAction jnodeNavigationAction) {
		super(new StringsReferencesModel(results));

		this.renderer = renderer;
		this.jnodeNavigationAction = jnodeNavigationAction;
		this.rowHeight = rowHeight;

		setRowHeight(rowHeight);
		getColumnModel().getColumn(CODE_NODE_COLUMN_INDEX).setCellRenderer(this.renderer);

		setShowHorizontalLines(false);
		setDragEnabled(false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setColumnSelectionAllowed(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setAutoscrolls(false);

		setSelectionBackground(selectionBackground);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					openSelectedItem();
				}
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedItem();
				}
			}
		});
	}

	protected void openSelectedItem() {
		final StringResult node = getSelectedNode();
		if (node == null) {
			return;
		}

		jnodeNavigationAction.navigateToNode(node);
	}

	@Nullable
	private StringResult getSelectedNode() {
		try {
			int selectedId = getSelectedRow();
			if (selectedId == -1 || selectedId >= getRowCount()) {
				return null;
			}
			return (StringResult) getValueAt(convertRowIndexToModel(selectedId), CODE_NODE_COLUMN_INDEX);
		} catch (Exception e) {
			return null;
		}
	}

	private static final class StringsReferencesModel extends AbstractTableModel {

		private final List<StringResult> results;

		public StringsReferencesModel(List<StringResult> results) {
			this.results = results;
		}

		@Override
		public int getRowCount() {
			return results.size();
		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			final SingleStringResult single = (SingleStringResult) results.get(rowIndex);
			return single.getUnderlyingNode();
		}
	}

	@FunctionalInterface
	public interface JNodeNavigationAction {
		public abstract void navigateToNode(final StringResult node);
	}
}
