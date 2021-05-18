package jadx.gui.settings;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import say.swing.JFontChooser;

import jadx.api.JadxArgs;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JadxSettingsWindow extends JDialog {
	private static final long serialVersionUID = -1804570470377354148L;

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsWindow.class);

	private final transient MainWindow mainWindow;
	private final transient JadxSettings settings;
	private final transient String startSettings;
	private final transient LangLocale prevLang;

	private transient boolean needReload = false;

	public JadxSettingsWindow(MainWindow mainWindow, JadxSettings settings) {
		this.mainWindow = mainWindow;
		this.settings = settings;
		this.startSettings = JadxSettingsAdapter.makeString(settings);
		this.prevLang = settings.getLangLocale();

		initUI();

		setTitle(NLS.str("preferences.title"));
		setSize(400, 550);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		UiUtils.setWindowIcons(this);
		setLocationRelativeTo(null);
	}

	private void initUI() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel leftPanel = new JPanel();
		JPanel rightPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
		panel.add(leftPanel);
		panel.add(rightPanel);

		leftPanel.add(makeDeobfuscationGroup());
		leftPanel.add(makeRenameGroup());
		leftPanel.add(makeProjectGroup());
		leftPanel.add(makeEditorGroup());
		leftPanel.add(makeOtherGroup());
		leftPanel.add(makeSearchResGroup());

		rightPanel.add(makeDecompilationGroup());

		JButton saveBtn = new JButton(NLS.str("preferences.save"));
		saveBtn.addActionListener(event -> {
			settings.sync();
			enableComponents(this, false);

			SwingUtilities.invokeLater(() -> {
				if (needReload) {
					mainWindow.reOpenFile();
				}
				if (!settings.getLangLocale().equals(prevLang)) {
					JOptionPane.showMessageDialog(
							this,
							NLS.str("msg.language_changed", settings.getLangLocale()),
							NLS.str("msg.language_changed_title", settings.getLangLocale()),
							JOptionPane.INFORMATION_MESSAGE);
				}
				dispose();
			});
		});
		JButton cancelButton = new JButton(NLS.str("preferences.cancel"));
		cancelButton.addActionListener(event -> cancel());

		JButton resetBtn = new JButton(NLS.str("preferences.reset"));
		resetBtn.addActionListener(event -> {
			int res = JOptionPane.showConfirmDialog(
					JadxSettingsWindow.this,
					NLS.str("preferences.reset_message"),
					NLS.str("preferences.reset_title"),
					JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				String defaults = JadxSettingsAdapter.makeString(JadxSettings.makeDefault());
				JadxSettingsAdapter.fill(settings, defaults);
				mainWindow.loadSettings();
				needReload();
				getContentPane().removeAll();
				initUI();
				pack();
				repaint();
			}
		});

		JButton copyBtn = new JButton(NLS.str("preferences.copy"));
		copyBtn.addActionListener(event -> {

			JsonObject settingsJson = JadxSettingsAdapter.makeJsonObject(this.settings);
			// remove irrelevant preferences
			settingsJson.remove("windowPos");
			settingsJson.remove("mainWindowExtendedState");
			settingsJson.remove("lastSaveProjectPath");
			settingsJson.remove("lastOpenFilePath");
			settingsJson.remove("lastSaveFilePath");
			settingsJson.remove("recentProjects");
			String settingsText = new GsonBuilder().setPrettyPrinting().create().toJson(settingsJson);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(settingsText);
			clipboard.setContents(selection, selection);
			JOptionPane.showMessageDialog(
					JadxSettingsWindow.this,
					NLS.str("preferences.copy_message"));
		});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonPane.add(resetBtn);
		buttonPane.add(copyBtn);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(saveBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = getContentPane();
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		getRootPane().setDefaultButton(saveBtn);

		KeyStroke strokeEsc = KeyStroke.getKeyStroke("ESCAPE");
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(strokeEsc, "ESCAPE");
		getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
	}

	private void cancel() {
		JadxSettingsAdapter.fill(settings, startSettings);
		mainWindow.loadSettings();
		dispose();
	}

	private static void enableComponents(Container container, boolean enable) {
		for (Component component : container.getComponents()) {
			if (component instanceof Container) {
				enableComponents((Container) component, enable);
			}
			component.setEnabled(enable);
		}
	}

	private SettingsGroup makeDeobfuscationGroup() {
		JCheckBox deobfOn = new JCheckBox();
		deobfOn.setSelected(settings.isDeobfuscationOn());
		deobfOn.addItemListener(e -> {
			settings.setDeobfuscationOn(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox deobfForce = new JCheckBox();
		deobfForce.setSelected(settings.isDeobfuscationForceSave());
		deobfForce.addItemListener(e -> {
			settings.setDeobfuscationForceSave(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SpinnerNumberModel minLenModel = new SpinnerNumberModel(settings.getDeobfuscationMinLength(), 0, Integer.MAX_VALUE, 1);
		JSpinner minLenSpinner = new JSpinner(minLenModel);
		minLenSpinner.addChangeListener(e -> {
			settings.setDeobfuscationMinLength((Integer) minLenSpinner.getValue());
			needReload();
		});

		SpinnerNumberModel maxLenModel = new SpinnerNumberModel(settings.getDeobfuscationMaxLength(), 0, Integer.MAX_VALUE, 1);
		JSpinner maxLenSpinner = new JSpinner(maxLenModel);
		maxLenSpinner.addChangeListener(e -> {
			settings.setDeobfuscationMaxLength((Integer) maxLenSpinner.getValue());
			needReload();
		});

		JCheckBox deobfSourceAlias = new JCheckBox();
		deobfSourceAlias.setSelected(settings.isDeobfuscationUseSourceNameAsAlias());
		deobfSourceAlias.addItemListener(e -> {
			settings.setDeobfuscationUseSourceNameAsAlias(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox deobfKotlinMetadata = new JCheckBox();
		deobfKotlinMetadata.setSelected(settings.isDeobfuscationParseKotlinMetadata());
		deobfKotlinMetadata.addItemListener(e -> {
			settings.setDeobfuscationParseKotlinMetadata(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup deobfGroup = new SettingsGroup(NLS.str("preferences.deobfuscation"));
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_on"), deobfOn);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_force"), deobfForce);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_min_len"), minLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_max_len"), maxLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_source_alias"), deobfSourceAlias);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_kotlin_metadata"), deobfKotlinMetadata);
		deobfGroup.end();

		Collection<JComponent> connectedComponents =
				Arrays.asList(deobfForce, minLenSpinner, maxLenSpinner, deobfSourceAlias, deobfKotlinMetadata);
		deobfOn.addItemListener(e -> enableComponentList(connectedComponents, e.getStateChange() == ItemEvent.SELECTED));
		enableComponentList(connectedComponents, settings.isDeobfuscationOn());
		return deobfGroup;
	}

	private SettingsGroup makeRenameGroup() {
		JCheckBox renameCaseSensitive = new JCheckBox();
		renameCaseSensitive.setSelected(settings.isRenameCaseSensitive());
		renameCaseSensitive.addItemListener(e -> {
			settings.updateRenameFlag(JadxArgs.RenameEnum.CASE, e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox renameValid = new JCheckBox();
		renameValid.setSelected(settings.isRenameValid());
		renameValid.addItemListener(e -> {
			settings.updateRenameFlag(JadxArgs.RenameEnum.VALID, e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox renamePrintable = new JCheckBox();
		renamePrintable.setSelected(settings.isRenamePrintable());
		renamePrintable.addItemListener(e -> {
			settings.updateRenameFlag(JadxArgs.RenameEnum.PRINTABLE, e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.rename"));
		group.addRow(NLS.str("preferences.rename_case"), renameCaseSensitive);
		group.addRow(NLS.str("preferences.rename_valid"), renameValid);
		group.addRow(NLS.str("preferences.rename_printable"), renamePrintable);
		return group;
	}

	private void enableComponentList(Collection<JComponent> connectedComponents, boolean enabled) {
		connectedComponents.forEach(comp -> comp.setEnabled(enabled));
	}

	private SettingsGroup makeProjectGroup() {
		JCheckBox autoSave = new JCheckBox();
		autoSave.setSelected(settings.isAutoSaveProject());
		autoSave.addItemListener(e -> settings.setAutoSaveProject(e.getStateChange() == ItemEvent.SELECTED));

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.project"));
		group.addRow(NLS.str("preferences.autoSave"), autoSave);

		return group;
	}

	private SettingsGroup makeEditorGroup() {
		JButton fontBtn = new JButton(NLS.str("preferences.select_font"));
		JButton smaliFontBtn = new JButton(NLS.str("preferences.select_smali_font"));

		EditorTheme[] editorThemes = EditorTheme.getAllThemes();
		JComboBox<EditorTheme> themesCbx = new JComboBox<>(editorThemes);
		for (EditorTheme theme : editorThemes) {
			if (theme.getPath().equals(settings.getEditorThemePath())) {
				themesCbx.setSelectedItem(theme);
				break;
			}
		}
		themesCbx.addActionListener(e -> {
			int i = themesCbx.getSelectedIndex();
			EditorTheme editorTheme = editorThemes[i];
			settings.setEditorThemePath(editorTheme.getPath());
			mainWindow.loadSettings();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.editor"));
		JLabel fontLabel = group.addRow(getFontLabelStr(), fontBtn);
		group.addRow(NLS.str("preferences.theme"), themesCbx);
		JLabel smaliFontLabel = group.addRow(getSmaliFontLabelStr(), smaliFontBtn);

		fontBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				fontChooser.setSelectedFont(settings.getFont());
				int result = fontChooser.showDialog(JadxSettingsWindow.this);
				if (result == JFontChooser.OK_OPTION) {
					Font font = fontChooser.getSelectedFont();
					LOG.debug("Selected Font: {}", font);
					settings.setFont(font);
					mainWindow.loadSettings();
					fontLabel.setText(getFontLabelStr());
				}
			}
		});

		smaliFontBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFontChooser fontChooser = new JPreferredFontChooser();
				fontChooser.setSelectedFont(settings.getSmaliFont());
				int result = fontChooser.showDialog(JadxSettingsWindow.this);
				if (result == JFontChooser.OK_OPTION) {
					Font font = fontChooser.getSelectedFont();
					LOG.debug("Selected Font: {} for smali", font);
					settings.setSmaliFont(font);
					mainWindow.loadSettings();
					smaliFontLabel.setText(getSmaliFontLabelStr());
				}
			}
		});
		return group;
	}

	private String getFontLabelStr() {
		Font font = settings.getFont();
		String fontStyleName = FontUtils.convertFontStyleToString(font.getStyle());
		return NLS.str("preferences.font") + ": " + font.getFontName() + ' ' + fontStyleName + ' ' + font.getSize();
	}

	private String getSmaliFontLabelStr() {
		Font font = settings.getSmaliFont();
		String fontStyleName = FontUtils.convertFontStyleToString(font.getStyle());
		return NLS.str("preferences.smali_font") + ": " + font.getFontName() + ' ' + fontStyleName + ' ' + font.getSize();
	}

	private SettingsGroup makeDecompilationGroup() {
		JCheckBox fallback = new JCheckBox();
		fallback.setSelected(settings.isFallbackMode());
		fallback.addItemListener(e -> {
			settings.setFallbackMode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox showInconsistentCode = new JCheckBox();
		showInconsistentCode.setSelected(settings.isShowInconsistentCode());
		showInconsistentCode.addItemListener(e -> {
			settings.setShowInconsistentCode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox resourceDecode = new JCheckBox();
		resourceDecode.setSelected(settings.isSkipResources());
		resourceDecode.addItemListener(e -> {
			settings.setSkipResources(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
				settings.getThreadsCount(), 1, Runtime.getRuntime().availableProcessors() * 2, 1);
		JSpinner threadsCount = new JSpinner(spinnerModel);
		threadsCount.addChangeListener(e -> {
			settings.setThreadsCount((Integer) threadsCount.getValue());
			needReload();
		});

		JButton editExcludedPackages = new JButton(NLS.str("preferences.excludedPackages.button"));
		editExcludedPackages.addActionListener(event -> {

			String oldExcludedPackages = settings.getExcludedPackages();
			String result = JOptionPane.showInputDialog(this, NLS.str("preferences.excludedPackages.editDialog"),
					settings.getExcludedPackages());
			if (result != null) {
				settings.setExcludedPackages(result);
				if (!oldExcludedPackages.equals(result)) {
					needReload();
				}
			}
		});

		JCheckBox autoStartJobs = new JCheckBox();
		autoStartJobs.setSelected(settings.isAutoStartJobs());
		autoStartJobs.addItemListener(e -> settings.setAutoStartJobs(e.getStateChange() == ItemEvent.SELECTED));

		JCheckBox escapeUnicode = new JCheckBox();
		escapeUnicode.setSelected(settings.isEscapeUnicode());
		escapeUnicode.addItemListener(e -> {
			settings.setEscapeUnicode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox replaceConsts = new JCheckBox();
		replaceConsts.setSelected(settings.isReplaceConsts());
		replaceConsts.addItemListener(e -> {
			settings.setReplaceConsts(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox respectBytecodeAccessModifiers = new JCheckBox();
		respectBytecodeAccessModifiers.setSelected(settings.isRespectBytecodeAccessModifiers());
		respectBytecodeAccessModifiers.addItemListener(e -> {
			settings.setRespectBytecodeAccessModifiers(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox useImports = new JCheckBox();
		useImports.setSelected(settings.isUseImports());
		useImports.addItemListener(e -> {
			settings.setUseImports(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox inlineAnonymous = new JCheckBox();
		inlineAnonymous.setSelected(settings.isInlineAnonymousClasses());
		inlineAnonymous.addItemListener(e -> {
			settings.setInlineAnonymousClasses(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox inlineMethods = new JCheckBox();
		inlineMethods.setSelected(settings.isInlineMethods());
		inlineMethods.addItemListener(e -> {
			settings.setInlineMethods(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox fsCaseSensitive = new JCheckBox();
		fsCaseSensitive.setSelected(settings.isFsCaseSensitive());
		fsCaseSensitive.addItemListener(e -> {
			settings.setFsCaseSensitive(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.decompile"));
		other.addRow(NLS.str("preferences.threads"), threadsCount);
		other.addRow(NLS.str("preferences.excludedPackages"), NLS.str("preferences.excludedPackages.tooltip"),
				editExcludedPackages);
		other.addRow(NLS.str("preferences.start_jobs"), autoStartJobs);
		other.addRow(NLS.str("preferences.showInconsistentCode"), showInconsistentCode);
		other.addRow(NLS.str("preferences.escapeUnicode"), escapeUnicode);
		other.addRow(NLS.str("preferences.replaceConsts"), replaceConsts);
		other.addRow(NLS.str("preferences.respectBytecodeAccessModifiers"), respectBytecodeAccessModifiers);
		other.addRow(NLS.str("preferences.useImports"), useImports);
		other.addRow(NLS.str("preferences.inlineAnonymous"), inlineAnonymous);
		other.addRow(NLS.str("preferences.inlineMethods"), inlineMethods);
		other.addRow(NLS.str("preferences.fsCaseSensitive"), fsCaseSensitive);
		other.addRow(NLS.str("preferences.fallback"), fallback);
		other.addRow(NLS.str("preferences.skipResourcesDecode"), resourceDecode);
		return other;
	}

	private SettingsGroup makeOtherGroup() {
		JComboBox<LangLocale> languageCbx = new JComboBox<>(NLS.getLangLocales());
		for (LangLocale locale : NLS.getLangLocales()) {
			if (locale.equals(settings.getLangLocale())) {
				languageCbx.setSelectedItem(locale);
				break;
			}
		}
		languageCbx.addActionListener(e -> settings.setLangLocale((LangLocale) languageCbx.getSelectedItem()));

		JCheckBox update = new JCheckBox();
		update.setSelected(settings.isCheckForUpdates());
		update.addItemListener(e -> settings.setCheckForUpdates(e.getStateChange() == ItemEvent.SELECTED));

		JCheckBox cfg = new JCheckBox();
		cfg.setSelected(settings.isCfgOutput());
		cfg.addItemListener(e -> {
			settings.setCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox rawCfg = new JCheckBox();
		rawCfg.setSelected(settings.isRawCfgOutput());
		rawCfg.addItemListener(e -> {
			settings.setRawCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.other"));
		group.addRow(NLS.str("preferences.language"), languageCbx);
		group.addRow(NLS.str("preferences.check_for_updates"), update);
		group.addRow(NLS.str("preferences.cfg"), cfg);
		group.addRow(NLS.str("preferences.raw_cfg"), rawCfg);
		return group;
	}

	private SettingsGroup makeSearchResGroup() {
		SettingsGroup group = new SettingsGroup(NLS.str("preferences.search_res_title"));
		int prevSize = settings.getSrhResourceSkipSize();
		String prevExts = settings.getSrhResourceFileExt();
		SpinnerNumberModel sizeLimitModel = new SpinnerNumberModel(prevSize,
				0, Integer.MAX_VALUE, 1);
		JSpinner spinner = new JSpinner(sizeLimitModel);
		JTextField fileExtField = new JTextField();
		group.addRow(NLS.str("preferences.res_skip_file"), spinner);
		group.addRow(NLS.str("preferences.res_file_ext"), fileExtField);

		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int size = (Integer) spinner.getValue();
				settings.setSrhResourceSkipSize(size);
			}
		});

		fileExtField.getDocument().addDocumentListener(new DocumentListener() {
			private void update() {
				String ext = fileExtField.getText();
				settings.setSrhResourceFileExt(ext);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});
		fileExtField.setText(prevExts);

		return group;
	}

	private void needReload() {
		needReload = true;
	}

	private static class SettingsGroup extends JPanel {
		private static final long serialVersionUID = -6487309975896192544L;

		private final GridBagConstraints c;
		private int row;

		public SettingsGroup(String title) {
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
	}

	private static class JPreferredFontChooser extends JFontChooser {
		private static final String[] PREFERRED_FONTS = new String[] {
				"Monospaced", "Consolas", "Courier", "Courier New",
				"Lucida Sans Typewriter", "Lucida Console",
				"SimSun", "SimHei",
		};

		private String[] filteredFonts;

		@Override
		protected String[] getFontFamilies() {
			if (filteredFonts == null) {
				GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
				Set<String> fontSet = new HashSet<>();
				Collections.addAll(fontSet, env.getAvailableFontFamilyNames());
				ArrayList<String> found = new ArrayList<>(PREFERRED_FONTS.length);
				for (String font : PREFERRED_FONTS) {
					if (fontSet.contains(font)) {
						found.add(font);
					}
				}
				if (found.size() == PREFERRED_FONTS.length) {
					filteredFonts = PREFERRED_FONTS;
				} else if (found.size() > 0) {
					filteredFonts = new String[found.size()];
					for (int i = 0; i < found.size(); i++) {
						filteredFonts[i] = found.get(i);
					}
				} else {
					// this machine is crazy.
					LOG.warn("Can't found any preferred fonts for smali, use all available.");
					filteredFonts = env.getAvailableFontFamilyNames();
				}
			}
			return filteredFonts;
		}
	}
}
