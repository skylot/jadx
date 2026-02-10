package jadx.plugins.script.kotlin.gui

import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.gui.JadxGuiContext
import jadx.gui.plugins.context.GuiPluginContext
import jadx.gui.plugins.context.ITreeInputCategory
import jadx.gui.settings.data.ITabStatePersist
import jadx.gui.treemodel.JNode
import java.nio.file.Path

object JadxScriptInputCategory {
	fun register(pluginContext: JadxPluginContext, guiContext: JadxGuiContext) {
		val internalContext = guiContext as GuiPluginContext
		val inputCategory = InputScriptsBuilder(pluginContext)
		internalContext.registerTreeInputCategory(inputCategory)
		internalContext.registerTabStatePersistAdapter(InputScriptTabStatePersist(inputCategory))
	}
}

class InputScriptsBuilder(private val pluginContext: JadxPluginContext) : ITreeInputCategory {
	var scriptsRootNode: JInputScripts? = null

	override fun filesFilter(file: Path): Boolean {
		return file.fileName.toString().endsWith(".jadx.kts", ignoreCase = true)
	}

	override fun buildInputNode(files: List<Path>): JNode {
		val scriptsNode = JInputScripts(pluginContext, files)
		scriptsRootNode = scriptsNode
		return scriptsNode
	}
}

class InputScriptTabStatePersist(private val scriptsBuilder: InputScriptsBuilder) : ITabStatePersist {
	override fun getNodeClass() = JInputScript::class.java

	override fun save(node: JNode): String {
		return node.name
	}

	override fun load(nodeName: String): JNode? {
		return scriptsBuilder.scriptsRootNode?.searchNode { it.name.equals(nodeName) }
	}
}
