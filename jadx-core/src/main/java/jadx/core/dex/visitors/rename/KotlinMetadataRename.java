package jadx.core.dex.visitors.rename;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.kotlin.ClsAliasPair;
import jadx.core.utils.kotlin.KotlinMetadataUtils;

public class KotlinMetadataRename {

	public static void process(RootNode root) {
		if (root.getArgs().isParseKotlinMetadata()) {
			for (ClassNode cls : root.getClasses()) {
				if (cls.contains(AFlag.DONT_RENAME)) {
					continue;
				}
				ClsAliasPair kotlinCls = KotlinMetadataUtils.getClassAlias(cls);
				if (kotlinCls != null) {
					cls.rename(kotlinCls.getName());
					cls.getPackageNode().rename(kotlinCls.getPkg());
				}
			}
		}
	}
}
