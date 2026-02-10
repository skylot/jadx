package jadx.plugins.script.kotlin.runtime.data

import jadx.core.dex.nodes.ClassNode
import jadx.plugins.script.kotlin.runtime.JadxScriptInstance

class Search(jadx: JadxScriptInstance) {
	private val dec = jadx.internalDecompiler

	fun classByFullName(fullName: String): ClassNode? {
		return dec.searchClassNodeByOrigFullName(fullName)
	}

	fun classesByShortName(fullName: String): List<ClassNode> {
		return dec.root.searchClassByShortName(fullName)
	}
}
