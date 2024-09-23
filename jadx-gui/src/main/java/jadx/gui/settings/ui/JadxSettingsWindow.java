package jadx.gui.settings.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
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
import jadx.api.JadxDecompiler;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.IntegerFormat;
import jadx.api.args.ResourceNameSource;
import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.settings.JadxUpdateChannel;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.settings.XposedCodegenLanguage;
import jadx.gui.settings.ui.cache.CacheSettingsGroup;
import jadx.gui.settings.ui.plugins.PluginSettings;
import jadx.gui.settings.ui.shortcut.ShortcutsSettingsGroup;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.ui.tab.dnd.TabDndGhostType;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ActionHandler;
import jadx.gui.utils.ui.DocumentUpdateListener;

public class JadxSettingsWindow extends JDialog {
	private static final long serialVersionUID = -1804570470377354148L;

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsWindow.class);

	private final transient MainWindow mainWindow;
	private final transient JadxSettings settings;
	private final transient String startSettings;
	private final transient String startSettingsHash;
	private final transient LangLocale prevLang;

	private transient boolean needReload = false;
	private transient SettingsTree tree;

	public JadxSettingsWindow(MainWindow mainWindow, JadxSettings settings) {
		this.mainWindow = mainWindow;
		this.settings = settings;
		this.startSettings = JadxSettingsAdapter.makeString(settings);
		this.startSettingsHash = calcSettingsHash();
		this.prevLang = settings.getLangLocale();

		initUI();

		setTitle(NLS.str("preferences.title"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		UiUtils.setWindowIcons(this);
		setLocationRelativeTo(null);
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(700, 800);
		}
		mainWindow.events().addListener(JadxEvents.RELOAD_SETTINGS_WINDOW, r -> UiUtils.uiRun(this::reloadUI));
		mainWindow.events().addListener(JadxEvents.RELOAD_PROJECT, r -> UiUtils.uiRun(this::reloadUI));
	}

	private void reloadUI() {
		int[] selection = tree.getSelectionRows();
		getContentPane().removeAll();
		initUI();
		// wait for other events to process
		UiUtils.uiRun(() -> {
			tree.setSelectionRows(selection);
			SwingUtilities.updateComponentTreeUI(this);
		});
	}

	private void initUI() {
		JPanel wrapGroupPanel = new JPanel(new BorderLayout(10, 10));

		List<ISettingsGroup> groups = new ArrayList<>();
		groups.add(makeDecompilationGroup());
		groups.add(makeDeobfuscationGroup());
		groups.add(makeRenameGroup());
		groups.add(new CacheSettingsGroup(this));
		groups.add(makeAppearanceGroup());
		groups.add(new ShortcutsSettingsGroup(this, settings));
		groups.add(makeSearchResGroup());
		groups.add(makeProjectGroup());
		groups.add(new PluginSettings(mainWindow, settings).build());
		groups.add(makeOtherGroup());

		tree = new SettingsTree();
		tree.init(wrapGroupPanel, groups);
		JScrollPane leftPane = new JScrollPane(tree);
		leftPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 3, 3));

		JScrollPane rightPane = new JScrollPane(wrapGroupPanel);
		rightPane.getVerticalScrollBar().setUnitIncrement(16);
		rightPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rightPane.setBorder(BorderFactory.createEmptyBorder(10, 3, 3, 10));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		splitPane.setLeftComponent(leftPane);
		splitPane.setRightComponent(rightPane);

		Container contentPane = getContentPane();
		contentPane.add(splitPane, BorderLayout.CENTER);
		contentPane.add(buildButtonsPane(), BorderLayout.PAGE_END);

		KeyStroke strokeEsc = KeyStroke.getKeyStroke("ESCAPE");
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(strokeEsc, "ESCAPE");
		getRootPane().getActionMap().put("ESCAPE", new ActionHandler(this::cancel));
	}

	private JPanel buildButtonsPane() {
		JButton saveBtn = new JButton(NLS.str("preferences.save"));
		saveBtn.addActionListener(event -> save());

		JButton cancelButton = new JButton(NLS.str("preferences.cancel"));
		cancelButton.addActionListener(event -> cancel());

		JButton resetBtn = new JButton(NLS.str("preferences.reset"));
		resetBtn.addActionListener(event -> reset());

		JButton copyBtn = new JButton(NLS.str("preferences.copy"));
		copyBtn.addActionListener(event -> copySettings());

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonPane.add(resetBtn);
		buttonPane.add(copyBtn);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(saveBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		getRootPane().setDefaultButton(saveBtn);
		return buttonPane;
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

		JButton editWhitelistedEntities = new JButton(NLS.str("preferences.excludedPackages.button"));
		editWhitelistedEntities.addActionListener(event -> {
			String prevWhitelistedEntities = settings.getDeobfuscationWhitelistStr();
			String result = JOptionPane.showInputDialog(this,
					NLS.str("preferences.deobfuscation_whitelist.editDialog"),
					prevWhitelistedEntities);
			if (result != null) {
				settings.setDeobfuscationWhitelistStr(result);
				if (!prevWhitelistedEntities.equals(result)) {
					needReload();
				}
			}
		});

		SettingsGroup deobfGroup = new SettingsGroup(NLS.str("preferences.deobfuscation"));
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_on"), deobfOn);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_min_len"), minLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_max_len"), maxLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_res_name_source"), resNamesSource);
		deobfGroup.addRow(NLS.str("preferences.generated_renames_mapping_file_mode"), generatedRenamesMappingFileModeCB);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_whitelist"),
				NLS.str("preferences.deobfuscation_whitelist.tooltip"), editWhitelistedEntities);

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

		JComboBox<UseSourceNameAsClassNameAlias> useSourceNameAsClassNameAlias = new JComboBox<>(UseSourceNameAsClassNameAlias.values());
		useSourceNameAsClassNameAlias.setSelectedItem(settings.getUseSourceNameAsClassNameAlias());
		useSourceNameAsClassNameAlias.addActionListener(e -> {
			settings.setUseSourceNameAsClassNameAlias((UseSourceNameAsClassNameAlias) useSourceNameAsClassNameAlias.getSelectedItem());
			needReload();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.rename"));
		group.addRow(NLS.str("preferences.rename_case"), renameCaseSensitive);
		group.addRow(NLS.str("preferences.rename_valid"), renameValid);
		group.addRow(NLS.str("preferences.rename_printable"), renamePrintable);
		group.addRow(NLS.str("preferences.rename_use_source_name_as_class_name_alias"), useSourceNameAsClassNameAlias);
		return group;
	}

	private void enableComponentList(Collection<JComponent> connectedComponents, boolean enabled) {
		connectedComponents.forEach(comp -> comp.setEnabled(enabled));
	}

	private SettingsGroup makeProjectGroup() {
		JComboBox<JadxSettings.SAVEOPTION> dropdown = new JComboBox<>(JadxSettings.SAVEOPTION.values());
		dropdown.setSelectedItem(settings.getSaveOption());
		dropdown.addActionListener(e -> {
			settings.setSaveOption((JadxSettings.SAVEOPTION) dropdown.getSelectedItem());
			needReload();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.project"));
		group.addRow(NLS.str("preferences.saveOption"), dropdown);

		return group;
	}

	private SettingsGroup makeAppearanceGroup() {
		JComboBox<LangLocale> languageCbx = new JComboBox<>(NLS.getLangLocales());
		for (LangLocale locale : NLS.getLangLocales()) {
			if (locale.equals(settings.getLangLocale())) {
				languageCbx.setSelectedItem(locale);
				break;
			}
		}
		languageCbx.addActionListener(e -> settings.setLangLocale((LangLocale) languageCbx.getSelectedItem()));

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
		group.addRow(NLS.str("preferences.language"), languageCbx);
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

		JComboBox<TabDndGhostType> tabDndGhostTypeCbx = new JComboBox<>(TabDndGhostType.values());
		tabDndGhostTypeCbx.setSelectedItem(settings.getTabDndGhostType());
		tabDndGhostTypeCbx.addActionListener(e -> {
			settings.setTabDndGhostType((TabDndGhostType) tabDndGhostTypeCbx.getSelectedItem());
			mainWindow.loadSettings();
		});
		group.addRow(NLS.str("preferences.tab_dnd_appearance"), tabDndGhostTypeCbx);

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

		JCheckBox moveInnerClasses = new JCheckBox();
		moveInnerClasses.setSelected(settings.isMoveInnerClasses());
		moveInnerClasses.addItemListener(e -> {
			settings.setMoveInnerClasses(e.getStateChange() == ItemEvent.SELECTED);
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

		JComboBox<IntegerFormat> integerFormat = new JComboBox<>(IntegerFormat.values());
		integerFormat.setSelectedItem(settings.getIntegerFormat());
		integerFormat.addActionListener(e -> {
			settings.setIntegerFormat((IntegerFormat) integerFormat.getSelectedItem());
			needReload();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.decompile"));
		other.addRow(NLS.str("preferences.threads"), threadsCount);
		other.addRow(NLS.str("preferences.excludedPackages"),
				NLS.str("preferences.excludedPackages.tooltip"), editExcludedPackages);
		other.addRow(NLS.str("preferences.start_jobs"), autoStartJobs);
		other.addRow(NLS.str("preferences.decompilationMode"), decompilationModeComboBox);
		other.addRow(NLS.str("preferences.showInconsistentCode"), showInconsistentCode);
		other.addRow(NLS.str("preferences.escapeUnicode"), escapeUnicode);
		other.addRow(NLS.str("preferences.replaceConsts"), replaceConsts);
		other.addRow(NLS.str("preferences.respectBytecodeAccessModifiers"), respectBytecodeAccessModifiers);
		other.addRow(NLS.str("preferences.useImports"), useImports);
		other.addRow(NLS.str("preferences.useDebugInfo"), useDebugInfo);
		other.addRow(NLS.str("preferences.inlineAnonymous"), inlineAnonymous);
		other.addRow(NLS.str("preferences.inlineMethods"), inlineMethods);
		other.addRow(NLS.str("preferences.inlineKotlinLambdas"), inlineKotlinLambdas);
		other.addRow(NLS.str("preferences.moveInnerClasses"), moveInnerClasses);
		other.addRow(NLS.str("preferences.extractFinally"), extractFinally);
		other.addRow(NLS.str("preferences.fsCaseSensitive"), fsCaseSensitive);
		other.addRow(NLS.str("preferences.useDx"), useDx);
		other.addRow(NLS.str("preferences.skipResourcesDecode"), resourceDecode);
		other.addRow(NLS.str("preferences.useKotlinMethodsForVarNames"), kotlinRenameVars);
		other.addRow(NLS.str("preferences.commentsLevel"), commentsLevel);
		other.addRow(NLS.str("preferences.integerFormat"), integerFormat);
		return other;
	}

	private SettingsGroup makeOtherGroup() {
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

		JComboBox<XposedCodegenLanguage> xposedCodegenLanguage =
				new JComboBox<>(XposedCodegenLanguage.getEntries().toArray(new XposedCodegenLanguage[0]));
		xposedCodegenLanguage.setSelectedItem(settings.getXposedCodegenLanguage());
		xposedCodegenLanguage.addActionListener(e -> {
			settings.setXposedCodegenLanguage((XposedCodegenLanguage) xposedCodegenLanguage.getSelectedItem());
			mainWindow.loadSettings();
		});

		JComboBox<JadxUpdateChannel> updateChannel =
				new JComboBox<>(JadxUpdateChannel.getEntries().toArray(new JadxUpdateChannel[0]));
		updateChannel.setSelectedItem(settings.getJadxUpdateChannel());
		updateChannel.addActionListener(e -> {
			settings.setJadxUpdateChannel((JadxUpdateChannel) updateChannel.getSelectedItem());
			mainWindow.loadSettings();
		});

		SettingsGroup group = new SettingsGroup(NLS.str("preferences.other"));
		group.addRow(NLS.str("preferences.lineNumbersMode"), lineNumbersMode);
		group.addRow(NLS.str("preferences.jumpOnDoubleClick"), jumpOnDoubleClick);
		group.addRow(NLS.str("preferences.useAlternativeFileDialog"), useAltFileDialog);
		group.addRow(NLS.str("preferences.check_for_updates"), update);
		group.addRow(NLS.str("preferences.cfg"), cfg);
		group.addRow(NLS.str("preferences.raw_cfg"), rawCfg);
		group.addRow(NLS.str("preferences.xposed_codegen_language"), xposedCodegenLanguage);
		group.addRow(NLS.str("preferences.update_channel"), updateChannel);
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

	private void save() {
		settings.sync();
		enableComponents(this, false);
		SwingUtilities.invokeLater(() -> {
			if (shouldReload()) {
				mainWindow.getShortcutsController().loadSettings();
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
	}

	private void cancel() {
		JadxSettingsAdapter.fill(settings, startSettings);
		mainWindow.loadSettings();
		dispose();
	}

	private void reset() {
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
	}

	private void copySettings() {
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
	}

	public void needReload() {
		needReload = true;
	}

	private boolean shouldReload() {
		return needReload || !startSettingsHash.equals(calcSettingsHash());
	}

	@SuppressWarnings("resource")
	private String calcSettingsHash() {
		JadxDecompiler decompiler = mainWindow.getWrapper().getCurrentDecompiler().orElse(null);
		return settings.toJadxArgs().makeCodeArgsHash(decompiler);
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}
}
