package jadx.api;

import java.util.List;

import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.IDexTreeVisitor;

public class JadxInternalAccess {

	public static RootNode getRoot(JadxDecompiler d) {
		return d.getRoot();
	}

	public static List<IDexTreeVisitor> getPassList(JadxDecompiler d) {
		return d.getPasses();
	}
}
