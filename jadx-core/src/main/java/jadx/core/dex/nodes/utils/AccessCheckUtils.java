package jadx.core.dex.nodes.utils;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

class AccessCheckUtils {

	private final RootNode root;

	AccessCheckUtils(RootNode rootNode) {
		this.root = rootNode;
	}

	boolean isAccessible(ICodeNode targetNode, ICodeNode callerNode) {
		ClassNode targetCls = getDeclaringClass(targetNode);
		ClassNode callerCls = getDeclaringClass(callerNode);

		if (targetCls.equals(callerCls)) {
			return true;
		}

		AccessInfo targetVisibility;
		if (targetNode == targetCls) {
			targetVisibility = targetNode.getAccessFlags().getVisibility();
		} else {
			AccessInfo targetClsVisibility = targetCls.getAccessFlags().getVisibility();
			AccessInfo targetNodeVisibility = targetNode.getAccessFlags().getVisibility();
			targetVisibility = targetClsVisibility.isVisibilityWeakerThan(targetNodeVisibility)
					? targetClsVisibility
					: targetNodeVisibility;
		}

		if (targetVisibility.isPublic()) {
			return true;
		}

		if (targetVisibility.isProtected()) {
			return isProtectedAccessible(targetCls, callerCls);
		}

		if (targetVisibility.isPackagePrivate()) {
			return isPackagePrivateAccessible(targetCls, callerCls);
		}

		if (targetVisibility.isPrivate()) {
			return isPrivateAccessible(targetCls, callerCls);
		}

		throw new JadxRuntimeException(targetVisibility + " is not supported");
	}

	private ClassNode getDeclaringClass(ICodeNode node) {
		if (node instanceof ClassNode) {
			return (ClassNode) node;
		} else if (node instanceof MethodNode) {
			return ((MethodNode) node).getParentClass();
		} else if (node instanceof FieldNode) {
			return ((FieldNode) node).getParentClass();
		} else {
			throw new JadxRuntimeException(node + " is not supported");
		}
	}

	private boolean isProtectedAccessible(ClassNode cls, ClassNode callerCls) {
		return isPackagePrivateAccessible(cls, callerCls) || isSuperType(cls, callerCls);
	}

	private boolean isPackagePrivateAccessible(ClassNode cls, ClassNode callerCls) {
		return cls.getPackageNode().equals(callerCls.getPackageNode());
	}

	private boolean isPrivateAccessible(ClassNode cls, ClassNode callerCls) {
		return cls.getTopParentClass().equals(callerCls.getTopParentClass());
	}

	private boolean isSuperType(ClassNode cls, ClassNode superCls) {
		return root.getClsp().getSuperTypes(cls.getRawName()).stream()
				.anyMatch(x -> x.equals(superCls.getRawName()));
	}
}
