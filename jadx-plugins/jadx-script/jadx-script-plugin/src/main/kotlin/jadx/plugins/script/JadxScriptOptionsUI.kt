package jadx.plugins.script

import jadx.api.plugins.gui.ISettingsGroup
import jadx.api.plugins.gui.JadxGuiContext
import jadx.plugins.script.runtime.data.JadxScriptAllOptions
import javax.swing.JPanel

object JadxScriptOptionsUI {

	fun setup(guiContext: JadxGuiContext, scriptOptions: JadxScriptAllOptions) {
		val settings = guiContext.settings()
		val subGroups = scriptOptions.descriptions
			.groupBy { it.script }
			.map { (script, options) -> settings.buildSettingsGroupForOptions(script, options) }
			.toList()
		settings.setCustomSettingsGroup(EmptyRootGroup("Scripts", subGroups))
	}
}

private class EmptyRootGroup(
	private val title: String,
	private val subGroups: List<ISettingsGroup>,
) : ISettingsGroup {

	override fun getTitle() = title

	override fun buildComponent() = JPanel()

	override fun getSubGroups() = subGroups
}
