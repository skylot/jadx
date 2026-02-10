package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaPackage;
import jadx.gui.strings.pkg.PackageFilter;
import jadx.gui.strings.pkg.PackageMatchType;
import jadx.gui.strings.pkg.StringPackageFilter;
import jadx.gui.utils.NLS;

public class StringsPackageFilterDialog extends JDialog {

	public static final List<PackageFilter> STRINGS_PACKAGE_FILTERS;

	private static final String[] COLUMN_NAMES = new String[] { "Rule Type", "Rule" };
	private static final int RULE_COLUMN_INDEX = 0;
	private static final int PACKAGE_COLUMN_INDEX = 1;

	public static boolean isPackageAllowed(final JavaPackage pkg) {
		boolean matched = false;
		for (final PackageFilter filter : STRINGS_PACKAGE_FILTERS) {
			if (filter.doesMatch(pkg)) {
				matched = true;
				break;
			}
		}
		return !matched;
	}

	private final PackageFiltersUpdatedAction onPackageFiltersUpdated;

	private JTable filterTable;
	private FilterTableModel filterModel;
	private JButton deleteFilterButton;

	public StringsPackageFilterDialog(final JFrame owner, final PackageFiltersUpdatedAction onUpdateAction) {
		super(owner, "String Search Package Filter", false);

		this.onPackageFiltersUpdated = onUpdateAction;

		setLocationRelativeTo(owner);
		setSize(600, 400);
		initUI();
	}

	private void initUI() {
		JButton addFilterButton = new JButton(NLS.str("common_dialog.add"));
		addFilterButton.addActionListener(e -> {
			AddEntryModal addEntryModal = new AddEntryModal();
			addEntryModal.setVisible(true);
			PackageFilter filter = addEntryModal.createFilterFromOptions();
			if (filter != null) {
				STRINGS_PACKAGE_FILTERS.add(filter);
				final int newPackageIndex = STRINGS_PACKAGE_FILTERS.size() - 1;
				this.filterModel.fireTableRowsInserted(newPackageIndex, newPackageIndex);
				onPackageFiltersUpdated.executeHandler(filter, PackageFilterUpdateType.ADDED);
			}
		});
		deleteFilterButton = new JButton(NLS.str("popup.delete"));
		deleteFilterButton.setEnabled(false);
		deleteFilterButton.addActionListener(e -> {
			final int index = this.filterTable.getSelectedRow();
			if (index != -1) {
				final PackageFilter filter = STRINGS_PACKAGE_FILTERS.get(index);
				STRINGS_PACKAGE_FILTERS.remove(filter);
				this.filterModel.fireTableRowsDeleted(index, index);
				onPackageFiltersUpdated.executeHandler(filter, PackageFilterUpdateType.DELETED);
			}
		});
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(addFilterButton);
		buttonsPanel.add(deleteFilterButton);

		initFilterTable();

		JScrollPane scroll = new JScrollPane(this.filterTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(buttonsPanel, BorderLayout.PAGE_END);
		contentPanel.add(scroll, BorderLayout.CENTER);

		getContentPane().add(contentPanel);
	}

	private void initFilterTable() {
		this.filterModel = new FilterTableModel();

		this.filterTable = new JTable(this.filterModel);
		this.filterTable.setShowHorizontalLines(false);
		this.filterTable.setDragEnabled(false);
		this.filterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.filterTable.setColumnSelectionAllowed(false);
		this.filterTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.filterTable.setAutoscrolls(false);

		this.filterTable.getSelectionModel().addListSelectionListener(e -> {
			final boolean hasSelectedRow = this.filterTable.getSelectedRow() != -1;
			this.deleteFilterButton.setEnabled(hasSelectedRow);
		});
	}

	public enum PackageFilterUpdateType {
		ADDED,
		DELETED,
		EDITED
	}

	@FunctionalInterface
	public interface PackageFiltersUpdatedAction {
		public abstract void executeHandler(PackageFilter packageFilter, PackageFilterUpdateType updateType);
	}

	private final class FilterTableModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			return STRINGS_PACKAGE_FILTERS.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		@Override
		public String getColumnName(int index) {
			return COLUMN_NAMES[index];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			final PackageFilter filter = STRINGS_PACKAGE_FILTERS.get(rowIndex);
			final Object val;
			switch (columnIndex) {
				case RULE_COLUMN_INDEX:
					val = filter.getMatchType();
					break;
				case PACKAGE_COLUMN_INDEX:
					val = filter.getConfiguration();
					break;
				default:
					val = "NO OBJECT";
					break;
			}
			return val;
		}
	}

	private final class AddEntryModal extends JDialog {

		private transient boolean cancelled = false;
		private transient JTextField packageNameField;
		private transient JComboBox<PackageMatchType> matchTypeSelection;

		public AddEntryModal() {
			this(NLS.str("popup.exclude_packages"), "", PackageMatchType.CONTAINS);
		}

		public AddEntryModal(String title, String packageBoxText, PackageMatchType defaultMatchType) {
			super(StringsPackageFilterDialog.this, title, true);

			setLocationRelativeTo(StringsPackageFilterDialog.this);
			setSize(600, 100);
			initUI(packageBoxText, defaultMatchType);
		}

		private void initUI(String packageBoxText, PackageMatchType defaultMatchType) {

			JButton confirmButton = new JButton(NLS.str("common_dialog.ok"));
			confirmButton.addActionListener(ev -> {
				setVisible(false);
			});
			JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
			cancelButton.addActionListener((ev) -> {
				this.cancelled = true;
				setVisible(false);
			});

			JPanel buttonsPanel = new JPanel();
			buttonsPanel.add(confirmButton);
			buttonsPanel.add(cancelButton);

			JPanel infoPanel = new JPanel();
			JLabel info = new JLabel(NLS.str("strings.exclude_msg"));
			matchTypeSelection = new JComboBox<>(PackageMatchType.values());
			matchTypeSelection.setSelectedItem(defaultMatchType);
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));
			infoPanel.add(info);
			infoPanel.add(matchTypeSelection);

			JPanel searchButtonsPanel = new JPanel();
			searchButtonsPanel.setLayout(new BorderLayout(5, 5));

			JPanel inputPanel = new JPanel();
			packageNameField = new JTextField(packageBoxText);
			inputPanel.setLayout(new BorderLayout(5, 5));
			inputPanel.add(packageNameField, BorderLayout.CENTER);
			inputPanel.add(searchButtonsPanel, BorderLayout.LINE_END);

			JPanel contentPanel = new JPanel();
			contentPanel.setLayout(new GridLayout(3, 1));
			contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			contentPanel.add(infoPanel);
			contentPanel.add(inputPanel);
			contentPanel.add(buttonsPanel);

			getContentPane().add(contentPanel);
		}

		@Nullable
		private PackageFilter createFilterFromOptions() {
			if (this.cancelled) {
				return null;
			}

			return new StringPackageFilter(packageNameField.getText(), (PackageMatchType) matchTypeSelection.getSelectedItem());
		}
	}

	static {
		STRINGS_PACKAGE_FILTERS = new ArrayList<>();
		STRINGS_PACKAGE_FILTERS.add(new StringPackageFilter("android", PackageMatchType.STARTS_WITH));
		STRINGS_PACKAGE_FILTERS.add(new StringPackageFilter("androidx", PackageMatchType.STARTS_WITH));
		STRINGS_PACKAGE_FILTERS.add(new StringPackageFilter("com.android", PackageMatchType.STARTS_WITH));
		STRINGS_PACKAGE_FILTERS.add(new StringPackageFilter("com.google", PackageMatchType.STARTS_WITH));
	}
}
