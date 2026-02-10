package jadx.plugins.script.kotlin.gui

import jadx.api.plugins.JadxPluginContext
import jadx.gui.logs.LogOptions
import jadx.gui.settings.LineNumbersMode
import jadx.gui.ui.action.ActionModel
import jadx.gui.ui.action.JadxGuiAction
import jadx.gui.ui.codearea.AbstractCodeArea
import jadx.gui.ui.codearea.AbstractCodeContentPanel
import jadx.gui.ui.codearea.SearchBar
import jadx.gui.ui.tab.TabbedPane
import jadx.gui.utils.Icons
import jadx.gui.utils.NLS
import jadx.gui.utils.UiUtils
import jadx.gui.utils.ui.NodeLabel
import jadx.plugins.script.kotlin.ScriptServices
import jadx.plugins.script.kotlin.runtime.JadxScriptData.Companion.JADX_SCRIPT_LOG_PREFIX
import org.fife.ui.rsyntaxtextarea.ErrorStrip
import org.fife.ui.rtextarea.RTextScrollPane
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.border.EmptyBorder
import kotlin.script.experimental.api.ScriptDiagnostic

class ScriptContentPanel(
	private val pluginContext: JadxPluginContext,
	panel: TabbedPane,
	scriptNode: JInputScript,
) : AbstractCodeContentPanel(panel, scriptNode) {
	private val scriptArea: ScriptCodeArea = ScriptCodeArea(this, scriptNode)
	private val searchBar: SearchBar
	private val codeScrollPane: RTextScrollPane
	private val actionPanel: JPanel
	private val resultLabel: JLabel = NodeLabel("")
	private val errorService: ScriptErrorService = ScriptErrorService(scriptArea)
	private val scriptLog: Logger = LoggerFactory.getLogger(JADX_SCRIPT_LOG_PREFIX + scriptNode.name)

	init {
		actionPanel = buildScriptActionsPanel()
		searchBar = SearchBar(scriptArea)
		codeScrollPane = RTextScrollPane(scriptArea)

		initUI()
		applySettings()
		scriptArea.load()
	}

	private fun initUI() {
		val topPanel = JPanel(BorderLayout())
		topPanel.setBorder(EmptyBorder(5, 5, 5, 5))
		topPanel.add(actionPanel, BorderLayout.NORTH)
		topPanel.add(searchBar, BorderLayout.SOUTH)

		val codePanel = JPanel(BorderLayout())
		codePanel.setBorder(EmptyBorder(0, 0, 0, 0))
		codePanel.add(codeScrollPane)
		codePanel.add(ErrorStrip(scriptArea), BorderLayout.LINE_END)

		setLayout(BorderLayout())
		setBorder(EmptyBorder(0, 0, 0, 0))
		add(topPanel, BorderLayout.NORTH)
		add(codeScrollPane, BorderLayout.CENTER)

		val key = KeyStroke.getKeyStroke(KeyEvent.VK_F, UiUtils.ctrlButton())
		UiUtils.addKeyBinding(scriptArea, key, "SearchAction") { searchBar.toggle() }
	}

	private fun buildScriptActionsPanel(): JPanel {
		val runAction = JadxGuiAction(ActionModel.SCRIPT_RUN, Runnable { this.runScript() })
		val saveAction = JadxGuiAction(ActionModel.SCRIPT_SAVE, Runnable { scriptArea.save() })

		runAction.shortcutComponent = scriptArea
		saveAction.shortcutComponent = scriptArea

		tabbedPane.mainWindow.shortcutsController.bindImmediate(runAction)
		tabbedPane.mainWindow.shortcutsController.bindImmediate(saveAction)

		val save = saveAction.makeButton()
		scriptArea.scriptNode.addChangeListener { save.setEnabled(it) }

		val check = JButton(NLS.str("script.check"), Icons.CHECK)
		check.addActionListener { checkScript() }
		val format = JButton(NLS.str("script.format"), Icons.FORMAT)
		format.addActionListener { reformatCode() }
		val scriptLog = JButton(NLS.str("script.log"), Icons.FORMAT)
		scriptLog.addActionListener { showScriptLog() }

		val panel = JPanel()
		panel.setLayout(BoxLayout(panel, BoxLayout.LINE_AXIS))
		panel.setBorder(EmptyBorder(0, 0, 0, 0))
		panel.add(runAction.makeButton())
		panel.add(Box.createRigidArea(Dimension(10, 0)))
		panel.add(save)
		panel.add(Box.createRigidArea(Dimension(10, 0)))
		panel.add(check)
		panel.add(Box.createRigidArea(Dimension(10, 0)))
		panel.add(format)
		panel.add(Box.createRigidArea(Dimension(30, 0)))
		panel.add(resultLabel)
		panel.add(Box.createHorizontalGlue())
		panel.add(scriptLog)
		return panel
	}

	private fun runScript() {
		scriptArea.save()
		if (!checkScript(runScript = true)) {
			return
		}
		resetResultLabel()

		val tabbedPane = getTabbedPane()
		val mainWindow = tabbedPane.mainWindow
		mainWindow.backgroundExecutor.execute(NLS.str("script.run"), {
			try {
				mainWindow.wrapper.reloadPasses()
			} catch (e: Exception) {
				scriptLog.error("Passes reload failed", e)
			}
		}, {
			mainWindow.passesReloaded()
		})
	}

	private fun checkScript(runScript: Boolean = false): Boolean {
		try {
			resetResultLabel()
			val code = scriptArea.getText()

			if (code.contains("@file:DependsOn")) {
				if (!runScript) {
					resultLabel.setText("Checks disabled for scripts with external dependencies")
				}
				return true
			}

			val fileName = scriptArea.getNode().getName()
			val scriptServices = ScriptServices(pluginContext)
			val result = scriptServices.analyze(fileName, code)
			var success = result.success
			val issues: List<ScriptDiagnostic> = result.issues
			for (issue in issues) {
				val severity = issue.severity
				if (severity == ScriptDiagnostic.Severity.ERROR || severity == ScriptDiagnostic.Severity.FATAL) {
					scriptLog.error(
						issue.render(
							withSeverity = false,
							withLocation = true,
							withException = true,
							withStackTrace = true,
						),
					)
					success = false
				} else if (severity == ScriptDiagnostic.Severity.WARNING) {
					scriptLog.warn("Compile issue: {}", issue)
				}
			}
			val lintErrs: List<JadxLintError> = when {
				success -> getLintIssues(code)
				else -> listOf()
			}

			errorService.clearErrors()
			errorService.addCompilerIssues(issues)
			errorService.addLintErrors(lintErrs)
			if (!success) {
				resultLabel.setText("Compile issues: " + issues.size)
				showScriptLog()
			} else if (!lintErrs.isEmpty()) {
				resultLabel.setText("Lint issues: " + lintErrs.size)
			} else {
				resultLabel.setText("OK")
			}
			errorService.apply()
			return success
		} catch (e: Throwable) {
			scriptLog.error("Failed to check code", e)
			return true
		}
	}

	private fun getLintIssues(code: String): List<JadxLintError> {
		try {
			val lintErrs = KtLintUtils.lint(code)
			for (error in lintErrs) {
				scriptLog.warn("Lint issue: {} ({}:{})(ruleId={})", error.detail, error.line, error.col, error.ruleId)
			}
			return lintErrs
		} catch (e: Throwable) { // can throw initialization error
			scriptLog.warn("KtLint failed", e)
			return listOf()
		}
	}

	private fun reformatCode() {
		resetResultLabel()
		try {
			val code = scriptArea.getText()
			val formattedCode = KtLintUtils.format(code)
			if (code != formattedCode) {
				scriptArea.updateCode(formattedCode)
				resultLabel.setText("Code updated")
				errorService.clearErrors()
			}
		} catch (e: Throwable) { // can throw initialization error
			scriptLog.error("Failed to reformat code", e)
		}
	}

	private fun resetResultLabel() {
		resultLabel.setText("")
	}

	private fun applySettings() {
		val settings = getSettings()
		codeScrollPane.setLineNumbersEnabled(settings.lineNumbersMode != LineNumbersMode.DISABLE)
		codeScrollPane.gutter.setLineNumberFont(settings.codeFont)
		scriptArea.loadSettings()
	}

	private fun showScriptLog() {
		mainWindow.showLogViewer(LogOptions.forScript(getNode().getName()))
	}

	override fun getCodeArea(): AbstractCodeArea {
		return scriptArea
	}

	override fun getChildrenComponent(): Component {
		return codeArea
	}

	override fun loadSettings() {
		applySettings()
		updateUI()
	}

	override fun dispose() {
		scriptArea.dispose()
	}
}
