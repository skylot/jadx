package jadx.api;

import jadx.core.dex.nodes.RootNode;

public class JadxInternalAccess {

	public static RootNode getRoot(JadxDecompiler d) {
		return d.getRoot();
	}
}
