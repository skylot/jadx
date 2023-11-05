package jadx.core.deobf.conditions;

import java.util.HashSet;
import java.util.Set;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class AvoidClsAndPkgNamesCollision extends AbstractDeobfCondition {

	private final Set<String> avoidClsNames = new HashSet<>();

	@Override
	public void init(RootNode root) {
		avoidClsNames.clear();
		for (PackageNode pkg : root.getPackages()) {
			avoidClsNames.add(pkg.getName());
		}
	}

	@Override
	public Action check(ClassNode cls) {
		if (avoidClsNames.contains(cls.getAlias())) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}
}
