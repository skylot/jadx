package jadx.plugins.script.runtime.data

import jadx.api.core.nodes.IRootNode
import jadx.core.dex.nodes.IDexNode
import jadx.core.dex.nodes.RootNode
import jadx.plugins.script.runtime.JadxScriptInstance

class RenamePass(private val jadx: JadxScriptInstance) {

	fun all(makeNewName: (String) -> String?) {
		all { name, _ -> makeNewName.invoke(name) }
	}

	fun all(makeNewName: (String, IDexNode) -> String?) {
		jadx.addPass(object : ScriptOrderedPreparePass(
			jadx,
			"RenameAll",
			runBefore = listOf("RenameVisitor")
		) {
			override fun init(root: IRootNode) {
				val rootNode = root as RootNode
				for (pkgNode in rootNode.packages) {
					makeNewName.invoke(pkgNode.pkgInfo.name, pkgNode)?.let {
						pkgNode.rename(it)
					}
				}
				for (cls in rootNode.classes) {
					makeNewName.invoke(cls.classInfo.shortName, cls)?.let {
						cls.classInfo.changeShortName(it)
					}
					for (mth in cls.methods) {
						if (mth.isConstructor) {
							continue
						}
						makeNewName.invoke(mth.name, mth)?.let {
							mth.rename(it)
						}
					}
					for (fld in cls.fields) {
						makeNewName.invoke(fld.name, fld)?.let {
							fld.fieldInfo.alias = it
						}
					}
				}
			}
		})
	}
}
