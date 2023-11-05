package jadx.api.deobf.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import jadx.api.deobf.IDeobfCondition;
import jadx.api.deobf.IRenameCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class CombineDeobfConditions implements IRenameCondition {

	public static IRenameCondition combine(List<IDeobfCondition> conditions) {
		return new CombineDeobfConditions(conditions);
	}

	public static IRenameCondition combine(IDeobfCondition... conditions) {
		return new CombineDeobfConditions(Arrays.asList(conditions));
	}

	private final List<IDeobfCondition> conditions;

	private CombineDeobfConditions(List<IDeobfCondition> conditions) {
		if (conditions == null || conditions.isEmpty()) {
			throw new IllegalArgumentException("Conditions list can't be empty");
		}
		this.conditions = conditions;
	}

	private boolean combineFunc(Function<IDeobfCondition, IDeobfCondition.Action> check) {
		for (IDeobfCondition c : conditions) {
			switch (check.apply(c)) {
				case NO_ACTION:
					// ignore
					break;
				case FORCE_RENAME:
					return true;
				case FORBID_RENAME:
					return false;
			}
		}
		return false;
	}

	@Override
	public void init(RootNode root) {
		conditions.forEach(c -> c.init(root));
	}

	@Override
	public boolean shouldRename(PackageNode pkg) {
		return combineFunc(c -> c.check(pkg));
	}

	@Override
	public boolean shouldRename(ClassNode cls) {
		return combineFunc(c -> c.check(cls));
	}

	@Override
	public boolean shouldRename(FieldNode fld) {
		return combineFunc(c -> c.check(fld));
	}

	@Override
	public boolean shouldRename(MethodNode mth) {
		return combineFunc(c -> c.check(mth));
	}
}
