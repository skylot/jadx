/**
 * Rename method parameters from value in attached annotation
 */

import jadx.api.plugins.input.data.attributes.JadxAttrType
import jadx.core.deobf.NameMapper
import jadx.core.dex.nodes.MethodNode
import jadx.plugins.script.runtime.data.ScriptDecompilePass

val annCls = "Lretrofit2/http/Query;"
val annParam = "value"

val jadx = getJadxInstance()

// access to method parameters variables available only in decompile passes
jadx.addPass(object : ScriptDecompilePass(jadx, "RenameParams") {
	override fun visit(mth: MethodNode) {
		// parameter annotations stored in method attribute
		mth.get(JadxAttrType.ANNOTATION_MTH_PARAMETERS)?.let { paramsAttr ->
			for ((paramNum, annAttr) in paramsAttr.paramList.withIndex()) {
				val name = annAttr?.get(annCls)?.values?.get(annParam)?.value as String?
				if (NameMapper.isValidIdentifier(name)) {
					mth.argRegs[paramNum].name = name
					log.info { "Rename param $paramNum to $name in method $mth" }
				}
			}
		}
	}
})

jadx.afterLoad {
	// force decompilation and run rename pass for all classes (optional)
	jadx.decompile.allThreaded()
}
