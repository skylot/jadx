package jadx.api;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class JadxInternalAccess {

	public static RootNode getRoot(JadxDecompiler d) {
		return d.getRoot();
	}

	public static JavaClass convertClassNode(JadxDecompiler d, ClassNode clsNode) {
		return d.convertClassNode(clsNode);
	}

	public static JavaMethod convertMethodNode(JadxDecompiler d, MethodNode mthNode) {
		return d.convertMethodNode(mthNode);
	}

	public static JavaField convertFieldNode(JadxDecompiler d, FieldNode fldNode) {
		return d.convertFieldNode(fldNode);
	}
}
