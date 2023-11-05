package jadx.core.deobf.conditions;

import jadx.api.JadxArgs;
import jadx.api.deobf.IDeobfCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class DeobfLengthCondition implements IDeobfCondition {

	private int minLength;
	private int maxLength;

	@Override
	public void init(RootNode root) {
		JadxArgs args = root.getArgs();
		this.minLength = args.getDeobfuscationMinLength();
		this.maxLength = args.getDeobfuscationMaxLength();
	}

	private Action checkName(String s) {
		int len = s.length();
		if (len < minLength || len > maxLength) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(PackageNode pkg) {
		return checkName(pkg.getName());
	}

	@Override
	public Action check(ClassNode cls) {
		return checkName(cls.getName());
	}

	@Override
	public Action check(FieldNode fld) {
		return checkName(fld.getName());
	}

	@Override
	public Action check(MethodNode mth) {
		return checkName(mth.getName());
	}
}
