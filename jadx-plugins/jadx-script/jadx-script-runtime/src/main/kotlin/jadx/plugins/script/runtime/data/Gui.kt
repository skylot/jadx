package jadx.plugins.script.runtime.data

import jadx.api.metadata.ICodeNodeRef
import jadx.api.plugins.gui.JadxGuiContext
import jadx.plugins.script.runtime.JadxScriptInstance

class Gui(
	private val jadx: JadxScriptInstance,
	private val guiContext: JadxGuiContext?,
) {

	fun isAvailable() = guiContext != null

	fun ifAvailable(block: Gui.() -> Unit) {
		guiContext?.let { this.apply(block) }
	}

	fun ui(block: () -> Unit) {
		context().uiRun(block)
	}

	fun addMenuAction(name: String, action: () -> Unit) {
		context().addMenuAction(name, action)
	}

	fun addPopupMenuAction(
		name: String,
		enabled: (ICodeNodeRef) -> Boolean = { _ -> true },
		keyBinding: String? = null,
		action: (ICodeNodeRef) -> Unit,
	) {
		context().addPopupMenuAction(name, enabled, keyBinding, action)
	}

	fun registerGlobalKeyBinding(id: String, keyBinding: String, action: () -> Unit): Boolean {
		return context().registerGlobalKeyBinding(id, keyBinding, action)
	}

	fun copyToClipboard(str: String) {
		context().copyToClipboard(str)
	}

	private fun context(): JadxGuiContext =
		guiContext ?: throw IllegalStateException("GUI plugins context not available!")
}
