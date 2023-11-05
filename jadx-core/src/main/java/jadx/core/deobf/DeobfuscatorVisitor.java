package jadx.core.deobf;

import jadx.api.JadxArgs;
import jadx.api.deobf.IAliasProvider;
import jadx.api.deobf.IRenameCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class DeobfuscatorVisitor extends AbstractVisitor {

	@Override
	public void init(RootNode root) throws JadxException {
		JadxArgs args = root.getArgs();
		if (!args.isDeobfuscationOn()) {
			return;
		}
		DeobfPresets mapping = DeobfPresets.build(root);
		if (args.getGeneratedRenamesMappingFileMode().shouldRead()) {
			if (mapping.load()) {
				mapping.apply(root);
			}
		}
		IAliasProvider aliasProvider = args.getAliasProvider();
		IRenameCondition renameCondition = args.getRenameCondition();
		mapping.initIndexes(aliasProvider);
		process(root, renameCondition, aliasProvider);
	}

	public static void process(RootNode root, IRenameCondition renameCondition, IAliasProvider aliasProvider) {
		boolean pkgUpdated = false;
		for (PackageNode pkg : root.getPackages()) {
			if (renameCondition.shouldRename(pkg)) {
				String alias = aliasProvider.forPackage(pkg);
				if (alias != null) {
					pkg.rename(alias, false);
					pkgUpdated = true;
				}
			}
		}
		if (pkgUpdated) {
			root.runPackagesUpdate();
		}

		for (ClassNode cls : root.getClasses()) {
			if (renameCondition.shouldRename(cls)) {
				String clsAlias = aliasProvider.forClass(cls);
				if (clsAlias != null) {
					cls.rename(clsAlias);
				}
			}
			for (FieldNode fld : cls.getFields()) {
				if (renameCondition.shouldRename(fld)) {
					String fldAlias = aliasProvider.forField(fld);
					if (fldAlias != null) {
						fld.rename(fldAlias);
					}
				}
			}
			for (MethodNode mth : cls.getMethods()) {
				if (renameCondition.shouldRename(mth)) {
					String mthAlias = aliasProvider.forMethod(mth);
					if (mthAlias != null) {
						mth.rename(mthAlias);
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		return "DeobfuscatorVisitor";
	}
}
