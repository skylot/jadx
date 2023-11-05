package jadx.core.deobf.conditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;

public class DeobfWhitelist extends AbstractDeobfCondition {

	public static final List<String> DEFAULT_LIST = Arrays.asList(
			"android.support.v4.*",
			"android.support.v7.*",
			"android.support.v4.os.*",
			"android.support.annotation.Px",
			"androidx.core.os.*",
			"androidx.annotation.Px");

	public static final String DEFAULT_STR = Utils.listToString(DEFAULT_LIST, " ");

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
		if (packages.contains(pkg.getPkgInfo().getFullName())) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		if (classes.contains(cls.getClassInfo().getFullName())) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}
}
