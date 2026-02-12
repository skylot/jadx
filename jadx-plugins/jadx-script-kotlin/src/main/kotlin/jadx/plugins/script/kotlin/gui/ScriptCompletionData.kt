package jadx.plugins.script.kotlin.gui

import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionProvider
import javax.swing.Icon
import javax.swing.text.JTextComponent

class ScriptCompletionData(
	private val provider: CompletionProvider,
	private val relevance: Int,
	private val input: String,
	private val code: String,
	private val replacePos: Int,
	private val icon: Icon,
	private val toolTip: String,
	private val summary: String,
) : Completion {

	override fun getInputText(): String {
		return input
	}

	override fun getProvider(): CompletionProvider {
		return provider
	}

	override fun getAlreadyEntered(comp: JTextComponent?): String? {
		return provider.getAlreadyEnteredText(comp)
	}

	override fun getRelevance(): Int {
		return relevance
	}

	override fun getReplacementText(): String {
		return code.substring(0, replacePos) + input
	}

	override fun getIcon(): Icon {
		return icon
	}

	override fun getSummary(): String {
		return summary
	}

	override fun getToolTipText(): String {
		return toolTip
	}

	override fun compareTo(other: Completion): Int {
		return relevance.compareTo(other.relevance)
	}

	override fun toString(): String {
		return input
	}
}
