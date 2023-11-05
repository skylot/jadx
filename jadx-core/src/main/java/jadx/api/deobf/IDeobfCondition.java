package jadx.api.deobf;

import jadx.api.deobf.impl.CombineDeobfConditions;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

/**
 * Utility interface to simplify merging several rename conditions to build {@link IRenameCondition}
 * instance with {@link CombineDeobfConditions#combine(IDeobfCondition...)}.
 */
public interface IDeobfCondition {

	enum Action {
		NO_ACTION,
		FORCE_RENAME,
		FORBID_RENAME,
	}

	void init(RootNode root);

	Action check(PackageNode pkg);

	Action check(ClassNode cls);

	Action check(FieldNode fld);

	Action check(MethodNode mth);
}
