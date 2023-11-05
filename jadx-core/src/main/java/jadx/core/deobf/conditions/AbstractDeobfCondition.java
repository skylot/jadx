package jadx.core.deobf.conditions;

import jadx.api.deobf.IDeobfCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public abstract class AbstractDeobfCondition implements IDeobfCondition {

	@Override
	public void init(RootNode root) {
	}

	@Override
	public Action check(PackageNode pkg) {
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		return Action.NO_ACTION;
	}

	@Override
	public Action check(MethodNode mth) {
		return Action.NO_ACTION;
	}
}
