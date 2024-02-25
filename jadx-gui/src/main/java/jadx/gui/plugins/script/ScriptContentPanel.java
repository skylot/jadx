package jadx.gui.plugins.script;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.script.experimental.api.ScriptDiagnostic;
import kotlin.script.experimental.api.ScriptDiagnostic.Severity;

import jadx.gui.JadxWrapper;
import jadx.gui.logs.LogOptions;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.treemodel.JInputScript;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.SearchBar;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;
import jadx.plugins.script.ide.ScriptAnalyzeResult;
import jadx.plugins.script.ide.ScriptServices;

import static jadx.plugins.script.runtime.ScriptRuntime.JADX_SCRIPT_LOG_PREFIX;

public class ScriptContentPanel extends AbstractCodeContentPanel {
	private static final long serialVersionUID = 6575696321112417513L;

	private final ScriptCodeArea scriptArea;
	private final SearchBar searchBar;
	private final RTextScrollPane codeScrollPane;
	private final JPanel actionPanel;
	private final JLabel resultLabel;
	private final ScriptErrorService errorService;
	private final Logger scriptLog;

	public ScriptContentPanel(TabbedPane panel, JInputScript scriptNode) {
		super(panel, scriptNode);
		scriptArea = new ScriptCodeArea(this, scriptNode);
		resultLabel = new NodeLabel("");
		errorService = new ScriptErrorService(scriptArea);
		actionPanel = buildScriptActionsPanel();
		searchBar = new SearchBar(scriptArea);
		codeScrollPane = new RTextScrollPane(scriptArea);
		scriptLog = LoggerFactory.getLogger(JADX_SCRIPT_LOG_PREFIX + scriptNode.getName());

		initUI();
		applySettings();
		scriptArea.load();
	}

	private void initUI() {
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		topPanel.add(actionPanel, BorderLayout.NORTH);
		topPanel.add(searchBar, BorderLayout.SOUTH);

		JPanel codePanel = new JPanel(new BorderLayout());
		codePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		codePanel.add(codeScrollPane);
		codePanel.add(new ErrorStrip(scriptArea), BorderLayout.LINE_END);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		add(topPanel, BorderLayout.NORTH);
		add(codeScrollPane, BorderLayout.CENTER);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, UiUtils.ctrlButton());
		UiUtils.addKeyBinding(scriptArea, key, "SearchAction", searchBar::toggle);
	}

	private JPanel buildScriptActionsPanel() {
		JadxGuiAction runAction = new JadxGuiAction(ActionModel.SCRIPT_RUN, this::runScript);
		JadxGuiAction saveAction = new JadxGuiAction(ActionModel.SCRIPT_SAVE, scriptArea::save);

		runAction.setShortcutComponent(scriptArea);
		saveAction.setShortcutComponent(scriptArea);

		tabbedPane.getMainWindow().getShortcutsController().bindImmediate(runAction);
		tabbedPane.getMainWindow().getShortcutsController().bindImmediate(saveAction);

		JButton save = saveAction.makeButton();
		scriptArea.getScriptNode().addChangeListener(save::setEnabled);

		JButton check = new JButton(NLS.str("script.check"), Icons.CHECK);
		check.addActionListener(ev -> checkScript());
		JButton format = new JButton(NLS.str("script.format"), Icons.FORMAT);
		format.addActionListener(ev -> reformatCode());
		JButton scriptLog = new JButton(NLS.str("script.log"), Icons.FORMAT);
		scriptLog.addActionListener(ev -> showScriptLog());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(new EmptyBorder(0, 0, 0, 0));
		panel.add(runAction.makeButton());
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(save);
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(check);
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(format);
		panel.add(Box.createRigidArea(new Dimension(30, 0)));
		panel.add(resultLabel);
		panel.add(Box.createHorizontalGlue());
		panel.add(scriptLog);
		return panel;
	}

	private void runScript() {
		scriptArea.save();
		if (!checkScript()) {
			return;
		}
		resetResultLabel();

		TabbedPane tabbedPane = getTabbedPane();
		MainWindow mainWindow = tabbedPane.getMainWindow();
		mainWindow.getBackgroundExecutor().execute(NLS.str("script.run"), () -> {
			try {
				JadxWrapper wrapper = mainWindow.getWrapper();
				wrapper.resetGuiPluginsContext();
				wrapper.getDecompiler().reloadPasses();
			} catch (Exception e) {
				scriptLog.error("Passes reload failed", e);
			}
		}, taskStatus -> {
			mainWindow.passesReloaded();
		});
	}

	private boolean checkScript() {
		try {
			resetResultLabel();
			String code = scriptArea.getText();
			String fileName = scriptArea.getNode().getName();

			ScriptServices scriptServices = new ScriptServices();
			ScriptAnalyzeResult result = scriptServices.analyze(fileName, code);
			boolean success = result.getSuccess();
			List<ScriptDiagnostic> issues = result.getIssues();
			for (ScriptDiagnostic issue : issues) {
				Severity severity = issue.getSeverity();
				if (severity == Severity.ERROR || severity == Severity.FATAL) {
					scriptLog.error("{}", issue.render(false, true, true, true));
					success = false;
				} else if (severity == Severity.WARNING) {
					scriptLog.warn("Compile issue: {}", issue);
				}
			}
			List<JadxLintError> lintErrs = Collections.emptyList();
			if (success) {
				lintErrs = getLintIssues(code);
			}

			errorService.clearErrors();
			errorService.addCompilerIssues(issues);
			errorService.addLintErrors(lintErrs);
			if (!success) {
				resultLabel.setText("Compile issues: " + issues.size());
				showScriptLog();
			} else if (!lintErrs.isEmpty()) {
				resultLabel.setText("Lint issues: " + lintErrs.size());
			} else {
				resultLabel.setText("OK");
			}
			errorService.apply();
			return success;
		} catch (Throwable e) {
			scriptLog.error("Failed to check code", e);
			return true;
		}
	}

	private List<JadxLintError> getLintIssues(String code) {
		try {
			List<JadxLintError> lintErrs = KtLintUtils.INSTANCE.lint(code);
			for (JadxLintError error : lintErrs) {
				scriptLog.warn("Lint issue: {} ({}:{})(ruleId={})",
						error.getDetail(), error.getLine(), error.getCol(), error.getRuleId());
			}
			return lintErrs;
		} catch (Throwable e) { // can throw initialization error
			scriptLog.warn("KtLint failed", e);
			return Collections.emptyList();
		}
	}

	private void reformatCode() {
		resetResultLabel();
		try {
			String code = scriptArea.getText();
			String formattedCode = KtLintUtils.INSTANCE.format(code);
			if (!code.equals(formattedCode)) {
				scriptArea.updateCode(formattedCode);
				resultLabel.setText("Code updated");
				errorService.clearErrors();
			}
		} catch (Throwable e) { // can throw initialization error
			scriptLog.error("Failed to reformat code", e);
		}
	}

	private void resetResultLabel() {
		resultLabel.setText("");
	}

	private void applySettings() {
		JadxSettings settings = getSettings();
		codeScrollPane.setLineNumbersEnabled(settings.getLineNumbersMode() != LineNumbersMode.DISABLE);
		codeScrollPane.getGutter().setLineNumberFont(settings.getFont());
		scriptArea.loadSettings();
	}

	private void showScriptLog() {
		getTabbedPane().getMainWindow().showLogViewer(LogOptions.forScript(getNode().getName()));
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		return scriptArea;
	}

	@Override
	public void loadSettings() {
		applySettings();
		updateUI();
	}

	public void dispose() {
		scriptArea.dispose();
	}
}
