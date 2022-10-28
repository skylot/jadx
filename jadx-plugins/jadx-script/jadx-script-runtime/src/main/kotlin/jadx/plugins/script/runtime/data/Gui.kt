package jadx.plugins.script.runtime.data

import jadx.api.plugins.gui.JadxGuiContext
import jadx.plugins.script.runtime.JadxScriptInstance

class Gui(
	private val jadx: JadxScriptInstance,
	private val guiContext: JadxGuiContext?
) {

	fun isAvailable() = guiContext != null

	fun ifAvailable(block: Gui.() -> Unit) {
		guiContext?.let { this.apply(block) }
	}

	fun ui(block: () -> Unit) {
		guiContext?.uiRun(block)
	}

	fun addMenuAction(name: String, action: () -> Unit) {
		guiContext?.addMenuAction(name, action)
	}
}
