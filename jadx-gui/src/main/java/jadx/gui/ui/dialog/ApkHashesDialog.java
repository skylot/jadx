package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.Nullable;

import jadx.gui.links.ApkHashStore;
import jadx.gui.links.ApkHashStore.Entry;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * List of all opened APK files with their hashes.
 */
public class ApkHashesDialog extends CommonDialog {
	private static final long serialVersionUID = 4861543716723318549L;

	private final transient ApkHashStore store;
	private transient JTable table;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public ApkHashesDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.store = mainWindow.getLinkController().getStore();
		initUI();
		commonWindowInit();
	}

	private void initUI() {
		setTitle(NLS.str("apk_hashes.title"));
		String[] columns = { NLS.str("apk_hashes.col_date"), NLS.str("apk_hashes.col_path"), NLS.str("apk_hashes.col_hash") };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		for (Entry e : store.entries()) {
			model.addRow(new Object[] { dateFormat.format(new Date(e.getLastOpened())), e.getPath(), e.getHash() });
		}
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JButton openBtn = new JButton(NLS.str("apk_hashes.open"));
		openBtn.addActionListener(e -> openSelected());
		JButton copyBtn = new JButton(NLS.str("apk_hashes.copy_hash"));
		copyBtn.addActionListener(e -> {
			String hash = valueAt(2);
			if (hash != null) {
				UiUtils.copyToClipboard(hash);
			}
		});
		JButton closeBtn = new JButton(NLS.str("common_dialog.close"));
		closeBtn.addActionListener(e -> dispose());

		JPanel buttons = new JPanel();
		buttons.add(openBtn);
		buttons.add(copyBtn);
		buttons.add(closeBtn);

		JPanel content = new JPanel(new BorderLayout(5, 5));
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(content);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private @Nullable String valueAt(int column) {
		int row = table.getSelectedRow();
		return row == -1 ? null : (String) table.getValueAt(row, column);
	}

	private void openSelected() {
		String pathStr = valueAt(1);
		if (pathStr == null) {
			return;
		}
		Path path = Path.of(pathStr);
		if (!Files.isRegularFile(path)) {
			UiUtils.errorMessage(this, NLS.str("apk_hashes.file_missing", pathStr));
			return;
		}
		dispose();
		mainWindow.open(path);
	}
}
