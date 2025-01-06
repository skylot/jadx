package jadx.plugins.kotlin.smap.pass

import jadx.api.plugins.pass.JadxPassInfo
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo
import jadx.api.plugins.pass.types.JadxPreparePass
import jadx.core.dex.attributes.AFlag
import jadx.core.dex.nodes.RootNode
import jadx.plugins.kotlin.smap.KotlinSmapOptions
import jadx.plugins.kotlin.smap.utils.KotlinSmapUtils

class KotlinSourceDebugExtensionPass(
	private val options: KotlinSmapOptions,
) : JadxPreparePass {

	override fun getInfo(): JadxPassInfo {
		return OrderedJadxPassInfo(
			"SourceDebugExtensionPrepare",
			"Use kotlin.jvm.internal.SourceDebugExtension annotation to rename class & package",
		)
			.before("RenameVisitor")
	}

	override fun init(root: RootNode) {
		if (options.isClassAliasSourceDbg) {
			for (cls in root.classes) {
				if (cls.contains(AFlag.DONT_RENAME)) {
					continue
				}

				// rename class & package
				val kotlinCls = KotlinSmapUtils.getClassAlias(cls)
				if (kotlinCls != null) {
					cls.rename(kotlinCls.name)
					cls.packageNode.rename(kotlinCls.pkg)
				}
			}
		}
	}
}
