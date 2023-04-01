package jadx.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import say.swing.JFontChooser;

import jadx.api.CommentsLevel;
import jadx.api.DecompilationMode;
import jadx.api.JadxArgs;
import jadx.api.JadxArgs.UseKotlinMethodsForVarNames;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.ResourceNameSource;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.OptionDescription.OptionFlag;
import jadx.core.plugins.PluginContext;
import jadx.gui.cache.code.CodeCacheMode;
import jadx.gui.cache.usage.UsageCacheMode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.plugins.CollectPluginOptions;
import jadx.gui.utils.ui.DocumentUpdateListener;

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
		leftPanel.add(makeAppearanceGroup());
		leftPanel.add(makeOtherGroup());
		leftPanel.add(makeSearchResGroup());
		leftPanel.add(Box.createVerticalGlue());

		rightPanel.add(makeDecompilationGroup());
		rightPanel.add(makePluginOptionsGroup());
		rightPanel.add(Box.createVerticalGlue());

		JButton saveBtn = new JButton(NLS.str("preferences.save"));
		saveBtn.addActionListener(event -> {
			settings.sync();
			enableComponents(this, false);

			SwingUtilities.invokeLater(() -> {
				if (needReload) {
					mainWindow.reopen();
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

		JComboBox<ResourceNameSource> resNamesSource = new JComboBox<>(ResourceNameSource.values());
		resNamesSource.setSelectedItem(settings.getResourceNameSource());
		resNamesSource.addActionListener(e -> {
			settings.setResourceNameSource((ResourceNameSource) resNamesSource.getSelectedItem());
			needReload();
		});

		JComboBox<GeneratedRenamesMappingFileMode> generatedRenamesMappingFileModeCB =
				new JComboBox<>(GeneratedRenamesMappingFileMode.values());
		generatedRenamesMappingFileModeCB.setSelectedItem(settings.getGeneratedRenamesMappingFileMode());
		generatedRenamesMappingFileModeCB.addActionListener(e -> {
			GeneratedRenamesMappingFileMode newValue =
					(GeneratedRenamesMappingFileMode) generatedRenamesMappingFileModeCB.getSelectedItem();
			if (newValue != settings.getGeneratedRenamesMappingFileMode()) {
				settings.setGeneratedRenamesMappingFileMode(newValue);
				needReload();
			}
		});

		SettingsGroup deobfGroup = new SettingsGroup(NLS.str("preferences.deobfuscation"));
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_on"), deobfOn);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_min_len"), minLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_max_len"), maxLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_res_name_source"), resNamesSource);
		deobfGroup.addRow(NLS.str("preferences.generated_renames_mapping_file_mode"), generatedRenamesMappingFileModeCB);
		deobfGroup.end();

		Collection<JComponent> connectedComponents = Arrays.asList(minLenSpinner, maxLenSpinner);
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

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.rename"));
		group.addRow(NLS.str("preferences.rename_case"), renameCaseSensitive);
		group.addRow(NLS.str("preferences.rename_valid"), renameValid);
		group.addRow(NLS.str("preferences.rename_printable"), renamePrintable);
		group.addRow(NLS.str("preferences.deobfuscation_source_alias"), deobfSourceAlias);
		group.addRow(NLS.str("preferences.deobfuscation_kotlin_metadata"), deobfKotlinMetadata);
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

	private SettingsGroup makeAppearanceGroup() {
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

		JComboBox<String> lafCbx = new JComboBox<>(LafManager.getThemes());
		lafCbx.setSelectedItem(settings.getLafTheme());
		lafCbx.addActionListener(e -> {
			settings.setLafTheme((String) lafCbx.getSelectedItem());
			mainWindow.loadSettings();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.appearance"));
		group.addRow(NLS.str("preferences.laf_theme"), lafCbx);
		group.addRow(NLS.str("preferences.theme"), themesCbx);
		JLabel fontLabel = group.addRow(getFontLabelStr(), fontBtn);
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
		JCheckBox useDx = new JCheckBox();
		useDx.setSelected(settings.isUseDx());
		useDx.addItemListener(e -> {
			settings.setUseDx(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JComboBox<DecompilationMode> decompilationModeComboBox = new JComboBox<>(DecompilationMode.values());
		decompilationModeComboBox.setSelectedItem(settings.getDecompilationMode());
		decompilationModeComboBox.addActionListener(e -> {
			settings.setDecompilationMode((DecompilationMode) decompilationModeComboBox.getSelectedItem());
			needReload();
		});

		JComboBox<CodeCacheMode> codeCacheModeComboBox = new JComboBox<>(CodeCacheMode.values());
		codeCacheModeComboBox.setSelectedItem(settings.getCodeCacheMode());
		codeCacheModeComboBox.addActionListener(e -> {
			settings.setCodeCacheMode((CodeCacheMode) codeCacheModeComboBox.getSelectedItem());
			needReload();
		});
		String codeCacheModeToolTip = CodeCacheMode.buildToolTip();

		JComboBox<UsageCacheMode> usageCacheModeComboBox = new JComboBox<>(UsageCacheMode.values());
		usageCacheModeComboBox.setSelectedItem(settings.getUsageCacheMode());
		usageCacheModeComboBox.addActionListener(e -> {
			settings.setUsageCacheMode((UsageCacheMode) usageCacheModeComboBox.getSelectedItem());
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

		// fix for #1331
		int threadsCountValue = settings.getThreadsCount();
		int threadsCountMax = Math.max(2, Math.max(threadsCountValue, Runtime.getRuntime().availableProcessors() * 2));
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(threadsCountValue, 1, threadsCountMax, 1);
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

		JCheckBox useDebugInfo = new JCheckBox();
		useDebugInfo.setSelected(settings.isDebugInfo());
		useDebugInfo.addItemListener(e -> {
			settings.setDebugInfo(e.getStateChange() == ItemEvent.SELECTED);
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

		JCheckBox inlineKotlinLambdas = new JCheckBox();
		inlineKotlinLambdas.setSelected(settings.isAllowInlineKotlinLambda());
		inlineKotlinLambdas.addItemListener(e -> {
			settings.setAllowInlineKotlinLambda(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox extractFinally = new JCheckBox();
		extractFinally.setSelected(settings.isExtractFinally());
		extractFinally.addItemListener(e -> {
			settings.setExtractFinally(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox fsCaseSensitive = new JCheckBox();
		fsCaseSensitive.setSelected(settings.isFsCaseSensitive());
		fsCaseSensitive.addItemListener(e -> {
			settings.setFsCaseSensitive(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JComboBox<UseKotlinMethodsForVarNames> kotlinRenameVars = new JComboBox<>(UseKotlinMethodsForVarNames.values());
		kotlinRenameVars.setSelectedItem(settings.getUseKotlinMethodsForVarNames());
		kotlinRenameVars.addActionListener(e -> {
			settings.setUseKotlinMethodsForVarNames((UseKotlinMethodsForVarNames) kotlinRenameVars.getSelectedItem());
			needReload();
		});

		JComboBox<CommentsLevel> commentsLevel = new JComboBox<>(CommentsLevel.values());
		commentsLevel.setSelectedItem(settings.getCommentsLevel());
		commentsLevel.addActionListener(e -> {
			settings.setCommentsLevel((CommentsLevel) commentsLevel.getSelectedItem());
			needReload();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.decompile"));
		other.addRow(NLS.str("preferences.threads"), threadsCount);
		other.addRow(NLS.str("preferences.excludedPackages"),
				NLS.str("preferences.excludedPackages.tooltip"), editExcludedPackages);
		other.addRow(NLS.str("preferences.start_jobs"), autoStartJobs);
		other.addRow(NLS.str("preferences.decompilationMode"), decompilationModeComboBox);
		other.addRow(NLS.str("preferences.codeCacheMode"), codeCacheModeToolTip, codeCacheModeComboBox);
		other.addRow(NLS.str("preferences.usageCacheMode"), usageCacheModeComboBox);
		other.addRow(NLS.str("preferences.showInconsistentCode"), showInconsistentCode);
		other.addRow(NLS.str("preferences.escapeUnicode"), escapeUnicode);
		other.addRow(NLS.str("preferences.replaceConsts"), replaceConsts);
		other.addRow(NLS.str("preferences.respectBytecodeAccessModifiers"), respectBytecodeAccessModifiers);
		other.addRow(NLS.str("preferences.useImports"), useImports);
		other.addRow(NLS.str("preferences.useDebugInfo"), useDebugInfo);
		other.addRow(NLS.str("preferences.inlineAnonymous"), inlineAnonymous);
		other.addRow(NLS.str("preferences.inlineMethods"), inlineMethods);
		other.addRow(NLS.str("preferences.inlineKotlinLambdas"), inlineKotlinLambdas);
		other.addRow(NLS.str("preferences.extractFinally"), extractFinally);
		other.addRow(NLS.str("preferences.fsCaseSensitive"), fsCaseSensitive);
		other.addRow(NLS.str("preferences.useDx"), useDx);
		other.addRow(NLS.str("preferences.skipResourcesDecode"), resourceDecode);
		other.addRow(NLS.str("preferences.useKotlinMethodsForVarNames"), kotlinRenameVars);
		other.addRow(NLS.str("preferences.commentsLevel"), commentsLevel);
		return other;
	}

	private SettingsGroup makePluginOptionsGroup() {
		SettingsGroup pluginsGroup = new SettingsGroup(NLS.str("preferences.plugins"));
		List<PluginContext> list = new CollectPluginOptions(mainWindow.getWrapper()).build();
		for (PluginContext context : list) {
			addPluginOptions(pluginsGroup, context);
		}
		return pluginsGroup;
	}

	private void addPluginOptions(SettingsGroup pluginsGroup, PluginContext context) {
		JadxPluginOptions options = context.getOptions();
		if (options == null) {
			return;
		}
		String pluginId = context.getPluginId();
		for (OptionDescription opt : options.getOptionsDescriptions()) {
			if (opt.getFlags().contains(OptionFlag.HIDE_IN_GUI)) {
				continue;
			}
			String optName = opt.name();
			String title;
			if (pluginId.equals("jadx-script")) {
				title = '[' + optName.replace("jadx-script.", "script:") + "] " + opt.description();
			} else {
				title = '[' + pluginId + "]  " + opt.description();
			}
			Consumer<String> updateFunc;
			String curValue;
			if (opt.getFlags().contains(OptionFlag.PER_PROJECT)) {
				JadxProject project = mainWindow.getProject();
				updateFunc = value -> project.updatePluginOptions(m -> m.put(optName, value));
				curValue = project.getPluginOption(optName);
			} else {
				Map<String, String> optionsMap = settings.getPluginOptions();
				updateFunc = value -> optionsMap.put(optName, value);
				curValue = optionsMap.get(optName);
			}
			String value = curValue != null ? curValue : opt.defaultValue();

			if (opt.values().isEmpty() || opt.getType() == OptionDescription.OptionType.BOOLEAN) {
				try {
					pluginsGroup.addRow(title, getPluginOptionEditor(opt, value, updateFunc));
				} catch (Exception e) {
					LOG.error("Failed to add editor for plugin option: {}", optName, e);
				}
			} else {
				JComboBox<String> combo = new JComboBox<>(opt.values().toArray(new String[0]));
				combo.setSelectedItem(value);
				combo.addActionListener(e -> {
					updateFunc.accept((String) combo.getSelectedItem());
					needReload();
				});
				pluginsGroup.addRow(title, combo);
			}
		}
	}

	private JComponent getPluginOptionEditor(OptionDescription opt, String value, Consumer<String> updateFunc) {
		switch (opt.getType()) {
			case STRING:
				JTextField textField = new JTextField();
				textField.setText(value == null ? "" : value);
				textField.getDocument().addDocumentListener(new DocumentUpdateListener(event -> {
					updateFunc.accept(textField.getText());
					needReload();
				}));
				return textField;

			case NUMBER:
				JSpinner numberField = new JSpinner();
				numberField.setValue(safeStringToInt(value, 0));
				numberField.addChangeListener(e -> {
					updateFunc.accept(numberField.getValue().toString());
					needReload();
				});
				return numberField;

			case BOOLEAN:
				JCheckBox boolField = new JCheckBox();
				boolField.setSelected(Objects.equals(value, "yes") || Objects.equals(value, "true"));
				boolField.addItemListener(e -> {
					boolean editorValue = e.getStateChange() == ItemEvent.SELECTED;
					updateFunc.accept(editorValue ? "yes" : "no");
					needReload();
				});
				return boolField;
		}
		return null;
	}

	private int safeStringToInt(String value, int defValue) {
		if (value == null) {
			return defValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			LOG.warn("Failed parse string to int: {}", value, e);
			return defValue;
		}
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

		JComboBox<LineNumbersMode> lineNumbersMode = new JComboBox<>(LineNumbersMode.values());
		lineNumbersMode.setSelectedItem(settings.getLineNumbersMode());
		lineNumbersMode.addActionListener(e -> {
			settings.setLineNumbersMode((LineNumbersMode) lineNumbersMode.getSelectedItem());
			mainWindow.loadSettings();
		});

		JCheckBox jumpOnDoubleClick = new JCheckBox();
		jumpOnDoubleClick.setSelected(settings.isJumpOnDoubleClick());
		jumpOnDoubleClick.addItemListener(e -> settings.setJumpOnDoubleClick(e.getStateChange() == ItemEvent.SELECTED));

		JCheckBox useAltFileDialog = new JCheckBox();
		useAltFileDialog.setSelected(settings.isUseAlternativeFileDialog());
		useAltFileDialog.addItemListener(e -> settings.setUseAlternativeFileDialog(e.getStateChange() == ItemEvent.SELECTED));

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
		group.addRow(NLS.str("preferences.lineNumbersMode"), lineNumbersMode);
		group.addRow(NLS.str("preferences.jumpOnDoubleClick"), jumpOnDoubleClick);
		group.addRow(NLS.str("preferences.useAlternativeFileDialog"), useAltFileDialog);
		group.addRow(NLS.str("preferences.check_for_updates"), update);
		group.addRow(NLS.str("preferences.cfg"), cfg);
		group.addRow(NLS.str("preferences.raw_cfg"), rawCfg);
		return group;
	}

	private SettingsGroup makeSearchResGroup() {
		JSpinner resultsPerPage = new JSpinner(
				new SpinnerNumberModel(settings.getSearchResultsPerPage(), 0, Integer.MAX_VALUE, 1));
		resultsPerPage.addChangeListener(ev -> settings.setSearchResultsPerPage((Integer) resultsPerPage.getValue()));

		JSpinner sizeLimit = new JSpinner(
				new SpinnerNumberModel(settings.getSrhResourceSkipSize(), 0, Integer.MAX_VALUE, 1));
		sizeLimit.addChangeListener(ev -> settings.setSrhResourceSkipSize((Integer) sizeLimit.getValue()));

		JTextField fileExtField = new JTextField();
		fileExtField.getDocument().addDocumentListener(new DocumentUpdateListener((ev) -> {
			String ext = fileExtField.getText();
			settings.setSrhResourceFileExt(ext);
		}));
		fileExtField.setText(settings.getSrhResourceFileExt());

		SettingsGroup searchGroup = new SettingsGroup(NLS.str("preferences.search_group_title"));
		searchGroup.addRow(NLS.str("preferences.search_results_per_page"), resultsPerPage);
		searchGroup.addRow(NLS.str("preferences.res_skip_file"), sizeLimit);
		searchGroup.addRow(NLS.str("preferences.res_file_ext"), fileExtField);
		return searchGroup;
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
