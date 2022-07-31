package jadx.api.deobf;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public interface IRenameCondition {

	void init(RootNode root);

	boolean shouldRename(PackageNode pkg);

	boolean shouldRename(ClassNode cls);

	boolean shouldRename(FieldNode fld);

	boolean shouldRename(MethodNode mth);
}
