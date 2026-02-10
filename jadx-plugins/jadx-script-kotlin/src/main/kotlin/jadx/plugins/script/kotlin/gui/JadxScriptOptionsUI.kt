package jadx.plugins.script.kotlin.gui

import jadx.api.plugins.gui.ISettingsGroup
import jadx.api.plugins.gui.JadxGuiContext
import jadx.plugins.script.kotlin.runtime.data.JadxScriptAllOptions
import javax.swing.JPanel

object JadxScriptOptionsUI {
	fun setup(guiContext: JadxGuiContext, scriptOptions: JadxScriptAllOptions) {
		guiContext.settings().setCustomSettingsGroup(ScriptOptionsRootGroup(guiContext, scriptOptions))
	}
}

private class ScriptOptionsRootGroup(
	private val guiContext: JadxGuiContext,
	private val scriptOptions: JadxScriptAllOptions,
) : ISettingsGroup {

	override fun getTitle() = "Scripts"

	override fun buildComponent() = JPanel() // empty panel for root node

	override fun getSubGroups(): List<ISettingsGroup> {
		val settings = guiContext.settings()
		return scriptOptions.descriptions
			.groupBy { it.script }
			.map { (script, options) -> settings.buildSettingsGroupForOptions(script, options) }
			.toList()
	}
}
