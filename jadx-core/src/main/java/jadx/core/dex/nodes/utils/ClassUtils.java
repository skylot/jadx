package jadx.core.dex.nodes.utils;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

public class ClassUtils {

	private final RootNode root;
	private final AccessCheckUtils accessCheckUtils;

	public ClassUtils(RootNode rootNode) {
		this.root = rootNode;
		this.accessCheckUtils = new AccessCheckUtils(root);
	}

	public boolean isAccessible(ClassNode cls, ClassNode callerCls) {
		return accessCheckUtils.isAccessible(cls, callerCls);
	}
}
