package jadx.core.deobf.conditions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;

/**
 * Disable deobfuscation for nodes:
 * - with 'DONT_RENAME' flag
 * - already renamed
 */
public class BaseDeobfCondition extends AbstractDeobfCondition {

	@Override
	public Action check(PackageNode pkg) {
		if (pkg.contains(AFlag.DONT_RENAME) || pkg.hasAlias()) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		if (cls.contains(AFlag.DONT_RENAME) || cls.getClassInfo().hasAlias()) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(MethodNode mth) {
		if (mth.contains(AFlag.DONT_RENAME)
				|| mth.getMethodInfo().hasAlias()
				|| mth.isConstructor()) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		if (fld.contains(AFlag.DONT_RENAME) || fld.getFieldInfo().hasAlias()) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}
}
