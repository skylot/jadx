package jadx.plugins.script.runtime.data

import jadx.core.dex.nodes.ClassNode
import jadx.plugins.script.runtime.JadxScriptInstance

class Search(private val jadx: JadxScriptInstance) {
	private val dec = jadx.internalDecompiler

	fun classByFullName(fullName: String): ClassNode? {
		return dec.searchClassNodeByOrigFullName(fullName)
	}

	fun classesByShortName(fullName: String): List<ClassNode> {
		return dec.root.searchClassByShortName(fullName)
	}
}
