package jadx.plugins.script.kotlin.gui

import jadx.api.ICodeInfo
import jadx.gui.jobs.IBackgroundTask
import jadx.gui.jobs.LoadTask
import jadx.gui.settings.JadxSettings
import jadx.gui.ui.action.JadxAutoCompletion
import jadx.gui.ui.codearea.AbstractCodeArea
import jadx.gui.ui.panel.ContentPanel
import jadx.gui.utils.shortcut.ShortcutsController
import org.fife.ui.autocomplete.AutoCompletion

class ScriptCodeArea(contentPanel: ContentPanel, val scriptNode: JInputScript) :
	AbstractCodeArea(contentPanel, scriptNode) {
	private val autoCompletion: AutoCompletion
	private val shortcutsController: ShortcutsController

	init {
		setSyntaxEditingStyle(scriptNode.syntaxName)
		isCodeFoldingEnabled = true
		closeCurlyBraces = true

		shortcutsController = contentPanel.mainWindow.shortcutsController
		val settings = contentPanel.mainWindow.settings
		autoCompletion = addAutoComplete(settings)
	}

	private fun addAutoComplete(settings: JadxSettings): AutoCompletion {
		val provider = ScriptCompleteProvider(this, scriptNode.pluginContext)
		provider.setAutoActivationRules(false, ".")
		val ac = JadxAutoCompletion(provider)
		ac.setListCellRenderer(ScriptCompletionRenderer(settings))
		ac.isAutoActivationEnabled = true
		ac.autoCompleteSingleChoices = true
		ac.install(this)
		shortcutsController.bindImmediate(ac)
		return ac
	}

	override fun getCodeInfo(): ICodeInfo {
		return node.codeInfo
	}

	override fun getLoadTask(): IBackgroundTask {
		return LoadTask(
			{ node.codeInfo.getCodeStr() },
			{ code ->
				text = code
				setCaretPosition(0)
				setLoaded()
			},
		)
	}

	override fun refresh() {
		text = node.codeInfo.getCodeStr()
	}

	fun updateCode(newCode: String?) {
		val caretPos = caretPosition
		text = newCode
		setCaretPosition(caretPos)
		scriptNode.isChanged = true
	}

	fun save() {
		scriptNode.save(getText())
		scriptNode.isChanged = false
	}

	override fun dispose() {
		shortcutsController.unbindActionsForComponent(this)
		autoCompletion.uninstall()
		super.dispose()
	}
}
