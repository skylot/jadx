package jadx.plugins.script.kotlin.gui

import jadx.api.plugins.JadxPluginContext
import jadx.gui.treemodel.JClass
import jadx.gui.treemodel.JNode
import jadx.gui.ui.MainWindow
import jadx.gui.utils.NLS
import jadx.gui.utils.UiUtils
import jadx.gui.utils.ui.SimpleMenuItem
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JPopupMenu

class JInputScripts(
	pluginContext: JadxPluginContext,
	scripts: List<Path>,
) : JNode() {
	companion object {
		private val INPUT_SCRIPTS_ICON: ImageIcon = UiUtils.openSvgIcon("nodes/scriptsModel")
	}

	init {
		for (script in scripts) {
			add(JInputScript(pluginContext, script))
		}
	}

	override fun onTreePopupMenu(mainWindow: MainWindow): JPopupMenu {
		val menu = JPopupMenu()
		menu.add(SimpleMenuItem(NLS.str("popup.add_scripts")) { mainWindow.addFiles() })
		menu.add(SimpleMenuItem(NLS.str("popup.new_script")) { mainWindow.addNewScript() })
		return menu
	}

	override fun getJParent(): JClass? {
		return null
	}

	override fun getIcon(): Icon {
		return INPUT_SCRIPTS_ICON
	}

	override fun getID(): String {
		return "JInputScripts"
	}

	override fun makeString(): String {
		return NLS.str("tree.input_scripts")
	}
}
