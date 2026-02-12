package jadx.plugins.script.kotlin.gui

import jadx.gui.settings.JadxSettings
import jadx.gui.utils.UiUtils
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionCellRenderer
import javax.swing.JList

class ScriptCompletionRenderer(settings: JadxSettings) : CompletionCellRenderer() {
	init {
		displayFont = settings.codeFont
	}

	override fun prepareForOtherCompletion(
		list: JList<*>?,
		c: Completion?,
		index: Int,
		selected: Boolean,
		hasFocus: Boolean,
	) {
		val cmpl = c as ScriptCompletionData
		setText(
			UiUtils.wrapHtml((UiUtils.escapeHtml(cmpl.inputText) + "  " + UiUtils.fadeHtml(UiUtils.escapeHtml(cmpl.summary)))),
		)
	}
}
