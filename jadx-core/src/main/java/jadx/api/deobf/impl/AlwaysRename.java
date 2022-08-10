package jadx.api.deobf.impl;

import jadx.api.deobf.IRenameCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class AlwaysRename implements IRenameCondition {

	public static final IRenameCondition INSTANCE = new AlwaysRename();

	private AlwaysRename() {
	}

	@Override
	public void init(RootNode root) {
	}

	@Override
	public boolean shouldRename(PackageNode pkg) {
		return true;
	}

	@Override
	public boolean shouldRename(ClassNode cls) {
		return true;
	}

	@Override
	public boolean shouldRename(FieldNode fld) {
		return true;
	}

	@Override
	public boolean shouldRename(MethodNode mth) {
		return true;
	}
}
