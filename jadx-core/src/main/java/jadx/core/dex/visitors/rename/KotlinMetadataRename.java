package jadx.core.dex.visitors.rename;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.kotlin.ClsAliasPair;
import jadx.core.utils.kotlin.ClsMetadataResult;
import jadx.core.utils.kotlin.KotlinMetadataUtils;

public class KotlinMetadataRename {

	public static void preDecompileProcess(RootNode root) {
		if (root.getArgs().isParseKotlinMetadata()) {
			for (ClassNode cls : root.getClasses()) {
				if (cls.contains(AFlag.DONT_RENAME)) {
					continue;
				}

				// rename class & package
				ClsAliasPair kotlinCls = KotlinMetadataUtils.getAlias(cls);
				if (kotlinCls != null) {
					cls.rename(kotlinCls.getName());
					cls.getPackageNode().rename(kotlinCls.getPkg());
				}
			}
		}
	}

	// TODO refactor into another class ?
	public static void process(ClassNode cls) {
		if (cls.contains(AFlag.DONT_RENAME)) {
			return;
		}
		ClsMetadataResult result = KotlinMetadataUtils.getMetadataResult(cls);
		if (result == null) {
			return;
		}

		// rename method arguments
		result.getMethodArgs().forEach((method, list) -> {
			list.forEach(rename -> {
				RegisterArg rArg = rename.getRArg();
				// TODO comment not being added ?
				RenameReasonAttr.forNode(rArg).append("from kotlin metadata");
				rArg.setName(rename.getAlias());
			});
		});

		// rename fields
		result.getFields().forEach((field, alias) -> {
			RenameReasonAttr.forNode(field).append("from kotlin metadata");
			field.rename(alias);
		});
	}
}
