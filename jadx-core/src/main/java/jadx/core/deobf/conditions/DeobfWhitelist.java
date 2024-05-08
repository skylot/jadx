package jadx.core.deobf.conditions;

import java.util.HashSet;
import java.util.Set;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class DeobfWhitelist extends AbstractDeobfCondition {
	private final Set<String> packages = new HashSet<>();
	private final Set<String> classes = new HashSet<>();

	@Override
	public void init(RootNode root) {
		packages.clear();
		classes.clear();
		for (String whitelistItem : root.getArgs().getDeobfuscationWhitelist()) {
			if (!whitelistItem.isEmpty()) {
				if (whitelistItem.endsWith(".*")) {
					packages.add(whitelistItem.substring(0, whitelistItem.length() - 2));
				} else {
					classes.add(whitelistItem);
				}
			}
		}
	}

	@Override
	public Action check(PackageNode pkg) {
		String pkgName = pkg.getPkgInfo().getFullName();
		if (packages.stream().anyMatch(pattern -> pattern.equals(pkgName) || pkgName.startsWith(pattern + "."))) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		if (classes.contains(cls.getClassInfo().getFullName())) {
			return Action.FORBID_RENAME;
		}
		return check(cls.getPackageNode());
	}

	@Override
	public Action check(FieldNode fld) {
		return check(fld.getParentClass());
	}

	@Override
	public Action check(MethodNode mth) {
		return check(mth.getParentClass());
	}
}
