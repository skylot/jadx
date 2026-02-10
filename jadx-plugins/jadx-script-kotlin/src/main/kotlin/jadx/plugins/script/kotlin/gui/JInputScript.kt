package jadx.plugins.script.kotlin.gui

import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.ICodeInfo
import jadx.api.impl.SimpleCodeInfo
import jadx.api.plugins.JadxPluginContext
import jadx.core.utils.exceptions.JadxRuntimeException
import jadx.core.utils.files.FileUtils
import jadx.gui.treemodel.JClass
import jadx.gui.treemodel.JEditableNode
import jadx.gui.ui.MainWindow
import jadx.gui.ui.panel.ContentPanel
import jadx.gui.ui.tab.TabbedPane
import jadx.gui.utils.NLS
import jadx.gui.utils.UiUtils
import jadx.gui.utils.ui.SimpleMenuItem
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JPopupMenu

private val log = KotlinLogging.logger {}

class JInputScript(
	val pluginContext: JadxPluginContext,
	private val scriptPath: Path,
) : JEditableNode() {
	companion object {
		private val SCRIPT_ICON: ImageIcon = UiUtils.openSvgIcon("nodes/kotlin_script")
	}

	private val name: String = scriptPath.fileName.toString().replace(".jadx.kts", "")

	override fun hasContent(): Boolean {
		return true
	}

	override fun getContentPanel(tabbedPane: TabbedPane): ContentPanel {
		return ScriptContentPanel(pluginContext, tabbedPane, this)
	}

	override fun getCodeInfo(): ICodeInfo {
		try {
			return SimpleCodeInfo(FileUtils.readFile(scriptPath))
		} catch (e: Exception) {
			throw JadxRuntimeException("Failed to read script file: " + scriptPath.toAbsolutePath(), e)
		}
	}

	override fun save(newContent: String?) {
		try {
			FileUtils.writeFile(scriptPath, newContent)
			log.debug { "Script saved: ${scriptPath.toAbsolutePath()}" }
		} catch (e: Exception) {
			throw JadxRuntimeException("Failed to write script file: " + scriptPath.toAbsolutePath(), e)
		}
	}

	override fun onTreePopupMenu(mainWindow: MainWindow): JPopupMenu {
		val menu = JPopupMenu()
		menu.add(SimpleMenuItem(NLS.str("popup.add_scripts")) { mainWindow.addFiles() })
		menu.add(SimpleMenuItem(NLS.str("popup.new_script")) { mainWindow.addNewScript() })
		menu.add(SimpleMenuItem(NLS.str("popup.remove")) { mainWindow.removeInput(scriptPath) })
		menu.add(SimpleMenuItem(NLS.str("popup.rename")) { mainWindow.renameInput(scriptPath) })
		return menu
	}

	override fun getSyntaxName(): String {
		return SyntaxConstants.SYNTAX_STYLE_KOTLIN
	}

	override fun getJParent(): JClass? {
		return null
	}

	override fun getIcon(): Icon {
		return SCRIPT_ICON
	}

	override fun getName(): String {
		return name
	}

	override fun makeString(): String {
		return name
	}

	override fun getTooltip(): String {
		return scriptPath.normalize().toAbsolutePath().toString()
	}
}
