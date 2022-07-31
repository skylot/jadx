package jadx.api.deobf;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public interface IAliasProvider {

	default void init(RootNode root) {
		// optional
	}

	String forPackage(PackageNode pkg);

	String forClass(ClassNode cls);

	String forField(FieldNode fld);

	String forMethod(MethodNode mth);

	/**
	 * Optional method to set initial max indexes loaded from mapping
	 */
	default void initIndexes(int pkg, int cls, int fld, int mth) {
		// optional
	}
}
