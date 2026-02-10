package jadx.gui.settings.ui.cache;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.gui.cache.code.CodeCacheMode;
import jadx.gui.cache.usage.UsageCacheMode;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.settings.ui.SettingsGroup;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.files.JadxFiles;
import jadx.gui.utils.ui.DocumentUpdateListener;

public class CacheSettingsGroup implements ISettingsGroup {

	private final String title = NLS.str("preferences.cache");
	private final JadxSettingsWindow settingsWindow;

	private JTextField customDirField;
	private JButton selectDirBtn;

	public CacheSettingsGroup(JadxSettingsWindow settingsWindow) {
		this.settingsWindow = settingsWindow;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public JComponent buildComponent() {
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
		options.add(buildBaseOptions());
		options.add(buildLocationSelector());

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(options, BorderLayout.PAGE_START);
		mainPanel.add(buildCachesView(), BorderLayout.CENTER);
		return mainPanel;
	}

	private JPanel buildCachesView() {
		CachesTable cachesTable = new CachesTable(settingsWindow.getMainWindow());
		JScrollPane scrollPane = new JScrollPane(cachesTable);
		cachesTable.setFillsViewportHeight(true);
		cachesTable.updateData();

		JButton calcUsage = new JButton(NLS.str("preferences.cache.btn.usage"));
		calcUsage.addActionListener(ev -> cachesTable.updateSizes());

		JButton deleteSelected = new JButton(NLS.str("preferences.cache.btn.delete_selected"));
		deleteSelected.addActionListener(ev -> cachesTable.deleteSelected());

		JButton deleteAll = new JButton(NLS.str("preferences.cache.btn.delete_all"));
		deleteAll.addActionListener(ev -> cachesTable.deleteAll());

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		buttons.add(calcUsage);
		buttons.add(Box.createHorizontalGlue());
		buttons.add(deleteSelected);
		buttons.add(deleteAll);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(NLS.str("preferences.cache.table.title")));
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.PAGE_END);
		return panel;
	}

	private JComponent buildLocationSelector() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(NLS.str("preferences.cache.location")),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		customDirField = new JTextField();
		customDirField.setColumns(10);
		customDirField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> {
			settingsWindow.getMainWindow().getSettings().setCacheDir(customDirField.getText());
		}));

		selectDirBtn = new JButton();
		selectDirBtn.setIcon(UIManager.getIcon("Tree.closedIcon"));
		selectDirBtn.addActionListener(e -> {
			FileDialogWrapper fd = new FileDialogWrapper(settingsWindow.getMainWindow(), FileOpenMode.CUSTOM_OPEN);
			fd.setFileExtList(Collections.emptyList());
			fd.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			List<Path> paths = fd.show();
			if (!paths.isEmpty()) {
				String dir = paths.get(0).toAbsolutePath().toString();
				customDirField.setText(dir);
				settingsWindow.getMainWindow().getSettings().setCacheDir(dir);
			}
		});

		JRadioButton defOpt = new JRadioButton(NLS.str("preferences.cache.location_default"));
		defOpt.setToolTipText(JadxFiles.CACHE_DIR.toString());
		defOpt.addActionListener(e -> changeCacheLocation(null));
		JRadioButton localOpt = new JRadioButton(NLS.str("preferences.cache.location_local"));
		localOpt.addActionListener(e -> changeCacheLocation("."));
		JRadioButton customOpt = new JRadioButton(NLS.str("preferences.cache.location_custom"));
		customOpt.addActionListener(e -> changeCacheLocation(""));

		ButtonGroup group = new ButtonGroup();
		group.add(defOpt);
		group.add(localOpt);
		group.add(customOpt);

		panel.add(defOpt);
		panel.add(localOpt);

		JPanel custom = new JPanel();
		custom.setLayout(new BoxLayout(custom, BoxLayout.LINE_AXIS));
		custom.add(customOpt);
		custom.add(Box.createHorizontalStrut(15));
		custom.add(customDirField);
		custom.add(selectDirBtn);
		panel.add(custom);

		String cacheDir = settingsWindow.getMainWindow().getSettings().getCacheDir();
		if (cacheDir == null) {
			defOpt.setSelected(true);
			changeCacheLocation(null);
		} else if (cacheDir.equals(".")) {
			localOpt.setSelected(true);
			changeCacheLocation(cacheDir);
		} else {
			customOpt.setSelected(true);
			customDirField.setText(cacheDir);
			changeCacheLocation("");
		}
		JLabel notice = new JLabel(NLS.str("preferences.cache.change_notice"));
		notice.setEnabled(false);
		panel.add(notice);
		return panel;
	}

	private void changeCacheLocation(@Nullable String locValue) {
		boolean custom = Objects.equals(locValue, "");
		customDirField.setEnabled(custom);
		selectDirBtn.setEnabled(custom);
		if (!custom) {
			settingsWindow.getMainWindow().getSettings().setCacheDir(locValue);
		}
	}

	private JComponent buildBaseOptions() {
		JadxSettings settings = settingsWindow.getMainWindow().getSettings();

		JComboBox<CodeCacheMode> codeCacheModeComboBox = new JComboBox<>(CodeCacheMode.values());
		codeCacheModeComboBox.setSelectedItem(settings.getCodeCacheMode());
		codeCacheModeComboBox.addActionListener(e -> {
			settings.setCodeCacheMode((CodeCacheMode) codeCacheModeComboBox.getSelectedItem());
			settingsWindow.needReload();
		});

		JComboBox<UsageCacheMode> usageCacheModeComboBox = new JComboBox<>(UsageCacheMode.values());
		usageCacheModeComboBox.setSelectedItem(settings.getUsageCacheMode());
		usageCacheModeComboBox.addActionListener(e -> {
			settings.setUsageCacheMode((UsageCacheMode) usageCacheModeComboBox.getSelectedItem());
			settingsWindow.needReload();
		});

		SettingsGroup group = new SettingsGroup(title);
		group.addRow(NLS.str("preferences.codeCacheMode"), CodeCacheMode.buildToolTip(), codeCacheModeComboBox);
		group.addRow(NLS.str("preferences.usageCacheMode"), usageCacheModeComboBox);
		return group.buildComponent();
	}
}
