package jadx.plugins.kotlin.metadata.pass

import jadx.api.plugins.pass.JadxPassInfo
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo
import jadx.api.plugins.pass.types.JadxPreparePass
import jadx.core.dex.attributes.AFlag
import jadx.core.dex.nodes.RootNode
import jadx.plugins.kotlin.metadata.KotlinMetadataOptions
import jadx.plugins.kotlin.metadata.utils.KotlinMetadataUtils

class KotlinMetadataPreparePass(
	private val options: KotlinMetadataOptions,
) : JadxPreparePass {

	override fun getInfo(): JadxPassInfo {
		return OrderedJadxPassInfo(
			"KotlinMetadataPrepare",
			"Use kotlin.Metadata annotation to rename class & package",
		)
			.before("RenameVisitor")
	}

	override fun init(root: RootNode) {
		if (options.isClassAlias) {
			for (cls in root.classes) {
				if (cls.contains(AFlag.DONT_RENAME)) {
					continue
				}

				// rename class & package
				val kotlinCls = KotlinMetadataUtils.getAlias(cls)
				if (kotlinCls != null) {
					cls.rename(kotlinCls.name)
					cls.packageNode.rename(kotlinCls.pkg)
				}
			}
		}
	}
}
